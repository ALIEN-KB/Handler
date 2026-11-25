package com.alienkb.handler.ui.screens

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alienkb.handler.core.A11y
import com.alienkb.handler.core.Engine
import com.alienkb.handler.core.Perms
import com.alienkb.handler.core.Prefs
import com.alienkb.handler.service.GuardService
import com.alienkb.handler.ui.components.HCard
import com.alienkb.handler.ui.components.PulseDot
import com.alienkb.handler.ui.components.SectionLabel
import com.alienkb.handler.ui.components.Tag
import com.alienkb.handler.ui.components.ToggleRow
import com.alienkb.handler.ui.theme.HC
import com.alienkb.handler.ui.theme.Mono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class Status(
    val canWrite: Boolean,
    val usage: Boolean,
    val targets: Set<String>,
    val targetNames: String,
    val enabledTargets: Int,
    val pausedBy: String?,
    val pausedLabel: String,
    val guardOn: Boolean,
    val guardRunning: Boolean,
    val protectedCount: Int,
    val restores: Int,
    val pauses: Int,
    val logs: List<Pair<Long, String>>,
)

private fun readStatus(ctx: Context): Status {
    val p = Prefs(ctx)
    val targets = p.targets
    return Status(
        canWrite = A11y.canWrite(ctx),
        usage = Perms.usageAccess(ctx),
        targets = targets,
        targetNames = Engine.targetNames(ctx, targets),
        enabledTargets = A11y.enabledCount(ctx, targets),
        pausedBy = p.suspendedBy,
        pausedLabel = Engine.label(ctx, p.suspendedBy),
        guardOn = p.guardEnabled,
        guardRunning = GuardService.running,
        protectedCount = p.protectedApps.size,
        restores = p.restoreCount,
        pauses = p.pauseCount,
        logs = p.logs().take(25),
    )
}

private data class Look(val color: Color, val badge: String, val title: String, val body: String)

private fun look(s: Status): Look = when {
    !s.canWrite -> Look(HC.Rose, "SETUP", "One step left", "Grant the one-time ADB permission so Handler can switch accessibility for you.")
    s.targets.isEmpty() -> Look(HC.Amber, "PICK", "Choose a service", "Tell Handler which accessibility service to manage, e.g. Curbox.")
    s.pausedBy == Engine.MANUAL -> Look(HC.Violet, "PAUSED", "Paused by you", "Comes back ON when you lock your phone, or tap resume.")
    s.pausedBy != null -> Look(HC.Violet, "PAUSED", "Paused for ${s.pausedLabel}", "Comes back ON when you leave it or lock your phone.")
    s.enabledTargets < s.targets.size -> Look(
        HC.Amber, "OFF", "${s.targetNames} is off",
        if (s.guardOn) "The watchdog will switch it back on in a moment." else "Turn on the watchdog, or tap resume.",
    )
    else -> Look(HC.Mint, "ON", "All good", "${s.targetNames} is running · ${s.protectedCount} protected apps.")
}

@Composable
fun HomeScreen(onOpenSetup: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    var showPicker by remember { mutableStateOf(false) }
    val status by produceState(initialValue = readStatus(ctx), refresh) {
        while (true) {
            value = withContext(Dispatchers.Default) { readStatus(ctx) }
            delay(1_000)
        }
    }
    val l = look(status)
    val color by animateColorAsState(l.color, label = "status")

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Handler", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = HC.TextHi)
                    Text("accessibility autopilot", fontFamily = Mono, fontSize = 12.sp, color = HC.TextLo)
                }
                Tag(if (status.guardRunning) "guard: online" else "guard: offline", if (status.guardRunning) HC.Mint else HC.TextLo)
            }
        }

        // Hero status card
        item {
            val shape = RoundedCornerShape(26.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.16f), HC.Surface1)))
                    .border(1.dp, Brush.linearGradient(listOf(color.copy(alpha = 0.8f), HC.Stroke)), shape)
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulseDot(color)
                    Spacer(Modifier.width(6.dp))
                    Text("STATUS", fontFamily = Mono, fontSize = 11.sp, color = HC.TextLo, letterSpacing = 2.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "[ ${l.badge} ]",
                    style = TextStyle(
                        fontFamily = Mono,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        shadow = Shadow(color.copy(alpha = 0.6f), blurRadius = 24f),
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Text(l.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = HC.TextHi)
                Spacer(Modifier.height(4.dp))
                Text(l.body, fontSize = 14.sp, lineHeight = 20.sp, color = HC.TextLo)
                Spacer(Modifier.height(16.dp))

                when {
                    !status.canWrite -> Button(
                        onClick = onOpenSetup,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = HC.Bg),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("Open setup", fontWeight = FontWeight.Bold) }

                    status.targets.isEmpty() -> Button(
                        onClick = { showPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = HC.Bg),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("Choose service", fontWeight = FontWeight.Bold) }

                    else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val paused = status.pausedBy != null
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.Default) {
                                    if (paused) Engine.resume(ctx, "from app", manual = true)
                                    else Engine.pause(ctx, Engine.MANUAL, "from app")
                                }
                                refresh++
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = HC.Bg),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (paused) "Resume" else "Pause now", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch(Dispatchers.Default) { Engine.ensureHealthy(ctx, "manual check") }
                                refresh++
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Rounded.Refresh, null, tint = HC.TextHi)
                            Spacer(Modifier.width(6.dp))
                            Text("Check now", color = HC.TextHi)
                        }
                    }
                }
            }
        }

        // Stats
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("protected", status.protectedCount.toString(), HC.Sky, Modifier.weight(1f))
                Stat("pauses", status.pauses.toString(), HC.Violet, Modifier.weight(1f))
                Stat("rescues", status.restores.toString(), HC.Amber, Modifier.weight(1f))
            }
        }

        item { SectionLabel("watchdog") }
        item {
            HCard {
                ToggleRow(
                    title = "Keep it alive",
                    subtitle = "Instantly switches your service back on if the system turns it off.",
                    checked = status.guardOn,
                    onChange = { on ->
                        Prefs(ctx).guardEnabled = on
                        if (on) GuardService.start(ctx) else if (status.pausedBy == null) GuardService.stop(ctx)
                        refresh++
                    },
                )
            }
        }

        item { SectionLabel("managed service") }
        item {
            HCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (status.targets.isEmpty()) "Nothing selected" else status.targetNames,
                            fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = HC.TextHi,
                        )
                        Text(
                            if (status.targets.isEmpty()) "Pick the accessibility service to manage"
                            else "${status.enabledTargets}/${status.targets.size} switched on",
                            fontSize = 13.sp, color = HC.TextLo,
                        )
                    }
                    TextButton(onClick = { showPicker = true }) { Text("Change", color = HC.Mint) }
                }
            }
        }

        item { SectionLabel("activity") }
        item {
            HCard {
                if (status.logs.isEmpty()) {
                    Text("> nothing yet. quiet is good._", fontFamily = Mono, fontSize = 12.sp, color = HC.TextLo)
                } else {
                    val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                    status.logs.forEach { (ts, msg) ->
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Text(fmt.format(Date(ts)), fontFamily = Mono, fontSize = 12.sp, color = HC.Mint.copy(alpha = 0.8f))
                            Spacer(Modifier.width(10.dp))
                            Text(msg, fontFamily = Mono, fontSize = 12.sp, color = HC.TextLo, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }

    if (showPicker) {
        ServicePickerDialog(
            initial = status.targets,
            onDismiss = { showPicker = false },
            onSave = { sel ->
                Prefs(ctx).targets = sel
                showPicker = false
                scope.launch(Dispatchers.Default) { Engine.ensureHealthy(ctx, "service changed") }
                GuardService.updateNotification(ctx)
                refresh++
            },
        )
    }
}

@Composable
private fun Stat(label: String, value: String, color: Color, modifier: Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .clip(shape)
            .background(HC.Surface1)
            .border(1.dp, HC.Stroke, shape)
            .padding(14.dp),
    ) {
        Text(value, fontFamily = Mono, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 12.sp, color = HC.TextLo)
    }
}

@Composable
fun ServicePickerDialog(initial: Set<String>, onDismiss: () -> Unit, onSave: (Set<String>) -> Unit) {
    val ctx = LocalContext.current
    val services = remember { A11y.installed(ctx) }
    var selected by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HC.Surface2,
        title = { Text("Managed service", color = HC.TextHi) },
        text = {
            if (services.isEmpty()) {
                Text("No accessibility services are installed.", color = HC.TextLo)
            } else {
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(services, key = { it.component }) { s ->
                        val checked = s.component in selected
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selected = if (checked) selected - s.component else selected + s.component }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = HC.Mint, checkmarkColor = HC.Bg),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(s.label, color = HC.TextHi, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(s.appLabel, color = HC.TextLo, fontSize = 12.sp, fontFamily = Mono)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(selected) }) { Text("Save", color = HC.Mint) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = HC.TextLo) } },
    )
}
