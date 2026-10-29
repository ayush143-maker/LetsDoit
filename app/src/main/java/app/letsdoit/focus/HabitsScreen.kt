package app.letsdoit.focus

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

val Danger = Color(0xFFD92D20)

@Composable
fun HabitsScreen(now: Long, onStartFocus: () -> Unit) {
    val ctx = LocalContext.current
    val has = remember(now / 1000L) { UsageTracker.hasAccess(ctx) }
    var refresh by remember { mutableStateOf(0) }
    var week by rememberSaveable { mutableStateOf(false) }
    val report by produceState<UsageReport?>(null, has, refresh) {
        value = if (has) {
            withContext(Dispatchers.Default) { runCatching { UsageTracker.load(ctx) }.getOrNull() }
        } else {
            null
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
    ) {
        PageHeader("Habits", "Your phone usage, judged honestly.")
        val r = report
        if (!has) {
            UsageAccessCard()
        } else if (r == null) {
            Text(
                "Reading your usage…",
                style = MaterialTheme.typography.bodyMedium,
                color = Muted
            )
        } else {
            HabitsReport(r, week, { week = it }, { refresh += 1 }, onStartFocus)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun UsageAccessCard() {
    val ctx = LocalContext.current
    LCard {
        Eyebrow("Usage access")
        Spacer(Modifier.height(8.dp))
        Text("I can't grade what I can't see.", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Allow Usage access so Let's Do It can read how long you spend in each app. It all stays on this device. In return you get a grade, a roast, and orders.",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted
        )
        Spacer(Modifier.height(14.dp))
        PButton("Grant usage access", Modifier.fillMaxWidth()) { UsageTracker.openSettings(ctx) }
        Spacer(Modifier.height(10.dp))
        PButton("Greyed out? Allow restricted settings", Modifier.fillMaxWidth(), filled = false) {
            openAppInfo(ctx)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Find Let's Do It in the list and switch on usage tracking. If it is greyed out (Android 13+, APK installed outside the Play Store): open App info, tap the three-dot menu, choose Allow restricted settings, then try again.",
            style = MaterialTheme.typography.labelMedium,
            color = Muted
        )
    }
}

@Composable
private fun HabitsReport(
    r: UsageReport,
    week: Boolean,
    onWeek: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onStartFocus: () -> Unit
) {
    val ctx = LocalContext.current
    val prefs by Repo.prefs.collectAsState()
    val sessions by Repo.sessions.collectAsState()
    val today = LocalDate.now()
    val byDay = remember(sessions) { Stats.minutesByDay(sessions) }

    val dayCount = if (week) 7 else 1
    val total = if (week) r.days.sumOf { it.totalMs } else r.days.last().totalMs
    val dist = if (week) r.days.sumOf { it.distractMs } else r.days.last().distractMs
    val apps = if (week) r.week else r.today
    val focusMin = if (week) {
        (0..6).sumOf { byDay[today.minusDays(it.toLong())] ?: 0L }
    } else {
        byDay[today] ?: 0L
    }
    val focusMs = focusMin * 60_000L
    val distPerDayMin = dist / dayCount / 60_000L
    val grade = Teacher.grade(distPerDayMin)
    val topBad = apps.filter { it.distracting }.maxByOrNull { it.ms }
    val gradeColor = when (grade) {
        "A", "B" -> Accent
        "C" -> Ink
        else -> Danger
    }
    val remark = remember(grade, week, topBad?.pkg) {
        Teacher.remark(
            grade,
            topBad?.label ?: "your phone",
            fmtMs(topBad?.ms ?: 0L),
            fmtMs(dist),
            fmtMs(total),
            fmtMs(focusMs)
        )
    }
    val orders = remember(apps, total, dist, focusMs, prefs.blocked, week) {
        Teacher.orders(apps, total / dayCount, dist / dayCount, focusMs / dayCount, prefs.blocked)
    }

    fun act(o: Order) {
        val p = o.pkg
        if (o.kind == 1 && p != null) {
            Repo.updatePrefs { it.copy(blocked = it.blocked + p, shieldOn = true) }
            Toast.makeText(ctx, "Added to your shield", Toast.LENGTH_SHORT).show()
        } else if (o.kind == 2) {
            onStartFocus()
        }
    }

    // Report card
    LCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(76.dp)
                    .clip(RCard)
                    .background(gradeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(grade, color = Sand, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Eyebrow(if (week) "Weekly report card" else "Today's report card")
                Spacer(Modifier.height(2.dp))
                Text(Teacher.verdict(grade), style = MaterialTheme.typography.titleLarge)
                Text(
                    fmtMs(dist) + (if (week) " on distractions this week" else " on distractions today"),
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = Line)
        Spacer(Modifier.height(14.dp))
        Eyebrow("Teacher's remarks")
        Spacer(Modifier.height(6.dp))
        Text(remark, style = MaterialTheme.typography.bodyLarge)
    }

    Spacer(Modifier.height(12.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Pill("Today", !week) { onWeek(false) }
        Pill("Last 7 days", week) { onWeek(true) }
        Spacer(Modifier.weight(1f))
        Text(
            "Refresh",
            color = Accent,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .clip(RSmall)
                .clickable { onRefresh() }
                .padding(8.dp)
        )
    }

    Spacer(Modifier.height(12.dp))
    LCard {
        Row {
            MiniStat("Screen time", fmtMs(total), Modifier.weight(1f))
            MiniStat("Distracting", fmtMs(dist), Modifier.weight(1f), Danger)
            MiniStat("Focused", fmtMs(focusMs), Modifier.weight(1f), Accent)
        }
    }

    Spacer(Modifier.height(12.dp))
    LCard {
        Eyebrow("Where your time went")
        Spacer(Modifier.height(6.dp))
        if (apps.isEmpty()) {
            Text(
                "No usage recorded for this period yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = Muted
            )
        } else {
            val maxMs = apps.first().ms
            apps.take(8).forEach { a -> AppUseRow(a, maxMs) }
        }
    }

    Spacer(Modifier.height(12.dp))
    LCard {
        Eyebrow("Last 7 days")
        Spacer(Modifier.height(14.dp))
        val maxDay = maxOf(r.days.maxOf { it.totalMs }, 1L)
        Row(
            Modifier
                .fillMaxWidth()
                .height(150.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            r.days.forEach { d ->
                val h = maxOf(4f, 96f * d.totalMs / maxDay)
                val hd = if (d.totalMs > 0L) h * d.distractMs / d.totalMs else 0f
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        if (d.totalMs > 0L) hoursLabel(d.totalMs) else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Muted
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(h.dp)
                            .clip(RSmall)
                            .background(Accent.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (hd > 0f) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(hd.dp)
                                    .background(Danger)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        d.date.dayOfWeek.name.take(1),
                        style = MaterialTheme.typography.labelSmall,
                        color = Muted
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Danger)
            )
            Spacer(Modifier.width(6.dp))
            Text("Distracting apps", style = MaterialTheme.typography.labelSmall, color = Muted)
            Spacer(Modifier.width(14.dp))
            Box(
                Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Accent.copy(alpha = 0.3f))
            )
            Spacer(Modifier.width(6.dp))
            Text("All other apps", style = MaterialTheme.typography.labelSmall, color = Muted)
        }
    }

    Spacer(Modifier.height(12.dp))
    LCard {
        Eyebrow("Teacher's orders")
        orders.forEachIndexed { i, o ->
            Spacer(Modifier.height(12.dp))
            if (i > 0) {
                HorizontalDivider(color = Line)
                Spacer(Modifier.height(12.dp))
            }
            Text("${i + 1}. ${o.title}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(o.body, style = MaterialTheme.typography.bodyMedium, color = Muted)
            val label = o.actionLabel
            if (label != null) {
                Spacer(Modifier.height(10.dp))
                PButton(label, filled = o.kind == 1) { act(o) }
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    Text(
        "Usage history comes from Android and only goes back about a week. Browsers and system apps count as normal use.",
        style = MaterialTheme.typography.labelSmall,
        color = Muted
    )
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier, color: Color = Ink) {
    Column(modifier) {
        Text(value, color = color, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Muted)
    }
}

@Composable
private fun AppUseRow(a: AppUse, maxMs: Long) {
    val ctx = LocalContext.current
    val icon = remember(a.pkg) {
        runCatching {
            ctx.packageManager.getApplicationIcon(a.pkg).toBitmap(96, 96).asImageBitmap()
        }.getOrNull()
    }
    val frac = (a.ms.toFloat() / maxMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RSmall)
            )
        } else {
            Box(Modifier.size(36.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    a.label,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(fmtMs(a.ms), style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(5.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Line)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(frac)
                        .fillMaxHeight()
                        .background(if (a.distracting) Danger else Accent)
                )
            }
        }
    }
}

private fun hoursLabel(ms: Long): String = String.format("%.1fh", ms / 3_600_000f)
