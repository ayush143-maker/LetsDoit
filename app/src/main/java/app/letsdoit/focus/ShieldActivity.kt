package app.letsdoit.focus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class ShieldActivity : ComponentActivity() {
    private val pkgState = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(transparent),
            navigationBarStyle = SystemBarStyle.dark(transparent)
        )
        pkgState.value = intent.getStringExtra("pkg") ?: ""
        setContent {
            LdiTheme {
                ShieldOverlay(
                    pkg = pkgState.value,
                    onClose = { finish() },
                    onBackToFocus = {
                        startActivity(
                            Intent(this, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        )
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pkgState.value = intent.getStringExtra("pkg") ?: ""
    }
}

@Composable
fun ShieldOverlay(pkg: String, onClose: () -> Unit, onBackToFocus: () -> Unit) {
    val ctx = LocalContext.current
    val active by Repo.active.collectAsState()
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(500L)
        }
    }
    val label = remember(pkg) { if (pkg.isEmpty()) "That app" else appLabel(ctx, pkg) }
    val quote = remember(pkg, active?.startedAt) { Quotes.random() }

    LaunchedEffect(active) {
        val a = active
        if (a == null || !a.running) onClose()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .systemBarsPadding()
            .padding(28.dp)
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Eyebrow("Focus shield", color = Muted)
            Spacer(Modifier.height(10.dp))
            Text(
                "$label can wait.",
                color = Cream,
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(Modifier.height(18.dp))
            Text(
                quote.text,
                color = Cream,
                style = MaterialTheme.typography.titleMedium
            )
            if (quote.by.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("— " + quote.by, color = Muted, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(28.dp))
            val a = active
            if (a != null) {
                Text(
                    fmtClock(a.remaining(now)),
                    color = Cream,
                    fontFamily = LocalMono.current,
                    fontSize = 44.sp
                )
                Text("left in this session", color = Muted, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(30.dp))
            PButton("Back to focus", Modifier.fillMaxWidth(), ink = Cream, paper = Ink, onClick = onBackToFocus)
            Spacer(Modifier.height(10.dp))
            PButton("Pause focus and leave", Modifier.fillMaxWidth(), filled = false, ink = Cream, paper = Ink) {
                Focus.pause()
                onClose()
            }
        }
    }
}
