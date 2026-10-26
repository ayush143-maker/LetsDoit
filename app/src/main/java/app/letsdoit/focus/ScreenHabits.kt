package app.letsdoit.focus

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

private data class UsageDay(
    val date: LocalDate,
    val appMs: Map<String, Long>
) {
    val totalMs: Long get() = appMs.values.sum()
}

private data class AppUsage(
    val packageName: String,
    val label: String,
    val dayMs: Map<LocalDate, Long>
) {
    val monthMs: Long get() = dayMs.values.sum()
}

private data class ScreenHabitsReport(
    val month: YearMonth,
    val days: List<UsageDay>,
    val apps: List<AppUsage>
) {
    fun day(date: LocalDate): UsageDay? = days.firstOrNull { it.date == date }
    val monthTotalMs: Long get() = days.sumOf { it.totalMs }
    val recordedDays: Int get() = days.count { it.totalMs > 0L }
    val averageRecordedDayMs: Long
        get() = if (recordedDays == 0) 0L else monthTotalMs / recordedDays
}

private object ScreenHabitsCache {
    val reports = mutableMapOf<YearMonth, ScreenHabitsReport>()
    val loadedAt = mutableMapOf<YearMonth, Long>()
}

private const val HISTORY_PREFS = "screen_habits_history"
private const val LABEL_PREFS = "screen_habits_labels"
private const val HISTORY_KEY = "daily_app_usage_json"
private const val CACHE_TTL_MS = 5 * 60_000L

fun hasUsageStatsAccess(ctx: Context): Boolean {
    val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    @Suppress("DEPRECATION")
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        ctx.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun openUsageAccessSettings(ctx: Context) {
    runCatching {
        ctx.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private fun readSavedHistory(ctx: Context): MutableMap<LocalDate, MutableMap<String, Long>> {
    val result = mutableMapOf<LocalDate, MutableMap<String, Long>>()
    runCatching {
        val root = JSONObject(ctx.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)
            .getString(HISTORY_KEY, "{}") ?: "{}")
        val dates = root.keys()
        while (dates.hasNext()) {
            val dateText = dates.next()
            val date = runCatching { LocalDate.parse(dateText) }.getOrNull() ?: continue
            val packages = root.optJSONObject(dateText) ?: continue
            val perApp = mutableMapOf<String, Long>()
            val keys = packages.keys()
            while (keys.hasNext()) {
                val pkg = keys.next()
                val value = packages.optLong(pkg, 0L)
                if (value > 0L) perApp[pkg] = value
            }
            result[date] = perApp
        }
    }
    return result
}

private fun saveHistory(ctx: Context, history: Map<LocalDate, Map<String, Long>>) {
    // Bound local storage to one year. Screen usage remains on-device.
    val cutoff = LocalDate.now().minusDays(366)
    val root = JSONObject()
    history.filterKeys { !it.isBefore(cutoff) }.forEach { (date, usage) ->
        val apps = JSONObject()
        usage.forEach { (pkg, ms) -> if (ms > 0L) apps.put(pkg, ms) }
        root.put(date.toString(), apps)
    }
    ctx.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)
        .edit().putString(HISTORY_KEY, root.toString()).apply()
}

private fun queryDayUsage(
    manager: UsageStatsManager,
    date: LocalDate,
    zone: ZoneId,
    now: Long
): MutableMap<String, Long> {
    val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val next = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val end = minOf(next, now)
    if (end <= start) return mutableMapOf()

    val byPackage = mutableMapOf<String, Long>()
    manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end).orEmpty()
        .forEach { stat ->
            val ms = stat.totalTimeInForeground
            if (ms > 0L) {
                // For a one-day query, duplicate package aggregates should not be added together.
                byPackage[stat.packageName] = maxOf(byPackage[stat.packageName] ?: 0L, ms)
            }
        }
    return byPackage
}

private fun readTodayEvents(
    manager: UsageStatsManager,
    today: LocalDate,
    zone: ZoneId,
    now: Long
): Map<String, Long> {
    val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
    val events = manager.queryEvents(start, now)
    val event = UsageEvents.Event()
    val totals = mutableMapOf<String, Long>()
    var foregroundPackage: String? = null
    var foregroundSince = 0L

    fun closeSpan(endTime: Long) {
        val pkg = foregroundPackage ?: return
        val end = minOf(endTime, now)
        if (end > foregroundSince) {
            totals[pkg] = (totals[pkg] ?: 0L) + end - foregroundSince
        }
        foregroundPackage = null
        foregroundSince = 0L
    }

    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        val pkg = event.packageName
        when (event.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED,
            UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                if (pkg != null && pkg != foregroundPackage) {
                    closeSpan(event.timeStamp)
                    foregroundPackage = pkg
                    foregroundSince = event.timeStamp
                }
            }
            UsageEvents.Event.ACTIVITY_PAUSED,
            UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                if (pkg != null && pkg == foregroundPackage) closeSpan(event.timeStamp)
            }
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> closeSpan(event.timeStamp)
        }
    }
    closeSpan(now)
    return totals
}

private fun readMonthReport(ctx: Context, month: YearMonth, now: Long): ScreenHabitsReport {
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val manager = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val history = readSavedHistory(ctx)
    val first = month.atDay(1)
    val last = month.atEndOfMonth()
    val freshDayData = mutableMapOf<LocalDate, MutableMap<String, Long>>()

    var date = first
    while (!date.isAfter(last) && !date.isAfter(today)) {
        val fresh = queryDayUsage(manager, date, zone, now)
        val old = history[date].orEmpty()
        // Use current Android data when available, otherwise keep previously observed local history.
        val combined = if (fresh.isNotEmpty()) fresh else old.toMutableMap()
        if (date == today) {
            val live = readTodayEvents(manager, today, zone, now)
            live.forEach { (pkg, ms) -> combined[pkg] = maxOf(combined[pkg] ?: 0L, ms) }
        }
        freshDayData[date] = combined
        history[date] = combined.toMutableMap()
        date = date.plusDays(1)
    }
    saveHistory(ctx, history)

    val days = (1..month.lengthOfMonth()).map { dayOfMonth ->
        val day = month.atDay(dayOfMonth)
        UsageDay(day, freshDayData[day] ?: history[day].orEmpty())
    }
    val packages = days.flatMap { it.appMs.keys }.toSet()
    val labels = loadApps(ctx).associate { it.pkg to it.label }
    val customLabels = ctx.getSharedPreferences(LABEL_PREFS, Context.MODE_PRIVATE)
    val apps = packages.mapNotNull { pkg ->
        val total = days.sumOf { it.appMs[pkg] ?: 0L }
        if (total <= 0L) return@mapNotNull null
        val original = labels[pkg] ?: appLabel(ctx, pkg)
        val defaultLabel = if (original.equals("YouTube", ignoreCase = true)) "Instagram" else original
        val label = customLabels.getString(pkg, defaultLabel) ?: defaultLabel
        AppUsage(
            packageName = pkg,
            label = label,
            dayMs = days.mapNotNull { d -> d.appMs[pkg]?.takeIf { it > 0L }?.let { d.date to it } }.toMap()
        )
    }.sortedByDescending { it.monthMs }

    return ScreenHabitsReport(month, days, apps)
}

private fun durationLabel(ms: Long): String {
    if (ms <= 0L) return "0 min"
    val mins = ms / 60_000L
    if (mins == 0L) return "<1 min"
    val hours = mins / 60L
    val minutes = mins % 60L
    return if (hours > 0L) "${hours}h ${minutes}m" else "${minutes} min"
}

private fun compactDuration(ms: Long): String {
    val mins = ms / 60_000L
    return when {
        mins >= 60L -> "${mins / 60L}h"
        mins > 0L -> "${mins}m"
        else -> "—"
    }
}

private fun roastFor(dayMs: Long, topApp: String?, averageMs: Long): String {
    if (dayMs <= 0L) {
        return "No verified screen-time total for this day. Even the roast needs receipts—check Usage Access and let Android report the facts."
    }
    val top = topApp ?: "your favourite app"
    val time = durationLabel(dayMs)
    return when {
        dayMs >= 8L * 60L * 60L * 1000L ->
            "SALE, $time on the phone? Keep this up and your brain will become an alu with Wi-Fi. $top has practically hired you full-time—with zero salary. Put it down and do one 25-minute focus block."
        dayMs >= 6L * 60L * 60L * 1000L ->
            "BRO, $time today? Your phone is a rectangle, not a life-support system. $top got your attention on EMI. Close it and earn back 25 minutes for something useful."
        dayMs >= 3L * 60L * 60L * 1000L ->
            "Sale, $time already? That was supposed to be a quick check, not a shift at $top Industries. Take a break from it and protect your next 25 minutes."
        dayMs >= 60L * 60L * 1000L ->
            "$time so far. Not a disaster, but don't let one quick check become an unpaid internship at $top. Pick one task and finish a focused 25 minutes."
        else ->
            "Phone time is $time. Nice—now don't donate the rest of your day to the scroll. Get one meaningful thing done before opening $top again."
    }.replace("\$time", time).replace("\$top", top)
}

@Composable
fun ScreenHabitsScreen(now: Long) {
    val ctx = LocalContext.current
    val today = remember(now / 60_000L) { Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate() }
    var usageAccess by remember { mutableStateOf(hasUsageStatsAccess(ctx)) }
    var selectedMonthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var selectedDateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var renamePackage by rememberSaveable { mutableStateOf<String?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }
    var report by remember(selectedMonthText) { mutableStateOf(ScreenHabitsCache.reports[runCatching { YearMonth.parse(selectedMonthText) }.getOrDefault(YearMonth.now())]) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val selectedMonth = runCatching { YearMonth.parse(selectedMonthText) }.getOrDefault(YearMonth.now())
    val selectedDate = runCatching { LocalDate.parse(selectedDateText) }.getOrDefault(today)

    LaunchedEffect(now / 10_000L) {
        usageAccess = hasUsageStatsAccess(ctx)
    }
    LaunchedEffect(usageAccess, selectedMonthText, now / 300_000L) {
        if (!usageAccess) {
            report = null
            loading = false
            failed = false
            return@LaunchedEffect
        }
        val cached = ScreenHabitsCache.reports[selectedMonth]
        val cacheAge = now - (ScreenHabitsCache.loadedAt[selectedMonth] ?: 0L)
        if (cached != null && cacheAge in 0..CACHE_TTL_MS) {
            report = cached
            loading = false
            failed = false
            return@LaunchedEffect
        }
        loading = true
        failed = false
        try {
            val fresh = withContext(Dispatchers.IO) { readMonthReport(ctx, selectedMonth, now) }
            ScreenHabitsCache.reports[selectedMonth] = fresh
            ScreenHabitsCache.loadedAt[selectedMonth] = now
            report = fresh
        } catch (_: Exception) {
            failed = true
        } finally {
            loading = false
        }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)
    ) {
        PageHeader("Screen Habits", "Your time. Your patterns. No excuses.")
        if (!usageAccess) {
            LCard {
                Eyebrow("Usage access")
                Spacer(Modifier.height(10.dp))
                Text("Let the numbers talk", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Allow Usage Access to see daily app time and monthly patterns. Reports are stored locally on this phone and are not uploaded.",
                    style = MaterialTheme.typography.bodyMedium, color = Muted
                )
                Spacer(Modifier.height(14.dp))
                PButton("Open Usage Access settings", Modifier.fillMaxWidth()) { openUsageAccessSettings(ctx) }
                Spacer(Modifier.height(10.dp))
                Text("After enabling access for Let's Do It, come back here.", style = MaterialTheme.typography.labelMedium, color = Muted)
            }
        } else if (loading && report == null) {
            LCard { Eyebrow("Screen time"); Spacer(Modifier.height(8.dp)); Text("Loading this month's usage…", color = Muted) }
        } else if (failed) {
            LCard {
                Eyebrow("Usage data unavailable")
                Spacer(Modifier.height(8.dp))
                Text("Android could not return usage records. Check Usage Access and try again.", color = Muted)
                Spacer(Modifier.height(12.dp))
                PButton("Open Usage Access settings", Modifier.fillMaxWidth()) { openUsageAccessSettings(ctx) }
            }
        } else {
            val data = report
            if (data != null) {
                val selectedDay = data.day(selectedDate)
                val selectedMs = selectedDay?.totalMs ?: 0L
                val selectedApps = data.apps.mapNotNull { app ->
                    app.dayMs[selectedDate]?.takeIf { it > 0L }?.let { app to it }
                }.sortedByDescending { it.second }
                val todayTotal = data.day(today)?.totalMs ?: 0L
                val todayTop = data.apps.mapNotNull { it.dayMs[today]?.let { value -> it to value } }.maxByOrNull { it.second }?.first?.label
                val currentMonth = YearMonth.from(today) == data.month

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(if (currentMonth) "Today" else "Selected month", durationLabel(if (currentMonth) todayTotal else data.monthTotalMs), Modifier.weight(1f))
                    StatTile(if (currentMonth) "Monthly daily avg" else "Daily avg", durationLabel(data.averageRecordedDayMs), Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                LCard {
                    Eyebrow("Straight talk · receipts only")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        roastFor(if (currentMonth) todayTotal else selectedMs, if (currentMonth) todayTop else selectedApps.firstOrNull()?.first?.label, data.averageRecordedDayMs),
                        style = MaterialTheme.typography.bodyLarge, color = Ink
                    )
                    Spacer(Modifier.height(8.dp))
                    val recordedDays = data.recordedDays
                    Text(
                        if (recordedDays == 0) "Not enough recorded days to calculate a meaningful average."
                        else "$recordedDays days have recorded usage this month. Average is calculated across recorded days, not missing days.",
                        style = MaterialTheme.typography.labelMedium, color = Muted
                    )
                }
                Spacer(Modifier.height(12.dp))
                LCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Eyebrow("Monthly phone-time calendar")
                            Spacer(Modifier.height(4.dp))
                            Text(data.month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + data.month.year, style = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(onClick = {
                            selectedMonthText = data.month.minusMonths(1).toString()
                            selectedDateText = data.month.minusMonths(1).atEndOfMonth().toString()
                        }) { Text("‹") }
                        TextButton(onClick = {
                            selectedMonthText = data.month.plusMonths(1).toString()
                            selectedDateText = data.month.plusMonths(1).atDay(1).toString()
                        }) { Text("›") }
                    }
                    Text(
                        "Tap a date to see which apps used the most time. Earlier months can only be shown if Android retained the data or Let's Do It previously saved it.",
                        style = MaterialTheme.typography.labelMedium, color = Muted
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(label, style = MaterialTheme.typography.labelSmall, color = Muted) }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    val offset = data.month.atDay(1).dayOfWeek.value - 1
                    val cells: List<LocalDate?> = List(offset) { null } + data.days.map { it.date }
                    val maxDayMs = maxOf(data.days.maxOfOrNull { it.totalMs } ?: 0L, 1L)
                    cells.chunked(7).forEach { week ->
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            week.forEach { date ->
                                if (date == null) {
                                    Spacer(Modifier.weight(1f).height(48.dp))
                                } else {
                                    val dayMs = data.day(date)?.totalMs ?: 0L
                                    val picked = date == selectedDate
                                    val future = date.isAfter(today)
                                    val alpha = if (dayMs <= 0L) 0f else (0.15f + 0.5f * dayMs.toFloat() / maxDayMs.toFloat()).coerceIn(0.15f, 0.65f)
                                    Column(
                                        Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(8.dp))
                                            .background(if (picked) AccentSoft else if (dayMs > 0L) Accent.copy(alpha = alpha) else Sand)
                                            .clickable {
                                                selectedDateText = date.toString()
                                                if (YearMonth.from(date) != selectedMonth) selectedMonthText = YearMonth.from(date).toString()
                                            }.padding(vertical = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelMedium, color = if (picked) Accent else Ink)
                                        Text(if (future) "—" else compactDuration(dayMs), style = MaterialTheme.typography.labelSmall, color = Muted, maxLines = 1)
                                    }
                                }
                            }
                            repeat(7 - week.size) { Spacer(Modifier.weight(1f).height(48.dp)) }
                        }
                        Spacer(Modifier.height(3.dp))
                    }
                    HorizontalDivider(color = Line)
                    Spacer(Modifier.height(10.dp))
                    Text("Recorded this month: ${durationLabel(data.monthTotalMs)}", style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(12.dp))
                LCard {
                    Eyebrow("What ate the time · ${selectedDate.dayOfMonth} ${selectedDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}")
                    Spacer(Modifier.height(8.dp))
                    Text("Total recorded: ${durationLabel(selectedMs)}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    if (selectedApps.isEmpty()) {
                        Text(
                            if (selectedDate.isAfter(today)) "This date has not happened yet."
                            else "No app usage was recorded for this date. That may mean no usage, or that Android did not retain the data.",
                            style = MaterialTheme.typography.bodyMedium, color = Muted
                        )
                    } else {
                        val top = selectedApps.first()
                        Text(
                            "${top.first.label} is the biggest recorded time sink for this date (${durationLabel(top.second)}). That tells us where the time went—not why you opened the app.",
                            style = MaterialTheme.typography.bodyMedium, color = Ink
                        )
                        Spacer(Modifier.height(10.dp))
                        selectedApps.take(6).forEach { (app, ms) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(app.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(durationLabel(ms), style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(Modifier.height(5.dp))
                            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(5.dp)).background(Line)) {
                                Box(Modifier.fillMaxHeight().fillMaxWidth((ms.toFloat() / maxOf(selectedMs, 1L).toFloat()).coerceIn(0f, 1f)).background(Accent))
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                LCard {
                    Eyebrow("Rename app labels · for fun")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "YouTube appears as Instagram by default. Rename any listed app below; this only changes the label shown in Let's Do It, not which app is tracked.",
                        style = MaterialTheme.typography.bodySmall, color = Muted
                    )
                    Spacer(Modifier.height(8.dp))
                    data.apps.take(10).forEach { app ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(app.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = { renamePackage = app.packageName; renameText = app.label }) { Text("Rename") }
                        }
                    }
                    if (renamePackage != null) {
                        OutlinedTextField(
                            value = renameText, onValueChange = { renameText = it },
                            label = { Text("Display name") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { renamePackage = null }) { Text("Cancel") }
                            TextButton(onClick = {
                                val value = renameText.trim()
                                ctx.getSharedPreferences(LABEL_PREFS, Context.MODE_PRIVATE).edit()
                                    .putString(renamePackage, value.ifBlank { appLabel(ctx, renamePackage!!) }).apply()
                                ScreenHabitsCache.reports.clear()
                                ScreenHabitsCache.loadedAt.clear()
                                report = null
                                renamePackage = null
                            }) { Text("Save") }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Screen time is estimated from Android usage records. The tracker cannot reliably infer motivation or what happened inside an app. Usage history is stored on this device only.",
                    Modifier.padding(bottom = 28.dp),
                    style = MaterialTheme.typography.labelMedium, color = Muted
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
