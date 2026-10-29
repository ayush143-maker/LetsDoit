package app.letsdoit.focus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Idle screen only. While a session runs, MainApp shows DeepFocusScreen instead.
@Composable
fun FocusScreen(
    now: Long,
    presetTask: String?,
    onPresetUsed: () -> Unit,
    onOpenShield: () -> Unit
) {
    val ctx = LocalContext.current
    val prefs by Repo.prefs.collectAsState()
    val tasks by Repo.tasks.collectAsState()
    val sessions by Repo.sessions.collectAsState()
    val blocks by Repo.blocks.collectAsState()
    val last by Repo.lastResult.collectAsState()

    var purpose by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf(25) }
    var taskId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(prefs.defaultMin) { minutes = prefs.defaultMin }
    LaunchedEffect(presetTask) {
        if (presetTask != null) {
            taskId = presetTask
            onPresetUsed()
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val accOn = remember(now / 2000L) { accessibilityOn(ctx) }
    val today = LocalDate.now()
    val byDay = remember(sessions) { Stats.minutesByDay(sessions) }
    val todayMin = byDay[today] ?: 0L
    val streak = Stats.streak(byDay, prefs.goalMin, today)
    val sessionsToday = sessions.count { Stats.day(it.end) == today }
    val blockedToday = blocks.count { Stats.day(it.ts) == today }
    val frac = (todayMin.toFloat() / prefs.goalMin.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val shieldReady = prefs.shieldOn && accOn && prefs.blocked.isNotEmpty()

    val begin = {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        Focus.start(ctx, minutes, purpose, taskId)
        purpose = ""
        taskId = null
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
    ) {
        PageHeader(
            "Let's Do It!",
            today.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
            subtitleBold = true
        )

        // Session card
        LCard {
            Eyebrow("Session length")
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundBtn("\u2212") { minutes = (minutes - 5).coerceAtLeast(5) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        fmtClock(minutes * 60_000L),
                        fontFamily = LocalMono.current,
                        fontSize = 54.sp,
                        letterSpacing = (-1).sp
                    )
                    Text("$minutes minutes", color = Muted, style = MaterialTheme.typography.labelMedium)
                }
                RoundBtn("+") { minutes = (minutes + 5).coerceAtMost(180) }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 25, 45, 60, 90).forEach { m ->
                    PresetChip("${m}m", minutes == m, Modifier.weight(1f)) { minutes = m }
                }
            }

            Spacer(Modifier.height(20.dp))
            Eyebrow("Purpose")
            Spacer(Modifier.height(8.dp))
            Field(purpose, { purpose = it }, "What will you work on?")

            val open = tasks.filter { !it.done }
            if (open.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Eyebrow("Link a task")
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Pill("None", taskId == null) { taskId = null }
                    open.forEach { t ->
                        Pill(t.title.take(24), taskId == t.id) { taskId = t.id }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            PButton("Start focus", Modifier.fillMaxWidth(), onClick = begin)
        }

        // Shield strip
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RCard)
                .background(Sand)
                .border(1.dp, Line, RCard)
                .clickable { onOpenShield() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RBox)
                    .background(if (shieldReady) AccentSoft else Cream),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    NavIcons.Shield,
                    contentDescription = null,
                    tint = if (shieldReady) Accent else Muted,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (shieldReady) "Focus shield is on" else "Focus shield is off",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    if (shieldReady) "${prefs.blocked.size} apps blocked during focus"
                    else if (!accOn) "Needs accessibility access \u00b7 tap to set up"
                    else "Tap to choose apps and turn it on",
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted
                )
            }
            Text("\u203a", style = MaterialTheme.typography.titleLarge, color = Muted)
        }

        // Today
        Spacer(Modifier.height(12.dp))
        LCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 8.dp.toPx()
                        val inset = stroke / 2f
                        val arcSize = Size(size.width - stroke, size.height - stroke)
                        drawArc(
                            color = Line,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        if (frac > 0f) {
                            drawArc(
                                color = Accent,
                                startAngle = -90f,
                                sweepAngle = 360f * frac,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                    }
                    Text("${(frac * 100).toInt()}%", style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Eyebrow("Today's focus")
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$todayMin", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            " / ${prefs.goalMin} min",
                            modifier = Modifier.padding(bottom = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Muted
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$streak day streak \u00b7 $sessionsToday sessions \u00b7 $blockedToday blocked",
                        style = MaterialTheme.typography.labelMedium,
                        color = Muted
                    )
                }
            }
            if (todayMin >= prefs.goalMin) {
                Spacer(Modifier.height(10.dp))
                Text("Daily goal reached. Nicely done.", style = MaterialTheme.typography.labelLarge, color = Accent)
            }
        }

        val l = last
        if (l != null) {
            Spacer(Modifier.height(12.dp))
            LCard {
                Eyebrow(if (l.completed) "Last session \u00b7 completed" else "Last session \u00b7 ended early")
                Spacer(Modifier.height(6.dp))
                Text(
                    fmtMs(l.focusedMs) + (if (l.purpose.isNotBlank()) " \u00b7 " + l.purpose else ""),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        val q = Quotes.today()
        LCard {
            Eyebrow("Thought for today")
            Spacer(Modifier.height(8.dp))
            Text(q.text, style = MaterialTheme.typography.titleMedium)
            if (q.by.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("\u2014 " + q.by, style = MaterialTheme.typography.labelMedium, color = Muted)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PresetChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RSmall)
            .background(if (selected) Ink else Cream)
            .border(1.dp, if (selected) Ink else Line, RSmall)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) Sand else Ink,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
