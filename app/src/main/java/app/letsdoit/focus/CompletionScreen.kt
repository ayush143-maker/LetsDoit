package app.letsdoit.focus

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun CompletionScreen(s: Session, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sessions by Repo.sessions.collectAsState()
    val prefs by Repo.prefs.collectAsState()
    val quote = remember(s.id) { Quotes.random() }
    val ring = remember { Animatable(0f) }
    val tick = remember { Animatable(0f) }
    val fade = remember { Animatable(0f) }

    LaunchedEffect(s.id) {
        ring.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
        tick.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
        if (prefs.vibrate) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        fade.animateTo(1f, tween(700))
    }

    val today = LocalDate.now()
    val byDay = remember(sessions) { Stats.minutesByDay(sessions) }
    val todayMin = byDay[today] ?: 0L
    val streak = Stats.streak(byDay, prefs.goalMin, today)

    Column(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .systemBarsPadding()
            .padding(26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Canvas(Modifier.size(200.dp)) {
            val w = size.width
            val stroke = 8.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(w - stroke, w - stroke)
            drawCircle(
                color = Accent.copy(alpha = 0.10f * ring.value),
                radius = w / 2f * (0.85f + 0.15f * ring.value)
            )
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
                color = Accent,
                startAngle = -90f,
                sweepAngle = 360f * ring.value,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            val p1 = Offset(w * 0.30f, w * 0.52f)
            val p2 = Offset(w * 0.45f, w * 0.67f)
            val p3 = Offset(w * 0.72f, w * 0.36f)
            val t = tick.value
            val t1 = (t * 2f).coerceIn(0f, 1f)
            val t2 = (t * 2f - 1f).coerceIn(0f, 1f)
            val lw = 10.dp.toPx()
            if (t1 > 0f) {
                drawLine(
                    color = Ink,
                    start = p1,
                    end = Offset(p1.x + (p2.x - p1.x) * t1, p1.y + (p2.y - p1.y) * t1),
                    strokeWidth = lw,
                    cap = StrokeCap.Round
                )
            }
            if (t2 > 0f) {
                drawLine(
                    color = Ink,
                    start = p2,
                    end = Offset(p2.x + (p3.x - p2.x) * t2, p2.y + (p3.y - p2.y) * t2),
                    strokeWidth = lw,
                    cap = StrokeCap.Round
                )
            }
        }
        Spacer(Modifier.height(30.dp))
        Column(Modifier.alpha(fade.value), horizontalAlignment = Alignment.CenterHorizontally) {
            Eyebrow("Session complete")
            Spacer(Modifier.height(8.dp))
            Text(
                fmtMs(s.focusedMs) + " of focus",
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center
            )
            if (s.purpose.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    s.purpose,
                    color = Muted,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                quote.text,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            if (quote.by.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("— " + quote.by, color = Muted, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatChip("Today", "$todayMin / ${prefs.goalMin} min", Modifier.weight(1f))
                StatChip("Streak", if (streak == 1) "1 day" else "$streak days", Modifier.weight(1f))
            }
        }
        Spacer(Modifier.weight(1f))
        Column(Modifier.alpha(fade.value)) {
            PButton(
                "Start another ${s.plannedMin} min",
                Modifier.fillMaxWidth()
            ) { Focus.start(ctx, s.plannedMin, s.purpose, s.taskId) }
            Spacer(Modifier.height(10.dp))
            PButton("Done", Modifier.fillMaxWidth(), filled = false) { onDone() }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RBox)
            .background(Sand)
            .border(1.dp, Line, RBox)
            .padding(14.dp)
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, color = Muted, style = MaterialTheme.typography.labelMedium)
    }
}
