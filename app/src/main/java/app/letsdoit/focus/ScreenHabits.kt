package app.letsdoit.focus

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

private data class UsageDay(val date: LocalDate, val totalMs: Long)

private data class AppUsage(
    val packageName: String,
    val label: String,
    val todayMs: Long,
    val weekMs: Long
)

private data class ScreenHabitsReport(
    val days: List<UsageDay>,
    val apps: List<AppUsage>
) {
    val todayMs: Long get() = days.lastOrNull()?.totalMs ?: 0L
    val weekMs: Long get() = days.sumOf { it.totalMs }
}

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

/**
 * Prefer Android's aggregated daily UsageStats for historical totals. Usage events are
 * merged into today's app totals as a live-data fallback because aggregated stats can lag
 * while an app is still open. All durations remain milliseconds internally.
 */
private fun readScreenHabitsReport(ctx: Context, now: Long): ScreenHabitsReport {
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val dates = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val manager = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val labels = loadApps(ctx).associate { it.pkg to it.label }
    val allDailyMs = mutableMapOf<String, MutableMap<LocalDate, Long>>()

    dates.forEach { date ->
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val nextDay = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = minOf(nextDay, now)
        if (dayEnd <= dayStart) return@forEach

        val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, dayStart, dayEnd)
        stats.orEmpty().forEach { stat ->
            val duration = stat.totalTimeInForeground
            if (duration > 0L) {
                val byDay = allDailyMs.getOrPut(stat.packageName) { mutableMapOf() }
                byDay[date] = maxOf(byDay[date] ?: 0L, duration)
            }
        }
    }

    // Reconstruct today's foreground intervals so a currently open app can appear before
    // Android refreshes its daily aggregate. Historical totals continue to use UsageStats.
    val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
    val events = manager.queryEvents(todayStart, now)
    val event = UsageEvents.Event()
    var foregroundPackage: String? = null
    var foregroundSince = 0L

    fun recordForegroundSpan(endTime: Long) {
        val pkg = foregroundPackage ?: return
        val end = minOf(endTime, now)
        if (end > foregroundSince) {
            val duration = end - foregroundSince
            val byDay = allDailyMs.getOrPut(pkg) { mutableMapOf() }
            byDay[today] = maxOf(byDay[today] ?: 0L, duration + (byDay[today] ?: 0L))
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
                if (pkg != null && foregroundPackage != pkg) {
                    recordForegroundSpan(event.timeStamp)
                    foregroundPackage = pkg
                    foregroundSince = event.timeStamp
                }
            }
            UsageEvents.Event.ACTIVITY_PAUSED,
            UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                if (pkg != null && foregroundPackage == pkg) {
                    recordForegroundSpan(event.timeStamp)
                }
            }
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> recordForegroundSpan(event.timeStamp)
        }
    }
    recordForegroundSpan(now)

    val days = dates.map { date ->
        UsageDay(date, allDailyMs.values.sumOf { it[date] ?: 0L })
    }
    val apps = allDailyMs.mapNotNull { (pkg, usageByDay) ->
        val label = labels[pkg] ?: return@mapNotNull null
        val week = usageByDay.values.sum()
        if (week <= 0L) null else AppUsage(
            packageName = pkg,
            label = label,
            todayMs = usageByDay[today] ?: 0L,
            weekMs = week
        )
    }.sortedByDescending { it.weekMs }

    return ScreenHabitsReport(days, apps)
}

private fun usageDuration(ms: Long): String {
    if (ms <= 0L) return "0 min"
    val minutes = ms / 60_000L
    if (minutes == 0L) return "<1 min"
    return if (minutes >= 60L) "${minutes / 60L}h ${minutes % 60L}m" else "$minutes min"
}

private fun compactUsage(ms: Long): String {
    val minutes = ms / 60_000L
    return if (minutes >= 60L) "${minutes / 60L}h"
    else if (minutes > 0L) "$minutes min"
    else "0"
}

private fun straightTalk(data: ScreenHabitsReport): String {
    val todayMs = data.todayMs
    val weekMs = data.weekMs
    if (weekMs <= 0L) {
        return "No reliable usage history has come back yet. Check Usage Access, then use your phone normally and check again. No fake numbers, no guesses."
    }
    if (todayMs <= 0L) {
        return "Today's total has not updated yet, even though earlier usage exists. Do not trust this zero as a clean day. Android may be lagging; check again shortly."
    }

    val topToday = data.apps.filter { it.todayMs > 0L }.maxByOrNull { it.todayMs }
    val appName = topToday?.label ?: data.apps.firstOrNull()?.label
    val hours = todayMs / 3_600_000L
    return when {
        hours >= 6L -> "Tough love: ${usageDuration(todayMs)} on your phone today is a serious amount of attention handed away. ${appName?.let { "Put $it on a limit, " } ?: ""}then finish one 25-minute focus session before opening distractions again."
        todayMs >= 3L * 60L * 60L * 1000L ->
            "Real talk: you have already given your phone ${usageDuration(todayMs)} today. That is more than a quick check. ${appName?.let { "Set a boundary for $it and " } ?: ""}protect your next 25 minutes for something you chose."
        else ->
            "You are at ${usageDuration(todayMs)} today. Keep your attention intentional: finish one focused 25-minute block before you drift into another scroll."
    }
}

@Composable
fun ScreenHabitsScreen(now: Long) {
    val ctx = LocalContext.current
    var usageAccess by remember { mutableStateOf(hasUsageStatsAccess(ctx)) }
    var report by remember { mutableStateOf<ScreenHabitsReport?>(null) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    // Check settings occasionally, but query the seven-day report less often to avoid
    // repeatedly scanning usage history and wasting battery.
    LaunchedEffect(now / 10_000L) {
        usageAccess = hasUsageStatsAccess(ctx)
    }
    LaunchedEffect(usageAccess, now / 300_000L) {
        if (!usageAccess) {
            report = null
            loading = false
            failed = false
        } else {
            loading = true
            failed = false
            try {
                report = withContext(Dispatchers.IO) { readScreenHabitsReport(ctx, now) }
            } catch (_: Exception) {
                failed = true
            } finally {
                loading = false
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
    ) {
        PageHeader("Screen Habits", "Know where your attention goes.")
        if (!usageAccess) {
            LCard {
                Eyebrow("Usage access")
                Spacer(Modifier.height(10.dp))
                Text("See where your time goes", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Allow Usage Access so Let's Do It can measure foreground app time and build daily reports. This data stays on your phone; it is not uploaded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
                Spacer(Modifier.height(14.dp))
                PButton("Open Usage Access settings", Modifier.fillMaxWidth()) {
                    openUsageAccessSettings(ctx)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "In Android Settings, select Let's Do It and enable usage access. Return here to load your report.",
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted
                )
            }
        } else if (loading && report == null) {
            LCard {
                Eyebrow("Screen time")
                Spacer(Modifier.height(8.dp))
                Text("Reading recent usage…", style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
        } else if (failed) {
            LCard {
                Eyebrow("Screen time unavailable")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Android did not return usage data. Check Usage Access in system settings and try again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
                Spacer(Modifier.height(12.dp))
                PButton("Open Usage Access settings", Modifier.fillMaxWidth()) {
                    openUsageAccessSettings(ctx)
                }
            }
        } else {
            val data = report
            if (data != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Today", usageDuration(data.todayMs), Modifier.weight(1f))
                    StatTile("7-day daily avg", usageDuration(data.weekMs / 7L), Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                LCard {
                    Eyebrow("Straight talk")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        straightTalk(data),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Ink
                    )
                    if (data.weekMs > 0L && data.todayMs > 0L) {
                        Spacer(Modifier.height(10.dp))
                        val average = data.weekMs / 7L
                        val ratio = if (average > 0L) data.todayMs.toDouble() / average.toDouble() else 0.0
                        Text(
                            when {
                                ratio >= 1.25 -> "Today is running above your recent daily average. Notice the pattern and change the next choice."
                                ratio <= 0.75 -> "You are below your recent daily average so far. Keep the rest of today intentional."
                                else -> "You are around your recent daily average. Decide where the next hour goes instead of letting the phone decide."
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Muted
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                LCard {
                    Eyebrow("Screen time · 7 days")
                    Spacer(Modifier.height(14.dp))
                    val maxDay = maxOf(data.days.maxOfOrNull { it.totalMs } ?: 0L, 60_000L)
                    Row(
                        Modifier.fillMaxWidth().height(142.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        data.days.forEach { day ->
                            Column(
                                Modifier.weight(1f).fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Text(
                                    compactUsage(day.totalMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Muted
                                )
                                Spacer(Modifier.height(5.dp))
                                val barHeight = if (day.totalMs <= 0L) 3f
                                    else maxOf(5f, 84f * day.totalMs.toFloat() / maxDay.toFloat())
                                Box(
                                    Modifier.fillMaxWidth()
                                        .height(barHeight.dp)
                                        .clip(RSmall)
                                        .background(
                                            if (day.date == LocalDate.now()) Accent
                                            else Accent.copy(alpha = 0.30f)
                                        )
                                )
                                Spacer(Modifier.height(7.dp))
                                Text(
                                    day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Muted
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Daily totals use Android's app-usage records, with today's live events used to cover delayed updates. Totals may vary slightly by device.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Muted
                    )
                }
                Spacer(Modifier.height(12.dp))
                LCard {
                    Eyebrow("Most-used apps · 7 days")
                    Spacer(Modifier.height(12.dp))
                    if (data.apps.isEmpty()) {
                        Text(
                            "Android has not returned app-level history yet. Keep Usage Access enabled and check again shortly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Muted
                        )
                    } else {
                        val maxApp = maxOf(data.apps.first().weekMs, 1L)
                        data.apps.take(8).forEachIndexed { index, app ->
                            if (index > 0) {
                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider(color = Line)
                                Spacer(Modifier.height(12.dp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    app.label,
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(usageDuration(app.weekMs), style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(Modifier.height(5.dp))
                            Box(
                                Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(6.dp))
                                    .background(Line)
                            ) {
                                Box(
                                    Modifier.fillMaxHeight()
                                        .fillMaxWidth((app.weekMs.toFloat() / maxApp.toFloat()).coerceIn(0f, 1f))
                                        .background(Accent)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Today · " + usageDuration(app.todayMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = Muted
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Only app names and foreground time are used for these reports. No screen content, messages, or browser pages are read.",
                    Modifier.padding(bottom = 28.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
