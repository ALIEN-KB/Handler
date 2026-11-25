package com.alienkb.handler.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alienkb.handler.core.A11y
import com.alienkb.handler.core.Perms
import com.alienkb.handler.service.GuardService
import com.alienkb.handler.ui.components.HCard
import com.alienkb.handler.ui.components.SectionLabel
import com.alienkb.handler.ui.components.TerminalWindow
import com.alienkb.handler.ui.theme.HC
import com.alienkb.handler.ui.theme.Mono
import kotlinx.coroutines.delay

private data class SetupState(
    val secure: Boolean,
    val usage: Boolean,
    val notifications: Boolean,
    val battery: Boolean,
)

@Composable
fun SetupScreen() {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var bump by remember { mutableIntStateOf(0) }
    val s by produceState(
        SetupState(A11y.canWrite(ctx), Perms.usageAccess(ctx), Perms.notifications(ctx), Perms.batteryUnrestricted(ctx)),
        bump,
    ) {
        while (true) {
            value = SetupState(A11y.canWrite(ctx), Perms.usageAccess(ctx), Perms.notifications(ctx), Perms.batteryUnrestricted(ctx))
            delay(1_500)
        }
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        bump++
        GuardService.updateNotification(ctx)
    }
    var showXiaomiHelp by remember { mutableStateOf(Perms.isXiaomi) }

    val done = listOf(s.secure, s.usage, s.notifications, s.battery).count { it }
    val total = 4
    val grant = Perms.grantCommand(ctx)
    val usageCmd = Perms.usageCommand(ctx)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("Setup", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = HC.TextHi)
                Text("$done of $total ready", fontSize = 13.sp, color = HC.TextLo)
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { done / total.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = if (done == total) HC.Mint else HC.Amber,
                    trackColor = HC.Surface3,
                )
            }
        }

        // 1. The ADB permission
        item {
            CheckCard(
                step = "01",
                title = "Secure settings permission",
                ok = s.secure,
                body = if (s.secure) "Granted. Handler can switch accessibility services on and off."
                else "The one permission that makes everything work. Grant it once from a computer with ADB; it survives reboots.",
            ) {
                if (!s.secure) {
                    TerminalWindow(accent = HC.Violet, title = "terminal") {
                        Text("> $grant", fontFamily = Mono, fontSize = 12.sp, color = HC.Violet, lineHeight = 17.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("# optional: also grants usage access", fontFamily = Mono, fontSize = 11.sp, color = HC.TextLo)
                        Text("> $usageCmd", fontFamily = Mono, fontSize = 12.sp, color = HC.Violet.copy(alpha = 0.8f), lineHeight = 17.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton("Copy command", HC.Violet, Modifier.weight(1f)) {
                            clipboard.setText(AnnotatedString("$grant\n$usageCmd"))
                            Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
                        }
                        ActionButton("Developer options", HC.Surface3, Modifier.weight(1f), HC.TextHi) {
                            Perms.openDeveloperOptions(ctx)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "How:\n" +
                            "1. Settings > About phone > tap OS version 7 times\n" +
                            "2. Developer options > turn on USB debugging\n" +
                            (if (Perms.isXiaomi) "3. Also turn on USB debugging (Security settings) (needs Mi account + SIM)\n" else "") +
                            "${if (Perms.isXiaomi) 4 else 3}. Plug into a computer, run the command above\n" +
                            "No computer? Use Shizuku or LADB with Wireless debugging.\n" +
                            "You can turn USB debugging off again afterwards.",
                        fontSize = 13.sp, lineHeight = 20.sp, color = HC.TextLo,
                    )
                }
            }
        }

        item {
            CheckCard(
                step = "02",
                title = "Usage access",
                ok = s.usage,
                body = "Lets Handler see which app is on screen, so it knows when you open or leave a protected app.",
            ) {
                if (!s.usage) ActionButton("Open usage access", HC.Mint, Modifier.fillMaxWidth()) { Perms.openUsageAccess(ctx) }
            }
        }

        item {
            CheckCard(
                step = "03",
                title = "Notification",
                ok = s.notifications,
                body = "A quiet, permanent notification keeps the watchdog alive and gives you a one-tap pause/resume.",
            ) {
                if (!s.notifications && Build.VERSION.SDK_INT >= 33) {
                    ActionButton("Allow notifications", HC.Mint, Modifier.fillMaxWidth()) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }

        item {
            CheckCard(
                step = "04",
                title = "Battery: no restrictions",
                ok = s.battery,
                body = "Stops Android from putting Handler to sleep.",
            ) {
                if (!s.battery) ActionButton("Remove restriction", HC.Mint, Modifier.fillMaxWidth()) { Perms.requestBattery(ctx) }
            }
        }

        item { SectionLabel(if (Perms.isXiaomi) "xiaomi / hyperos survival kit" else "extra protection") }
        item {
            HCard(borderColor = HC.Amber.copy(alpha = 0.35f)) {
                Row(
                    Modifier.fillMaxWidth().clickable { showXiaomiHelp = !showXiaomiHelp },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Info, null, tint = HC.Amber)
                    Spacer(Modifier.width(10.dp))
                    Text("Stop the system from killing Handler", color = HC.TextHi, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(if (showXiaomiHelp) "-" else "+", fontFamily = Mono, color = HC.Amber, fontSize = 18.sp)
                }
                if (showXiaomiHelp) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "* Autostart: ON for Handler (and for Curbox)\n" +
                            "* Battery saver: No restrictions (both apps)\n" +
                            "* Recents: long-press Handler > tap the lock icon\n" +
                            "* Other permissions: allow Home screen shortcuts\n" +
                            "* Do the same for Curbox, so it is killed less often too",
                        fontSize = 13.sp, lineHeight = 21.sp, color = HC.TextLo,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton("Autostart", HC.Amber, Modifier.weight(1f)) { Perms.openAutostart(ctx) }
                        ActionButton("App info", HC.Surface3, Modifier.weight(1f), HC.TextHi) { Perms.openAppDetails(ctx) }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun CheckCard(
    step: String,
    title: String,
    ok: Boolean,
    body: String,
    extra: @Composable () -> Unit,
) {
    val color = if (ok) HC.Mint else HC.Rose
    HCard(borderColor = color.copy(alpha = if (ok) 0.3f else 0.45f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(step, fontFamily = Mono, fontSize = 12.sp, color = HC.TextLo)
            Spacer(Modifier.width(10.dp))
            Text(title, color = HC.TextHi, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Icon(if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline, null, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(body, fontSize = 13.sp, lineHeight = 19.sp, color = HC.TextLo)
        if (!ok) Spacer(Modifier.height(12.dp))
        extra()
    }
}

@Composable
fun ActionButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    textColor: Color = HC.Bg,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = textColor),
    ) {
        if (text == "Copy command") {
            Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}
