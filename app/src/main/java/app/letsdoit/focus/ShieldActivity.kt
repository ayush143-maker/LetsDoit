package app.letsdoit.focus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class ShieldActivity : ComponentActivity() {
    private val pkgState = mutableStateOf("")
    private val nonce = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(transparent),
            navigationBarStyle = SystemBarStyle.dark(transparent)
        )
        pkgState.value = intent.getStringExtra("pkg") ?: ""
        nonce.value = 1
        setContent {
            LdiTheme {
                ShieldOverlay(
                    pkg = pkgState.value,
                    nonce = nonce.value,
                    onClose = { finish() },
                    onGoHome = { goHome() },
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
        // Every new attempt to open a blocked app gets a fresh message.
        nonce.value = nonce.value + 1
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }
}

@Composable
fun ShieldOverlay(
    pkg: String,
    nonce: Int,
    onClose: () -> Unit,
    onGoHome: () -> Unit,
    onBackToFocus: () -> Unit
) {
    val ctx = LocalContext.current
    val active by Repo.active.collectAsState()
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(500L)
        }
    }
    val label = remember(pkg) { if (pkg.isEmpty()) "That app" else appLabel(ctx, pkg) }
    val task = active?.purpose?.trim() ?: ""
    val msg = remember(pkg, nonce) {
        val a0 = Repo.active.value
        val left = if (a0 != null) fmtClock(a0.remaining(System.currentTimeMillis())) else ""
        ShieldMessages.pick(label, task, left)
    }
    val quote = remember(pkg, nonce) { Quotes.random() }

    BackHandler(onBack = onGoHome)

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
                msg.title,
                color = Cream,
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(Modifier.height(16.dp))
            Text(
                msg.body,
                color = Cream,
                style = MaterialTheme.typography.titleMedium
            )
            if (task.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .border(1.dp, Muted, RBox)
                        .padding(14.dp)
                ) {
                    Eyebrow("You said you would work on", color = Muted)
                    Spacer(Modifier.height(4.dp))
                    Text(task, color = Cream, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                quote.text + if (quote.by.isNotEmpty()) "  - " + quote.by else "",
                color = Muted,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))
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
            Spacer(Modifier.height(28.dp))
            PButton("Back to focus", Modifier.fillMaxWidth(), ink = Cream, paper = Ink, onClick = onBackToFocus)
            Spacer(Modifier.height(10.dp))
            PButton("Pause focus and leave", Modifier.fillMaxWidth(), filled = false, ink = Cream, paper = Ink) {
                Focus.pause()
                onClose()
            }
        }
    }
}
