package app.letsdoit.focus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun FocusScreen(
    now: Long,
    presetTask: String?,
    onPresetUsed: () -> Unit,
    onOpenShield: () -> Unit
) {
    val ctx = LocalContext.current
    val active by Repo.active.collectAsState()
    val prefs by Repo.prefs.collectAsState()
    val tasks by Repo.tasks.collectAsState()
    val sessions by Repo.sessions.collectAsState()
    val blocks by Repo.blocks.collectAsState()
    val last by Repo.lastResult.collectAsState()

    var purpose by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf(25) }
    var taskId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmEnd by remember { mutableStateOf(false) }

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
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LogoMark(40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Let's Do It", style = MaterialTheme.typography.titleMedium)
                Text(
                    today.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted
                )
            }
        }
        Spacer(Modifier.height(30.dp))

        val a = active
        if (a == null) {
            Eyebrow("Ready when you are")
            Spacer(Modifier.height(8.dp))
            Text("What will you work on?", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(20.dp))
            Field(purpose, { purpose = it }, "Give this session a purpose")

            val open = tasks.filter { !it.done }
            if (open.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Eyebrow("Link a task")
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Pill("No linked task", taskId == null) { taskId = null }
                    open.forEach { t ->
                        Pill(t.title.take(24), taskId == t.id) { taskId = t.id }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Eyebrow("Duration")
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15, 25, 45, 60, 90).forEach { m ->
                    Pill("$m min", minutes == m) { minutes = m }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundBtn("−") { minutes = (minutes - 5).coerceAtLeast(5) }
                Text(
                    "$minutes min",
                    modifier = Modifier.padding(horizontal = 18.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                RoundBtn("+") { minutes = (minutes + 5).coerceAtMost(180) }
            }

            Spacer(Modifier.height(22.dp))
            PButton("Start focus", Modifier.fillMaxWidth(), onClick = begin)
            Spacer(Modifier.height(14.dp))
            val shieldReady = prefs.shieldOn && accOn && prefs.blocked.isNotEmpty()
            Text(
                if (shieldReady) "Focus shield ready · ${prefs.blocked.size} apps"
                else "Focus shield not active · tap to set up",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenShield() }
                    .padding(vertical = 6.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                color = Muted
            )

            val l = last
            if (l != null) {
                Spacer(Modifier.height(16.dp))
                LCard {
                    Eyebrow(if (l.completed) "Focus complete" else "Last session ended")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        fmtMs(l.focusedMs) + (if (l.purpose.isNotBlank()) " · " + l.purpose else ""),
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (l.completed) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Time well spent.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Muted
                        )
                    }
                }
            }
        } else {
            val total = a.plannedMin * 60_000L
            val rem = a.remaining(now)
            val progress = (1f - rem.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            Eyebrow(if (a.running) "Focus in progress" else "Paused · shield is off")
            Spacer(Modifier.height(8.dp))
            Text(
                if (a.purpose.isNotBlank()) a.purpose else "Focus session",
                style = MaterialTheme.typography.headlineLarge
            )
            val linked = tasks.firstOrNull { it.id == a.taskId }
            if (linked != null) {
                Text("Task · " + linked.title, style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
            Spacer(Modifier.height(26.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(280.dp)) {
                    val stroke = 10.dp.toPx()
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
                    drawArc(
                        color = Ink,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        fmtClock(rem),
                        fontFamily = LocalMono.current,
                        fontSize = 58.sp,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        "of ${a.plannedMin} min",
                        style = MaterialTheme.typography.labelMedium,
                        color = Muted
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PButton(
                    if (a.running) "Pause focus" else "Resume focus",
                    Modifier.weight(1f)
                ) { if (a.running) Focus.pause() else Focus.resume() }
                PButton("End session", Modifier.weight(1f), filled = false) { confirmEnd = true }
            }
            Spacer(Modifier.height(14.dp))
            val shieldLine = when {
                !a.running -> "Paused · shield is off"
                prefs.shieldOn && accOn && prefs.blocked.isNotEmpty() ->
                    "Focus shield on · $blockedToday blocked today"
                prefs.shieldOn && !accOn -> "Shield needs accessibility access · tap to fix"
                else -> "Timer only · shield off"
            }
            Text(
                shieldLine,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenShield() }
                    .padding(vertical = 6.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                color = Muted
            )
        }

        Spacer(Modifier.height(26.dp))
        LCard {
            Eyebrow("Today's focus")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$todayMin", style = MaterialTheme.typography.displaySmall)
                Text(
                    " / ${prefs.goalMin} min",
                    modifier = Modifier.padding(bottom = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
            }
            Spacer(Modifier.height(10.dp))
            val frac = (todayMin.toFloat() / prefs.goalMin.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Line)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(frac)
                        .fillMaxHeight()
                        .background(Ink)
                )
            }
            Spacer(Modifier.height(14.dp))
            Row {
                Column(Modifier.weight(1f)) {
                    Text("$streak", style = MaterialTheme.typography.titleLarge)
                    Text("Day streak", style = MaterialTheme.typography.labelMedium, color = Muted)
                }
                Column(Modifier.weight(1f)) {
                    Text("$sessionsToday", style = MaterialTheme.typography.titleLarge)
                    Text("Sessions", style = MaterialTheme.typography.labelMedium, color = Muted)
                }
                Column(Modifier.weight(1f)) {
                    Text("$blockedToday", style = MaterialTheme.typography.titleLarge)
                    Text("Blocked", style = MaterialTheme.typography.labelMedium, color = Muted)
                }
            }
            if (todayMin >= prefs.goalMin) {
                Spacer(Modifier.height(10.dp))
                Text("Daily goal reached. Nicely done.", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.height(14.dp))
        val q = Quotes.today()
        LCard {
            Eyebrow("Thought for today")
            Spacer(Modifier.height(8.dp))
            Text(q.text, style = MaterialTheme.typography.titleMedium)
            if (q.by.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("— " + q.by, style = MaterialTheme.typography.labelMedium, color = Muted)
            }
        }
        Spacer(Modifier.height(32.dp))
    }

    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            containerColor = Cream,
            title = { Text("End this session?") },
            text = {
                Text("Your focused time is saved as an interrupted session. The focus shield will turn off.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmEnd = false
                    Focus.finish(ctx, false)
                }) { Text("End session", color = Ink) }
            },
            dismissButton = {
                TextButton(onClick = { confirmEnd = false }) { Text("Keep focusing", color = Muted) }
            }
        )
    }
}
