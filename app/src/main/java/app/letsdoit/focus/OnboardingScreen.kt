package app.letsdoit.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(now: Long, onFinish: () -> Unit) {
    val ctx = LocalContext.current
    val pager = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val accOn = remember(now / 1000L) { accessibilityOn(ctx) }
    val page = pager.currentPage

    Column(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .systemBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Let's Do It!", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Text(
                "Skip",
                color = Muted,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RSmall)
                    .clickable { onFinish() }
                    .padding(8.dp)
            )
        }

        HorizontalPager(
            state = pager,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { p ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center
            ) {
                OnboardVisual(p)
                Spacer(Modifier.height(16.dp))
                when (p) {
                    0 -> {
                        PageHeader(
                            "Focus on one thing.",
                            "Set a purpose, start the clock and give it your full attention. Tasks, streaks and insights keep you honest."
                        )
                    }
                    1 -> {
                        PageHeader(
                            "Block what pulls you away.",
                            "Pick the apps that distract you. While a session runs, opening one is stopped by your focus shield. Change the list anytime under Shield, then Apps."
                        )
                    }
                    else -> {
                        PageHeader(
                            "Allow one permission.",
                            "To notice when a blocked app opens, Let's Do It needs Accessibility access. It never reads your screen, passwords or messages, and nothing leaves your device."
                        )
                        if (accOn) {
                            Text(
                                "Access granted. You're all set.",
                                color = Accent,
                                style = MaterialTheme.typography.titleMedium
                            )
                        } else {
                            PButton("Open accessibility settings", Modifier.fillMaxWidth()) {
                                openAccessibility(ctx)
                            }
                            Spacer(Modifier.height(10.dp))
                            PButton(
                                "Greyed out? Allow restricted settings",
                                Modifier.fillMaxWidth(),
                                filled = false
                            ) { openAppInfo(ctx) }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Choose Installed apps, then Let's Do It, and switch it on. If the switch is greyed out (Android 13 and above), open App info, tap the three-dot menu, choose Allow restricted settings, then try again.",
                                style = MaterialTheme.typography.labelMedium,
                                color = Muted
                            )
                        }
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(3) { i ->
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(if (i == page) 24.dp else 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (i == page) Accent else Line)
                )
            }
        }
        val label = if (page < 2) "Next" else if (accOn) "Get started" else "Continue without shield"
        PButton(label, Modifier.fillMaxWidth()) {
            if (page < 2) {
                scope.launch { pager.animateScrollToPage(page + 1) }
            } else {
                onFinish()
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun OnboardVisual(p: Int) {
    when (p) {
        0 -> LogoMark(88.dp)
        1 -> Box(
            Modifier
                .size(88.dp)
                .clip(RCard)
                .background(AccentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                NavIcons.Shield,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(44.dp)
            )
        }
        else -> Box(
            Modifier
                .size(88.dp)
                .clip(RCard)
                .background(AccentSoft),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .width(52.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Accent),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    Modifier
                        .padding(end = 3.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Sand)
                )
            }
        }
    }
}
