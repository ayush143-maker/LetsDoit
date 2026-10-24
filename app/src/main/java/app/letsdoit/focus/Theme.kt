package app.letsdoit.focus

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Palette: cool neutral canvas, white cards with hairline borders, one blue accent.
// (Old names are kept so every screen picks the new look up automatically.)
val Cream = Color(0xFFF4F5F7)      // app background
val Sand = Color(0xFFFFFFFF)       // card / surface
val Line = Color(0xFFE2E5EA)       // hairline borders, tracks
val Ink = Color(0xFF0E1116)        // text + primary actions
val Muted = Color(0xFF667085)      // secondary text
val Accent = Color(0xFF2F5BFF)     // progress, active states
val AccentSoft = Color(0xFFE6ECFF) // active nav background

// Boxy shape scale
val RCard = RoundedCornerShape(12.dp)
val RBox = RoundedCornerShape(10.dp)
val RSmall = RoundedCornerShape(8.dp)

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
        lineHeight = (size * 1.3).sp,
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
            displayLarge = st(sans, 40, FontWeight.Bold, -1.2),
            displayMedium = st(sans, 34, FontWeight.Bold, -1.0),
            displaySmall = st(sans, 30, FontWeight.Bold, -0.8),
            headlineLarge = st(sans, 26, FontWeight.SemiBold, -0.6),
            headlineMedium = st(sans, 22, FontWeight.SemiBold, -0.4),
            headlineSmall = st(sans, 18, FontWeight.SemiBold, -0.3),
            titleLarge = st(sans, 18, FontWeight.SemiBold, -0.2),
            titleMedium = st(sans, 15, FontWeight.SemiBold, -0.1),
            titleSmall = st(sans, 14, FontWeight.SemiBold),
            bodyLarge = st(sans, 15, FontWeight.Normal, -0.1),
            bodyMedium = st(sans, 14),
            bodySmall = st(sans, 12),
            labelLarge = st(sans, 13, FontWeight.Medium),
            labelMedium = st(sans, 12, FontWeight.Medium),
            labelSmall = st(sans, 11, FontWeight.Medium, 0.5)
        )
    }
    val scheme = lightColorScheme(
        primary = Ink,
        onPrimary = Sand,
        secondary = Accent,
        primaryContainer = AccentSoft,
        background = Cream,
        onBackground = Ink,
        surface = Sand,
        onSurface = Ink,
        surfaceVariant = Sand,
        onSurfaceVariant = Muted,
        outline = Line
    )
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RSmall,
        medium = RBox,
        large = RCard,
        extraLarge = RCard
    )
    CompositionLocalProvider(LocalMono provides mono) {
        MaterialTheme(colorScheme = scheme, typography = typo, shapes = shapes) {
            Surface(Modifier.fillMaxSize(), color = Cream, contentColor = Ink) {
                content()
            }
        }
    }
}
