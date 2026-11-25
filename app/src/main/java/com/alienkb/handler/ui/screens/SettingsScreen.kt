package com.alienkb.handler.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alienkb.handler.core.Perms
import com.alienkb.handler.core.Prefs
import com.alienkb.handler.ui.components.AsciiArt
import com.alienkb.handler.ui.components.HCard
import com.alienkb.handler.ui.components.SectionLabel
import com.alienkb.handler.ui.components.ToggleRow
import com.alienkb.handler.ui.theme.HC
import com.alienkb.handler.ui.theme.Mono
import kotlin.math.roundToInt

private fun footer(version: String): String {
    fun row(text: String) = "|" + " $text".padEnd(24).take(24) + "|"
    return listOf(
        ".------------------------.",
        row("handler v$version"),
        row("no internet permission"),
        row("nothing leaves phone"),
        "'------------------------'",
    ).joinToString("\n")
}

@Composable
fun SettingsScreen(onReplayIntro: () -> Unit) {
    val ctx = LocalContext.current
    val p = remember { Prefs(ctx) }
    var autoDetect by remember { mutableStateOf(p.autoDetect) }
    var screenOff by remember { mutableStateOf(p.resumeOnScreenOff) }
    var grace by remember { mutableIntStateOf(p.graceSeconds) }
    var hideDev by remember { mutableStateOf(p.hideDevOptions) }
    var toasts by remember { mutableStateOf(p.showToasts) }
    val version = remember {
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
        } catch (_: Exception) {
            "?"
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = HC.TextHi) }

        item { SectionLabel("behaviour") }
        item {
            HCard {
                ToggleRow(
                    "Auto-detect protected apps",
                    "Pause automatically even when you open the app normally (not from its lock shortcut).",
                    autoDetect,
                    { autoDetect = it; p.autoDetect = it },
                )
                Divider()
                ToggleRow(
                    "Back ON when phone is locked",
                    "15 seconds after the screen turns off, accessibility comes back. You'll never forget it off.",
                    screenOff,
                    { screenOff = it; p.resumeOnScreenOff = it },
                )
                Divider()
                Column(Modifier.fillMaxWidth()) {
                    Row {
                        Text("Leave delay", color = HC.TextHi, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Text("${grace}s", fontFamily = Mono, color = HC.Mint, fontSize = 15.sp)
                    }
                    Text(
                        "How long you can be away from a protected app (e.g. copying an OTP) before accessibility comes back.",
                        color = HC.TextLo, fontSize = 13.sp, lineHeight = 18.sp,
                    )
                    Slider(
                        value = grace.toFloat(),
                        onValueChange = { grace = it.roundToInt() },
                        onValueChangeFinished = { p.graceSeconds = grace },
                        valueRange = 3f..60f,
                        colors = SliderDefaults.colors(
                            thumbColor = HC.Mint,
                            activeTrackColor = HC.Mint,
                            inactiveTrackColor = HC.Surface3,
                        ),
                    )
                }
                Divider()
                ToggleRow(
                    "Pop-up messages",
                    "Show a short toast when accessibility is paused or restored.",
                    toasts,
                    { toasts = it; p.showToasts = it },
                )
            }
        }

        item { SectionLabel("experimental") }
        item {
            HCard(borderColor = HC.Amber.copy(alpha = 0.3f)) {
                ToggleRow(
                    "Also hide Developer options",
                    "Some banks also refuse to run when Developer options / USB debugging are on. " +
                        "Handler turns them off while a protected app is open and restores them after.",
                    hideDev,
                    { hideDev = it; p.hideDevOptions = it },
                )
            }
        }

        item { SectionLabel("handler") }
        item {
            HCard {
                LinkRow("Replay the intro") { onReplayIntro() }
                Divider()
                LinkRow("Accessibility settings") { Perms.openAccessibility(ctx) }
                Divider()
                LinkRow("Clear activity log") { p.clearLog() }
            }
        }
        item {
            AsciiArt(
                footer(version),
                HC.Mint.copy(alpha = 0.7f),
                Modifier.fillMaxWidth(),
                maxSp = 12f,
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = HC.Stroke)
}

@Composable
private fun LinkRow(text: String, onClick: () -> Unit) {
    Text(
        "> $text",
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        fontFamily = Mono,
        fontSize = 14.sp,
        color = HC.TextHi,
    )
}
