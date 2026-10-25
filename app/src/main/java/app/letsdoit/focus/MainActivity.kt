package app.letsdoit.focus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UiState.init(this)
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(transparent, transparent),
            navigationBarStyle = SystemBarStyle.light(transparent, transparent)
        )
        if (Repo.active.value != null) {
            // Process may have been killed mid-session: make sure the timer service is alive.
            ContextCompat.startForegroundService(this, Intent(this, FocusService::class.java))
        }
        setContent {
            LdiTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val activity = LocalContext.current as ComponentActivity
    var tab by rememberSaveable { mutableStateOf(0) }
    var preset by remember { mutableStateOf<String?>(null) }
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(500L)
        }
    }
    val onboarded by UiState.onboarded.collectAsState()
    val dismissed by UiState.dismissedId.collectAsState()
    val active by Repo.active.collectAsState()
    val last by Repo.lastResult.collectAsState()

    val a = active
    val l = last
    val celebrate = l != null && l.completed && l.id != dismissed && a == null
    val dark = onboarded && (a != null || celebrate)

    // Dark system bars while the immersive deep-focus / completion screens are showing.
    LaunchedEffect(dark) {
        val t = android.graphics.Color.TRANSPARENT
        val style = if (dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t)
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    if (!onboarded) {
        OnboardingScreen(now) { UiState.finishOnboarding() }
    } else if (a != null) {
        DeepFocusScreen(a, now)
    } else if (l != null && celebrate) {
        CompletionScreen(l) { UiState.dismissedId.value = l.id }
    } else {
        Column(
            Modifier
                .fillMaxSize()
                .background(Cream)
                .imePadding()
        ) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> FocusScreen(now, preset, { preset = null }, { tab = 3 })
                    1 -> TasksScreen { id ->
                        preset = id
                        tab = 0
                    }
                    2 -> InsightsScreen(now)
                    else -> ShieldTab(now)
                }
            }
            BottomBar(tab) { tab = it }
        }
    }
}

@Composable
fun BottomBar(tab: Int, onTab: (Int) -> Unit) {
    val labels = listOf("Focus", "Tasks", "Insights", "Shield")
    val icons = listOf(NavIcons.Focus, NavIcons.Tasks, NavIcons.Insights, NavIcons.Shield)
    Column(Modifier.background(Sand)) {
        HorizontalDivider(color = Line)
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            labels.forEachIndexed { i, label ->
                val selected = tab == i
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onTab(i) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .width(56.dp)
                            .height(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) AccentSoft else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icons[i],
                            contentDescription = label,
                            tint = if (selected) Accent else Muted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) Ink else Muted
                    )
                }
            }
        }
    }
}
