package app.letsdoit.focus

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val KNOWN_DISTRACTIONS = setOf(
    "com.instagram.android", "com.instagram.barcelona", "com.google.android.youtube",
    "com.facebook.katana", "com.facebook.lite", "com.twitter.android", "com.snapchat.android",
    "com.reddit.frontpage", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
    "com.netflix.mediaclient", "com.pinterest", "com.discord", "in.mohalla.sharechat",
    "com.amazon.avod.thirdpartyclient", "tv.twitch.android.app"
)

private fun openAccessibility(ctx: Context) {
    runCatching {
        ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun openAppInfo(ctx: Context) {
    runCatching {
        ctx.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + ctx.packageName))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

@Composable
fun ShieldSetupScreen(now: Long) {
    val ctx = LocalContext.current
    val prefs by Repo.prefs.collectAsState()
    val accOn = remember(now / 1000L) { accessibilityOn(ctx) }
    val apps by produceState<List<AppInfo>>(emptyList()) {
        value = withContext(Dispatchers.Default) { loadApps(ctx) }
    }
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Spacer(Modifier.height(18.dp))
                Eyebrow("Distraction control")
                Spacer(Modifier.height(6.dp))
                Text("Make room for what matters.", style = MaterialTheme.typography.displaySmall)
            }
        }

        item {
            LCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Eyebrow("Accessibility access", Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (accOn) Ink else Line)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            if (accOn) "Granted" else "Not granted",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (accOn) Cream else Ink
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (accOn)
                        "All set. During a focus session, apps from your list are sent back to your home screen."
                    else
                        "Needed once so Let's Do It can notice when a blocked app opens while you focus. It cannot read your screen, passwords or messages, and nothing leaves your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
                if (!accOn) {
                    Spacer(Modifier.height(14.dp))
                    PButton("Open accessibility settings", Modifier.fillMaxWidth()) { openAccessibility(ctx) }
                    Spacer(Modifier.height(10.dp))
                    PButton("Greyed out? Allow restricted settings", Modifier.fillMaxWidth(), filled = false) {
                        openAppInfo(ctx)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "1. Open accessibility settings, then Installed apps (or Downloaded apps) and choose Let's Do It.\n" +
                            "2. Turn it on and confirm.\n" +
                            "If the switch is greyed out (Android 13+ for apps installed from an APK): open App info, tap the ⋮ menu, choose Allow restricted settings, then try step 1 again.",
                        style = MaterialTheme.typography.labelMedium,
                        color = Muted
                    )
                }
            }
        }

        item {
            LCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Use focus shield", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Selected apps are redirected only when focus is running. This isn't a tamper-proof app lock.",
                            style = MaterialTheme.typography.labelMedium,
                            color = Muted
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    LdiSwitch(prefs.shieldOn) { on -> Repo.updatePrefs { it.copy(shieldOn = on) } }
                }
            }
        }

        item {
            LCard {
                Eyebrow("Focus preferences")
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Default duration", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    RoundBtn("−") { Repo.updatePrefs { it.copy(defaultMin = (it.defaultMin - 5).coerceAtLeast(5)) } }
                    Text("${prefs.defaultMin} min", Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.titleSmall)
                    RoundBtn("+") { Repo.updatePrefs { it.copy(defaultMin = (it.defaultMin + 5).coerceAtMost(180)) } }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily focus goal", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    RoundBtn("−") { Repo.updatePrefs { it.copy(goalMin = (it.goalMin - 15).coerceAtLeast(15)) } }
                    Text("${prefs.goalMin} min", Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.titleSmall)
                    RoundBtn("+") { Repo.updatePrefs { it.copy(goalMin = (it.goalMin + 15).coerceAtMost(720)) } }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Completion alerts", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    LdiSwitch(prefs.alerts) { on -> Repo.updatePrefs { it.copy(alerts = on) } }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Vibrate when done", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    LdiSwitch(prefs.vibrate) { on -> Repo.updatePrefs { it.copy(vibrate = on) } }
                }
            }
        }

        item {
            Column {
                Spacer(Modifier.height(6.dp))
                Eyebrow("Distracting apps · ${prefs.blocked.size} selected")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PButton("Auto-pick social apps", Modifier.weight(1f), filled = false) {
                        val found = apps.map { it.pkg }.filter { it in KNOWN_DISTRACTIONS }
                        Repo.updatePrefs { it.copy(blocked = it.blocked + found) }
                    }
                    PButton("Clear", Modifier.weight(0.5f), filled = false) {
                        Repo.updatePrefs { it.copy(blocked = emptySet()) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Field(query, { query = it }, "Search apps")
            }
        }

        if (apps.isEmpty()) {
            item {
                Text("Loading apps…", style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
        }

        items(filtered, key = { it.pkg }) { app ->
            val checked = app.pkg in prefs.blocked
            val icon = remember(app.pkg) {
                runCatching {
                    ctx.packageManager.getApplicationIcon(app.pkg).toBitmap(80, 80).asImageBitmap()
                }.getOrNull()
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (checked) Sand else Cream)
                    .clickable {
                        Repo.updatePrefs {
                            it.copy(blocked = if (checked) it.blocked - app.pkg else it.blocked + app.pkg)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(38.dp))
                } else {
                    Box(Modifier.size(38.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    app.label,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                CheckCircle(checked) {
                    Repo.updatePrefs {
                        it.copy(blocked = if (checked) it.blocked - app.pkg else it.blocked + app.pkg)
                    }
                }
            }
        }

        item {
            Column {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Cream canvas. Obsidian actions. No account, ads, tracking or network access. Your tasks and focus history stay on this device.",
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted
                )
                Spacer(Modifier.height(6.dp))
                Text("Let's Do It · 1.1.0", style = MaterialTheme.typography.labelMedium, color = Muted)
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun LdiSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Cream,
            checkedTrackColor = Ink,
            checkedBorderColor = Ink,
            uncheckedThumbColor = Muted,
            uncheckedTrackColor = Sand,
            uncheckedBorderColor = Line
        )
    )
}
