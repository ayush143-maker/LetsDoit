package app.letsdoit.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// Monthly calendar: the more you focus on a day, the deeper the blue.
@Composable
fun MonthCard(byDay: Map<LocalDate, Long>, sessions: List<Session>, goal: Int) {
    val today = LocalDate.now()
    var offset by rememberSaveable { mutableStateOf(0) }
    var selected by rememberSaveable { mutableStateOf(-1L) }

    val ym = YearMonth.from(today).plusMonths(offset.toLong())
    val len = ym.lengthOfMonth()
    val lead = ym.atDay(1).dayOfWeek.value - 1
    val rows = (lead + len + 6) / 7
    val days = (1..len).map { ym.atDay(it) }
    val mins = days.map { byDay[it] ?: 0L }
    val total = mins.sum()
    val activeDays = mins.count { it > 0L }
    val goalDays = mins.count { it >= goal }
    val bestMin = mins.maxOrNull() ?: 0L
    val monthSessions = sessions.count { YearMonth.from(Stats.day(it.end)) == ym }
    val title = ym.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()) + " " + ym.year

    LCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Monthly view")
                Spacer(Modifier.height(2.dp))
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            RoundBtn("\u2039") {
                offset -= 1
                selected = -1L
            }
            Spacer(Modifier.width(8.dp))
            if (offset < 0) {
                RoundBtn("\u203a") {
                    offset += 1
                    selected = -1L
                }
            } else {
                Spacer(Modifier.size(40.dp))
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { w ->
                Text(
                    w,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (r in 0 until rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (c in 0 until 7) {
                        val idx = r * 7 + c - lead
                        if (idx in 0 until len) {
                            val d = days[idx]
                            val m = mins[idx]
                            val future = d.isAfter(today)
                            val f = (m.toFloat() / goal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                            val bg = if (m > 0L) Accent.copy(alpha = 0.18f + 0.82f * f)
                            else if (future) Color.Transparent
                            else Line.copy(alpha = 0.55f)
                            val fg = if (m > 0L && f > 0.45f) Sand
                            else if (future) Muted.copy(alpha = 0.5f)
                            else Ink
                            val shape = RoundedCornerShape(8.dp)
                            var tile = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(shape)
                                .background(bg)
                            if (d.toEpochDay() == selected) {
                                tile = tile.border(1.5.dp, Accent, shape)
                            } else if (d == today) {
                                tile = tile.border(1.5.dp, Ink, shape)
                            }
                            Box(
                                tile.clickable {
                                    selected = if (selected == d.toEpochDay()) -1L else d.toEpochDay()
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${d.dayOfMonth}",
                                    color = fg,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        } else {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Less", style = MaterialTheme.typography.labelSmall, color = Muted)
            Spacer(Modifier.width(6.dp))
            listOf(0.18f, 0.4f, 0.7f, 1f).forEach { a ->
                Box(
                    Modifier
                        .padding(horizontal = 2.dp)
                        .size(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Accent.copy(alpha = a))
                )
            }
            Spacer(Modifier.width(6.dp))
            Text("More", style = MaterialTheme.typography.labelSmall, color = Muted)
            Spacer(Modifier.weight(1f))
            Text("Goal $goal min/day", style = MaterialTheme.typography.labelSmall, color = Muted)
        }

        if (selected >= 0L) {
            val d = LocalDate.ofEpochDay(selected)
            val daySessions = sessions.filter { Stats.day(it.end) == d }
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RBox)
                    .background(Cream)
                    .border(1.dp, Line, RBox)
                    .padding(14.dp)
            ) {
                Text(
                    d.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (daySessions.isEmpty()) "No focus sessions"
                    else fmtMin(byDay[d] ?: 0L) + " across " + daySessions.size + " sessions",
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted
                )
                daySessions.takeLast(5).forEach { s ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        fmtMs(s.focusedMs) + (if (s.purpose.isNotBlank()) " \u00b7 " + s.purpose else ""),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row {
            MonthStat("Focused", fmtMin(total), Modifier.weight(1f))
            MonthStat("Active days", "$activeDays / $len", Modifier.weight(1f))
            MonthStat("Goal days", "$goalDays", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row {
            MonthStat("Sessions", "$monthSessions", Modifier.weight(1f))
            MonthStat(
                "Avg per active day",
                if (activeDays > 0) fmtMin(total / activeDays) else "\u2014",
                Modifier.weight(1f)
            )
            MonthStat(
                "Best day",
                if (bestMin > 0L) fmtMin(bestMin) else "\u2014",
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Muted)
    }
}
