package app.letsdoit.focus

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun InsightsScreen(now: Long) {
    val ctx = LocalContext.current
    val sessions by Repo.sessions.collectAsState()
    val prefs by Repo.prefs.collectAsState()
    val blocks by Repo.blocks.collectAsState()

    val today = LocalDate.now()
    val byDay = remember(sessions) { Stats.minutesByDay(sessions) }
    val streak = Stats.streak(byDay, prefs.goalMin, today)
    val totalMin = sessions.sumOf { it.focusedMs } / 60_000L
    val completed = sessions.count { it.completed }
    val week = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val weekMins = week.map { byDay[it] ?: 0L }
    val maxW = maxOf(weekMins.maxOrNull() ?: 0L, 1L)
    val weekTotal = weekMins.sum()
    val best = Stats.bestHour(sessions)
    val weekStart = today.minusDays(6)
    val weekBlocks = blocks.filter { !Stats.day(it.ts).isBefore(weekStart) }
    val topPkg = weekBlocks.groupingBy { it.pkg }.eachCount().maxByOrNull { it.value }?.key

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
    ) {
        PageHeader("Insights", "See where your time really goes.")

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Day streak", "$streak", Modifier.weight(1f))
            StatTile("This week", fmtMin(weekTotal), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Total focused", fmtMin(totalMin), Modifier.weight(1f))
            StatTile("Sessions completed", "$completed", Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        LCard {
            Eyebrow("Minutes by day")
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                week.forEachIndexed { i, d ->
                    val m = weekMins[i]
                    val h = maxOf(4f, 96f * m / maxW)
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            if (m > 0L) "$m" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = Muted
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(h.dp)
                                .clip(RSmall)
                                .background(if (d == today) Accent else Accent.copy(alpha = 0.3f))
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            d.dayOfWeek.name.take(1),
                            style = MaterialTheme.typography.labelSmall,
                            color = Muted
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Includes time from interrupted sessions. Sessions are credited to the day they end.",
                style = MaterialTheme.typography.labelSmall,
                color = Muted
            )
        }

        Spacer(Modifier.height(14.dp))
        MonthCard(byDay, sessions, prefs.goalMin)

        Spacer(Modifier.height(14.dp))
        LCard {
            Eyebrow("Patterns")
            Spacer(Modifier.height(10.dp))
            Text(
                if (best != null) "You focus best around ${Stats.hourLabel(best)}."
                else "Complete a few sessions to see when you focus best.",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(10.dp))
            Text(
                if (weekBlocks.isEmpty()) "No distractions blocked this week."
                else "${weekBlocks.size} distractions blocked this week" +
                    (if (topPkg != null) " · most tempting: " + appLabel(ctx, topPkg) else ""),
                style = MaterialTheme.typography.bodyMedium,
                color = Muted
            )
        }

        Spacer(Modifier.height(14.dp))
        LCard {
            Eyebrow("Focus history")
            Spacer(Modifier.height(10.dp))
            if (sessions.isEmpty()) {
                Text(
                    "Every focused minute counts. Your session history will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
            } else {
                val recent = sessions.takeLast(8).reversed()
                recent.forEachIndexed { i, s ->
                    if (i > 0) HorizontalDivider(color = Line)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (s.purpose.isNotBlank()) s.purpose else "Focus session",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                Stats.whenLabel(s.end) + " · " + (if (s.completed) "Completed" else "Interrupted"),
                                style = MaterialTheme.typography.labelMedium,
                                color = Muted
                            )
                        }
                        Text(fmtMs(s.focusedMs), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
