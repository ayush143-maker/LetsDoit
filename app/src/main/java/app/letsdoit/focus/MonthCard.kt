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

// GitHub-style levels: 0 = no focus, 1..4 = deeper blue as you reach 25% / 50% / 100% of your daily goal.
private fun level(m: Long, goal: Int): Int {
    if (m <= 0L) return 0
    val f = m.toFloat() / goal.coerceAtLeast(1).toFloat()
    return if (f >= 1f) 4 else if (f >= 0.5f) 3 else if (f >= 0.25f) 2 else 1
}

private fun levelAlpha(lv: Int): Float {
    return when (lv) {
        1 -> 0.25f
        2 -> 0.5f
        3 -> 0.75f
        else -> 1f
    }
}

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
    val bestIdx = mins.indices.maxByOrNull { mins[it] } ?: 0
    val bestMin = mins.getOrElse(bestIdx) { 0L }
    val bestLabel = if (bestMin > 0L) {
        fmtMin(bestMin) + " · " + days[bestIdx].format(DateTimeFormatter.ofPattern("MMM d"))
    } else {
        "—"
    }
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
                            val lv = level(m, goal)
                            val bg = if (lv > 0) Accent.copy(alpha = levelAlpha(lv))
                            else if (future) Color.Transparent
                            else Line.copy(alpha = 0.55f)
                            val fg = if (lv >= 3) Sand
                            else if (future) Muted.copy(alpha = 0.5f)
                            else Ink
                            val shape = RoundedCornerShape(8.dp)
                            var tile = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(shape)
                                .background(bg)
                            if (d.toEpochDay() == selected) {
                                tile = tile.border(1.5.dp, Ink, shape)
                            } else if (d == today) {
                                tile = tile.border(1.5.dp, Accent, shape)
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
            Box(
                Modifier
                    .padding(horizontal = 2.dp)
                    .size(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Line.copy(alpha = 0.55f))
            )
            listOf(1, 2, 3, 4).forEach { lv ->
                Box(
                    Modifier
                        .padding(horizontal = 2.dp)
                        .size(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Accent.copy(alpha = levelAlpha(lv)))
                )
            }
            Spacer(Modifier.width(6.dp))
            Text("More", style = MaterialTheme.typography.labelSmall, color = Muted)
            Spacer(Modifier.weight(1f))
            Text("Goal " + fmtMin(goal.toLong()), style = MaterialTheme.typography.labelSmall, color = Muted)
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatRow("Focused this month", fmtMin(total))
            StatRow("Active days", "$activeDays of $len")
            StatRow("Goal days", "$goalDays")
            StatRow("Sessions", "$monthSessions")
            StatRow("Best day", bestLabel)
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RBox)
            .background(Cream)
            .border(1.dp, Line, RBox)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = Muted
        )
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
