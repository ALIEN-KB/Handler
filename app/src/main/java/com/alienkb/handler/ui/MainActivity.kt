package com.alienkb.handler.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.alienkb.handler.core.Engine
import com.alienkb.handler.core.Prefs
import com.alienkb.handler.core.Shortcuts
import com.alienkb.handler.service.GuardService
import com.alienkb.handler.ui.onboarding.OnboardingScreen
import com.alienkb.handler.ui.screens.AppsScreen
import com.alienkb.handler.ui.screens.HomeScreen
import com.alienkb.handler.ui.screens.SettingsScreen
import com.alienkb.handler.ui.screens.SetupScreen
import com.alienkb.handler.ui.theme.HC
import com.alienkb.handler.ui.theme.HandlerTheme
import kotlin.concurrent.thread

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Rounded.Home),
    Apps("Apps", Icons.Rounded.Apps),
    Setup("Setup", Icons.Rounded.Checklist),
    Settings("Settings", Icons.Rounded.Tune),
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = Prefs(this)
        val app = applicationContext
        thread {
            Engine.autoPickTarget(app)
            Shortcuts.syncDynamic(app)
        }

        setContent {
            HandlerTheme {
                var onboarding by remember { mutableStateOf(!prefs.onboardingDone) }
                Surface(Modifier.fillMaxSize(), color = HC.Bg) {
                    AnimatedContent(
                        targetState = onboarding,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "root",
                    ) { showIntro ->
                        if (showIntro) {
                            OnboardingScreen(onDone = {
                                val first = !prefs.onboardingDone
                                prefs.onboardingDone = true
                                onboarding = false
                                if (first && prefs.guardEnabled) GuardService.start(this@MainActivity)
                            })
                        } else {
                            MainScaffold(
                                startOnSetup = !com.alienkb.handler.core.A11y.canWrite(this@MainActivity),
                                onReplayIntro = { onboarding = true },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val p = Prefs(this)
        if (p.onboardingDone && p.guardEnabled) GuardService.start(this)
    }
}

@Composable
private fun MainScaffold(startOnSetup: Boolean, onReplayIntro: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(if (startOnSetup) Tab.Setup else Tab.Home) }
    Scaffold(
        containerColor = HC.Bg,
        bottomBar = {
            NavigationBar(containerColor = HC.Surface1, tonalElevation = 0.dp) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = HC.Bg,
                            selectedTextColor = HC.Mint,
                            indicatorColor = HC.Mint,
                            unselectedIconColor = HC.TextLo,
                            unselectedTextColor = HC.TextLo,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Crossfade(targetState = tab, label = "tab") { current ->
                when (current) {
                    Tab.Home -> HomeScreen(onOpenSetup = { tab = Tab.Setup })
                    Tab.Apps -> AppsScreen()
                    Tab.Setup -> SetupScreen()
                    Tab.Settings -> SettingsScreen(onReplayIntro = onReplayIntro)
                }
            }
        }
    }
}
