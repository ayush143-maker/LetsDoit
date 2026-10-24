package app.letsdoit.focus

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Cream = Color(0xFFF7F1E6)
val Sand = Color(0xFFEDE5D4)
val Line = Color(0xFFDCD2BD)
val Ink = Color(0xFF0C0C0C)
val Muted = Color(0xFF7B7466)

val LocalMono = staticCompositionLocalOf<FontFamily> { FontFamily.Monospace }

private fun loadFamily(
    ctx: Context,
    files: List<Pair<String, FontWeight>>,
    fallback: FontFamily
): FontFamily {
    val fonts = files.mapNotNull { (file, weight) ->
        try {
            ctx.assets.open("fonts/$file").close()
            Font(assetManager = ctx.assets, path = "fonts/$file", weight = weight)
        } catch (e: Exception) {
            null
        }
    }
    return if (fonts.isEmpty()) fallback else FontFamily(fonts)
}

private fun st(f: FontFamily, size: Int, w: FontWeight = FontWeight.Normal, ls: Double = 0.0) =
    TextStyle(
        fontFamily = f,
        fontWeight = w,
        fontSize = size.sp,
        lineHeight = (size * 1.35).sp,
        letterSpacing = ls.sp
    )

@Composable
fun LdiTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val sans = remember {
        loadFamily(
            ctx,
            listOf(
                "IBMPlexSans-Regular.ttf" to FontWeight.Normal,
                "IBMPlexSans-Medium.ttf" to FontWeight.Medium,
                "IBMPlexSans-SemiBold.ttf" to FontWeight.SemiBold,
                "IBMPlexSans-Bold.ttf" to FontWeight.Bold
            ),
            FontFamily.SansSerif
        )
    }
    val mono = remember {
        loadFamily(
            ctx,
            listOf(
                "IBMPlexMono-Regular.ttf" to FontWeight.Normal,
                "IBMPlexMono-Medium.ttf" to FontWeight.Medium
            ),
            FontFamily.Monospace
        )
    }
    val typo = remember(sans) {
        Typography(
            displayLarge = st(sans, 44, FontWeight.Bold, -1.0),
            displayMedium = st(sans, 38, FontWeight.Bold, -0.8),
            displaySmall = st(sans, 32, FontWeight.Bold, -0.6),
            headlineLarge = st(sans, 28, FontWeight.SemiBold, -0.4),
            headlineMedium = st(sans, 24, FontWeight.SemiBold, -0.3),
            headlineSmall = st(sans, 20, FontWeight.SemiBold, -0.2),
            titleLarge = st(sans, 20, FontWeight.SemiBold),
            titleMedium = st(sans, 16, FontWeight.SemiBold),
            titleSmall = st(sans, 15, FontWeight.SemiBold, 0.1),
            bodyLarge = st(sans, 16),
            bodyMedium = st(sans, 14),
            bodySmall = st(sans, 12),
            labelLarge = st(sans, 14, FontWeight.Medium),
            labelMedium = st(sans, 12, FontWeight.Medium),
            labelSmall = st(sans, 11, FontWeight.Medium, 0.4)
        )
    }
    val scheme = lightColorScheme(
        primary = Ink,
        onPrimary = Cream,
        background = Cream,
        onBackground = Ink,
        surface = Cream,
        onSurface = Ink,
        surfaceVariant = Sand,
        onSurfaceVariant = Muted,
        outline = Line
    )
    CompositionLocalProvider(LocalMono provides mono) {
        MaterialTheme(colorScheme = scheme, typography = typo) {
            Surface(Modifier.fillMaxSize(), color = Cream, contentColor = Ink) {
                content()
            }
        }
    }
}
