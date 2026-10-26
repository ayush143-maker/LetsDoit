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

private fun readScreenHabitsReport(ctx: Context, now: Long): ScreenHabitsReport {
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val startDay = today.minusDays(6)
    val startMs = startDay.atStartOfDay(zone).toInstant().toEpochMilli()
    val manager = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val labels = loadApps(ctx).associate { it.pkg to it.label }
    val appDailyMs = mutableMapOf<String, MutableMap<LocalDate, Long>>()
    val events = manager.queryEvents(startMs, now)
    val event = UsageEvents.Event()
    var foregroundPackage: String? = null
    var foregroundSince = 0L

    fun recordSpan(endTime: Long) {
        val pkg = foregroundPackage ?: return
        val finish = minOf(endTime, now)
        var cursor = maxOf(startMs, foregroundSince)
        if (labels.containsKey(pkg)) {
            while (cursor < finish) {
                val day = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
                val nextDay = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val segmentEnd = minOf(finish, nextDay)
                val duration = segmentEnd - cursor
                val byDay = appDailyMs.getOrPut(pkg) { mutableMapOf() }
                byDay[day] = (byDay[day] ?: 0L) + duration
                cursor = segmentEnd
            }
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
                    recordSpan(event.timeStamp)
                    foregroundPackage = pkg
                    foregroundSince = event.timeStamp
                }
            }
            UsageEvents.Event.ACTIVITY_PAUSED,
            UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                if (pkg != null && foregroundPackage == pkg) {
                    recordSpan(event.timeStamp)
                }
            }
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> recordSpan(event.timeStamp)
        }
    }
    recordSpan(now)

    val dates = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val days = dates.map { date ->
        UsageDay(date, appDailyMs.values.sumOf { it[date] ?: 0L })
    }
    val apps = appDailyMs.map { (pkg, usageByDay) ->
        AppUsage(
            packageName = pkg,
            label = labels[pkg] ?: pkg,
            todayMs = usageByDay[today] ?: 0L,
            weekMs = usageByDay.values.sum()
        )
    }.filter { it.weekMs > 0L }.sortedByDescending { it.weekMs }

    return ScreenHabitsReport(days, apps)
}

private fun compactUsage(ms: Long): String {
    val minutes = ms / 60_000L
    return if (minutes >= 60L) (minutes / 60L).toString() + "h"
    else minutes.toString()
}

@Composable
fun ScreenHabitsScreen(now: Long) {
    val ctx = LocalContext.current
    var usageAccess by remember { mutableStateOf(hasUsageStatsAccess(ctx)) }
    var report by remember { mutableStateOf<ScreenHabitsReport?>(null) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    // Re-check after returning from Android Settings without continuously querying usage data.
    LaunchedEffect(now / 2_000L) {
        usageAccess = hasUsageStatsAccess(ctx)
    }
    LaunchedEffect(usageAccess, now / 60_000L) {
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
        PageHeader("Screen Habits", "A clearer picture of your phone time.")
        if (!usageAccess) {
            LCard {
                Eyebrow("Usage access")
                Spacer(Modifier.height(10.dp))
                Text("See where your time goes", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Allow Usage Access so Let's Do It can measure app foreground time and build daily reports. This data stays on your phone; it is not uploaded.",
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
                    "Android did not return usage events. Check Usage Access in system settings and try again.",
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
                    StatTile("Today", fmtMs(data.todayMs / 60_000L), Modifier.weight(1f))
                    StatTile("7-day average", fmtMs(data.weekMs / 7L / 60_000L), Modifier.weight(1f))
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
                        "Daily totals are estimated from Android foreground events and may vary slightly by device.",
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
                            "No recent app usage events were returned. Keep Usage Access enabled and check again later.",
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
                                Text(fmtMs(app.weekMs / 60_000L), style = MaterialTheme.typography.titleSmall)
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
                                "Today · " + fmtMs(app.todayMs / 60_000L),
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
