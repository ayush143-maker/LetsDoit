package app.letsdoit.focus

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

val RingLight = Color(0xFF8AA4FF)

@Composable
fun DeepFocusScreen(a: Active, now: Long) {
    val ctx = LocalContext.current
    val prefs by Repo.prefs.collectAsState()
    val tasks by Repo.tasks.collectAsState()
    val blocks by Repo.blocks.collectAsState()
    var confirmEnd by remember { mutableStateOf(false) }
    val quote = remember(a.startedAt) { Quotes.random() }
    val total = a.plannedMin * 60_000L
    val rem = a.remaining(now)
    val progress = (1f - rem.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val accOn = remember(now / 2000L) { accessibilityOn(ctx) }
    val blockedToday = blocks.count { Stats.day(it.ts) == LocalDate.now() }
    val soft = Sand.copy(alpha = 0.6f)

    val pulse = rememberInfiniteTransition(label = "pulse")
    val breathe by pulse.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )
    val halo = if (a.running) breathe else 1f

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .systemBarsPadding()
            .padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(22.dp))
        Eyebrow(if (a.running) "Deep focus" else "Paused", color = soft)
        Spacer(Modifier.height(8.dp))
        Text(
            if (a.purpose.isNotBlank()) a.purpose else "Focus session",
            color = Sand,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        val linked = tasks.firstOrNull { it.id == a.taskId }
        if (linked != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Task · " + linked.title,
                color = soft,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 10.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawCircle(color = Accent.copy(alpha = 0.10f), radius = size.minDimension / 2f * halo)
                drawArc(
                    color = Sand.copy(alpha = 0.14f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = RingLight,
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
                    color = Sand,
                    fontFamily = LocalMono.current,
                    fontSize = 60.sp,
                    letterSpacing = (-1).sp
                )
                Text(
                    "of ${a.plannedMin} min",
                    color = soft,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            quote.text,
            color = Sand,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        if (quote.by.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text("— " + quote.by, color = soft, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(22.dp))
        val line = when {
            !a.running -> "Paused · shield is off"
            prefs.shieldOn && accOn && prefs.blocked.isNotEmpty() -> "Focus shield on · $blockedToday blocked today"
            prefs.shieldOn && !accOn -> "Shield needs accessibility access"
            else -> "Timer only · shield off"
        }
        Text(line, color = soft, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PButton(
                if (a.running) "Pause" else "Resume",
                Modifier.weight(1f),
                ink = Sand,
                paper = Ink
            ) { if (a.running) Focus.pause() else Focus.resume() }
            PButton("End", Modifier.weight(1f), filled = false, ink = Sand, paper = Ink) {
                confirmEnd = true
            }
        }
        Spacer(Modifier.height(18.dp))
    }

    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            containerColor = Sand,
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
