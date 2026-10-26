package app.letsdoit.focus

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun navIcon(name: String, build: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply(build).build()

private fun ImageVector.Builder.stroked(width: Float = 1.8f, block: PathBuilder.() -> Unit) {
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = block
    )
}

object NavIcons {
    // Timer
    val Focus: ImageVector by lazy {
        navIcon("nav_focus") {
            stroked {
                moveTo(4f, 13.5f)
                arcTo(8f, 8f, 0f, true, true, 20f, 13.5f)
                arcTo(8f, 8f, 0f, true, true, 4f, 13.5f)
                close()
            }
            stroked {
                moveTo(12f, 13.5f)
                lineTo(12f, 9.5f)
            }
            stroked {
                moveTo(9.5f, 2.5f)
                lineTo(14.5f, 2.5f)
            }
        }
    }

    // Checkbox with tick
    val Tasks: ImageVector by lazy {
        navIcon("nav_tasks") {
            stroked {
                moveTo(5f, 4f)
                lineTo(19f, 4f)
                lineTo(19f, 20f)
                lineTo(5f, 20f)
                close()
            }
            stroked {
                moveTo(8.5f, 12.2f)
                lineTo(11f, 14.7f)
                lineTo(15.5f, 9.5f)
            }
        }
    }

    // Bars
    val Insights: ImageVector by lazy {
        navIcon("nav_insights") {
            stroked(2.4f) {
                moveTo(6f, 20f)
                lineTo(6f, 13f)
                moveTo(12f, 20f)
                lineTo(12f, 5f)
                moveTo(18f, 20f)
                lineTo(18f, 9f)
            }
        }
    }

    // Phone outline with usage bars
    val Habits: ImageVector by lazy {
        navIcon("nav_habits") {
            stroked {
                moveTo(8f, 2.5f)
                lineTo(16f, 2.5f)
                curveTo(17.1f, 2.5f, 18f, 3.4f, 18f, 4.5f)
                lineTo(18f, 19.5f)
                curveTo(18f, 20.6f, 17.1f, 21.5f, 16f, 21.5f)
                lineTo(8f, 21.5f)
                curveTo(6.9f, 21.5f, 6f, 20.6f, 6f, 19.5f)
                lineTo(6f, 4.5f)
                curveTo(6f, 3.4f, 6.9f, 2.5f, 8f, 2.5f)
                close()
            }
            stroked(1.6f) {
                moveTo(9f, 15.5f)
                lineTo(9f, 12.5f)
                moveTo(12f, 15.5f)
                lineTo(12f, 9.5f)
                moveTo(15f, 15.5f)
                lineTo(15f, 7.5f)
            }
            stroked(1.4f) {
                moveTo(10f, 18.5f)
                lineTo(14f, 18.5f)
            }
        }
    }

    // Shield
    val Shield: ImageVector by lazy {
        navIcon("nav_shield") {
            stroked {
                moveTo(12f, 3f)
                lineTo(19.5f, 6f)
                lineTo(19.5f, 12f)
                curveTo(19.5f, 16.5f, 16.5f, 19.8f, 12f, 21f)
                curveTo(7.5f, 19.8f, 4.5f, 16.5f, 4.5f, 12f)
                lineTo(4.5f, 6f)
                close()
            }
        }
    }
}
