package app.letsdoit.focus

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

fun openAccessibility(ctx: Context) {
    runCatching {
        ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

fun openAppInfo(ctx: Context) {
    runCatching {
        ctx.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + ctx.packageName))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private fun setBlocked(pkg: String, on: Boolean) {
    Repo.updatePrefs { it.copy(blocked = if (on) it.blocked + pkg else it.blocked - pkg) }
}

@Composable
fun ShieldTab(now: Long) {
    val prefs by Repo.prefs.collectAsState()
    var seg by rememberSaveable { mutableStateOf(0) }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 22.dp)
    ) {
        PageHeader("Shield", "Block distractions while you focus.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Overview", seg == 0) { seg = 0 }
            Pill("Apps · ${prefs.blocked.size}", seg == 1) { seg = 1 }
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.weight(1f)) {
            if (seg == 0) {
                ShieldOverview(now) { seg = 1 }
            } else {
                AppsPanel(now) { seg = 0 }
            }
        }
    }
}

@Composable
fun ShieldOverview(now: Long, onOpenApps: () -> Unit) {
    val ctx = LocalContext.current
    val prefs by Repo.prefs.collectAsState()
    val accOn = remember(now / 1000L) { accessibilityOn(ctx) }
    val version = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: ""
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        LCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("Accessibility access", Modifier.weight(1f))
                Box(
                    Modifier
                        .clip(RSmall)
                        .background(if (accOn) Accent else Line)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        if (accOn) "Granted" else "Not granted",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (accOn) Sand else Ink
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (accOn)
                    "All set. During a focus session, apps from your list are stopped by the shield."
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
                    "1. Open accessibility settings, then Installed apps (or Downloaded apps) and choose Let's Do It.",
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted
                )
                Text(
                    "2. Turn it on and confirm.",
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted
                )
                Text(
                    "If the switch is greyed out (Android 13+, APK installed outside the Play Store): open App info, tap the three-dot menu, choose Allow restricted settings, then try step 1 again.",
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        LCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Use focus shield", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Selected apps are stopped only while focus is running. This isn't a tamper-proof app lock.",
                        style = MaterialTheme.typography.labelMedium,
                        color = Muted
                    )
                }
                Spacer(Modifier.width(12.dp))
                LdiSwitch(prefs.shieldOn) { on -> Repo.updatePrefs { it.copy(shieldOn = on) } }
            }
        }

        Spacer(Modifier.height(12.dp))
        LCard {
            Eyebrow("Distracting apps")
            Spacer(Modifier.height(8.dp))
            Text(
                if (prefs.blocked.isEmpty()) "No apps chosen yet."
                else "${prefs.blocked.size} apps are blocked during focus.",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(12.dp))
            PButton("Choose apps", Modifier.fillMaxWidth(), onClick = onOpenApps)
        }

        Spacer(Modifier.height(12.dp))
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

        Spacer(Modifier.height(18.dp))
        Text(
            "No account, ads, tracking or network access. Your tasks and focus history stay on this device.",
            style = MaterialTheme.typography.labelMedium,
            color = Muted
        )
        Spacer(Modifier.height(6.dp))
        Text("Let's Do It · $version", style = MaterialTheme.typography.labelMedium, color = Muted)
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
fun AppsPanel(now: Long, onOpenOverview: () -> Unit) {
    val ctx = LocalContext.current
    val prefs by Repo.prefs.collectAsState()
    val accOn = remember(now / 1000L) { accessibilityOn(ctx) }
    val apps by produceState<List<AppInfo>>(emptyList()) {
        value = withContext(Dispatchers.Default) { loadApps(ctx) }
    }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(0) }

    val filtered = remember(apps, query, filter, prefs.blocked) {
        apps.filter { a ->
            val matches = query.isBlank() || a.label.contains(query, ignoreCase = true)
            val isBlocked = a.pkg in prefs.blocked
            matches && (filter == 0 || (filter == 1 && isBlocked) || (filter == 2 && !isBlocked))
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RCard)
                .background(if (prefs.shieldOn) Ink else Sand)
                .border(1.dp, if (prefs.shieldOn) Ink else Line, RCard)
                .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Focus shield",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (prefs.shieldOn) Sand else Ink
                )
                Text(
                    if (prefs.shieldOn) "${prefs.blocked.size} apps blocked during focus"
                    else "Off · nothing is blocked",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (prefs.shieldOn) Sand.copy(alpha = 0.7f) else Muted
                )
            }
            LdiSwitch(prefs.shieldOn, dark = prefs.shieldOn) { on ->
                Repo.updatePrefs { it.copy(shieldOn = on) }
            }
        }

        if (!accOn) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Accessibility access is needed for the shield to work · tap to set up",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RBox)
                    .background(AccentSoft)
                    .clickable { onOpenOverview() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelMedium,
                color = Accent
            )
        }

        Spacer(Modifier.height(12.dp))
        Field(query, { query = it }, "Search apps")
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Pill("All · ${apps.size}", filter == 0) { filter = 0 }
            Pill("Blocked · ${prefs.blocked.size}", filter == 1) { filter = 1 }
            Pill("Allowed", filter == 2) { filter = 2 }
        }
        Row(
            Modifier.padding(top = 6.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "Auto-pick social apps",
                modifier = Modifier
                    .clickable {
                        val found = apps.map { it.pkg }.filter { it in KNOWN_DISTRACTIONS }
                        Repo.updatePrefs { it.copy(blocked = it.blocked + found) }
                    }
                    .padding(vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Accent
            )
            Text(
                "Clear all",
                modifier = Modifier
                    .clickable { Repo.updatePrefs { it.copy(blocked = emptySet()) } }
                    .padding(vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Muted
            )
        }

        if (apps.isEmpty()) {
            Text("Loading apps…", style = MaterialTheme.typography.bodyMedium, color = Muted)
        } else if (filtered.isEmpty()) {
            Text("No apps match.", style = MaterialTheme.typography.bodyMedium, color = Muted)
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.pkg }) { app ->
                AppRow(app, app.pkg in prefs.blocked)
            }
        }
    }
}

@Composable
private fun AppRow(app: AppInfo, checked: Boolean) {
    val ctx = LocalContext.current
    val icon = remember(app.pkg) {
        runCatching {
            ctx.packageManager.getApplicationIcon(app.pkg).toBitmap(96, 96).asImageBitmap()
        }.getOrNull()
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RBox)
            .background(Sand)
            .border(1.dp, if (checked) Accent else Line, RBox)
            .clickable { setBlocked(app.pkg, !checked) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RSmall)
            )
        } else {
            Box(Modifier.size(42.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                app.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (checked) {
                Text(
                    "Blocked during focus",
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent
                )
            }
        }
        LdiSwitch(checked) { on -> setBlocked(app.pkg, on) }
    }
}

@Composable
fun LdiSwitch(checked: Boolean, dark: Boolean = false, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = if (dark) {
            SwitchDefaults.colors(
                checkedThumbColor = Ink,
                checkedTrackColor = Sand,
                checkedBorderColor = Sand,
                uncheckedThumbColor = Sand,
                uncheckedTrackColor = Muted,
                uncheckedBorderColor = Muted
            )
        } else {
            SwitchDefaults.colors(
                checkedThumbColor = Sand,
                checkedTrackColor = Accent,
                checkedBorderColor = Accent,
                uncheckedThumbColor = Muted,
                uncheckedTrackColor = Line,
                uncheckedBorderColor = Line
            )
        }
    )
}
