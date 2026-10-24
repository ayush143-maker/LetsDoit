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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    var tab by rememberSaveable { mutableStateOf(0) }
    var preset by remember { mutableStateOf<String?>(null) }
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(500L)
        }
    }

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
                else -> ShieldSetupScreen(now)
            }
        }
        BottomBar(tab) { tab = it }
    }
}

@Composable
fun BottomBar(tab: Int, onTab: (Int) -> Unit) {
    val labels = listOf("Focus", "Tasks", "Insights", "Shield")
    Column(Modifier.background(Cream)) {
        HorizontalDivider(color = Line)
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            labels.forEachIndexed { i, label ->
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { onTab(i) }
                        .padding(top = 12.dp, bottom = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        label,
                        style = if (tab == i) MaterialTheme.typography.titleSmall
                        else MaterialTheme.typography.labelLarge,
                        color = if (tab == i) Ink else Muted
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (tab == i) Ink else Color.Transparent)
                    )
                }
            }
        }
    }
}
