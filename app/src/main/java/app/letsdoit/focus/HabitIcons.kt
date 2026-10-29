package app.letsdoit.focus

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Streak flame used for the Habits tab.
object HabitIcons {
    val Flame: ImageVector = ImageVector.Builder(
        name = "Habits",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 3f)
        curveTo(12f, 3f, 6.5f, 8.5f, 6.5f, 14f)
        curveTo(6.5f, 17.6f, 9f, 20.5f, 12f, 20.5f)
        curveTo(15f, 20.5f, 17.5f, 17.6f, 17.5f, 14f)
        curveTo(17.5f, 11.5f, 16f, 9.5f, 14.5f, 8f)
        curveTo(14.2f, 9.5f, 13.3f, 10.5f, 12.4f, 10.6f)
        curveTo(12.6f, 8f, 12.4f, 5.5f, 12f, 3f)
        close()
    }.build()
}
