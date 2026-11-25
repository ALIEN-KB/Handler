package com.alienkb.handler.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.alienkb.handler.core.Perms
import com.alienkb.handler.core.Prefs
import com.alienkb.handler.core.Shortcuts
import com.alienkb.handler.core.Suggest
import com.alienkb.handler.ui.components.HCard
import com.alienkb.handler.ui.components.SectionLabel
import com.alienkb.handler.ui.components.Tag
import com.alienkb.handler.ui.theme.HC
import com.alienkb.handler.ui.theme.Mono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

private class AppEntry(val pkg: String, val label: String, val icon: ImageBitmap?, val suggested: Boolean)

@Suppress("DEPRECATION")
private fun loadApps(ctx: Context): List<AppEntry> {
    val pm = ctx.packageManager
    val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(launcher, 0)
        .map { it.activityInfo.packageName }
        .distinct()
        .filter { it != ctx.packageName }
        .mapNotNull { pkg ->
            try {
                val ai = pm.getApplicationInfo(pkg, 0)
                val label = pm.getApplicationLabel(ai).toString()
                val icon = try {
                    pm.getApplicationIcon(ai).toBitmap(96, 96, Bitmap.Config.ARGB_8888).asImageBitmap()
                } catch (_: Exception) {
                    null
                }
                AppEntry(pkg, label, icon, Suggest.isFinance(pkg, label))
            } catch (_: Exception) {
                null
            }
        }
}

@Composable
fun AppsScreen() {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    var apps by remember { mutableStateOf<List<AppEntry>?>(null) }
    var protectedApps by remember { mutableStateOf(prefs.protectedApps) }
    var query by rememberSaveable { mutableStateOf("") }
    // Sort once (protected first) so rows don't jump around while you toggle.
    val sortSnapshot = remember(apps) { prefs.protectedApps }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { loadApps(ctx) }
    }

    val shown = remember(apps, query, sortSnapshot) {
        val q = query.trim().lowercase()
        apps.orEmpty()
            .filter { q.isEmpty() || it.label.lowercase().contains(q) || it.pkg.contains(q) }
            .sortedWith(
                compareByDescending<AppEntry> { it.pkg in sortSnapshot }
                    .thenByDescending { it.suggested }
                    .thenBy { it.label.lowercase() },
            )
    }

    fun toggle(pkg: String) {
        protectedApps = if (pkg in protectedApps) protectedApps - pkg else protectedApps + pkg
        prefs.protectedApps = protectedApps
        val app = ctx.applicationContext
        thread { Shortcuts.syncDynamic(app) }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column {
                Text("Protected apps", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = HC.TextHi)
                Text(
                    "${protectedApps.size} selected · accessibility goes OFF while these are open",
                    fontSize = 13.sp, color = HC.TextLo,
                )
            }
        }
        item {
            HCard(borderColor = HC.Sky.copy(alpha = 0.35f)) {
                Text("TIP", fontFamily = Mono, fontSize = 11.sp, color = HC.Sky, letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Add a lock shortcut to your home screen and open the bank from it. Accessibility is " +
                        "switched off *before* the bank starts, so its check always passes.",
                    fontSize = 13.sp, lineHeight = 19.sp, color = HC.TextLo,
                )
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search apps", color = HC.TextLo) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = HC.TextLo) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HC.Mint,
                    unfocusedBorderColor = HC.Stroke,
                    focusedContainerColor = HC.Surface1,
                    unfocusedContainerColor = HC.Surface1,
                    cursorColor = HC.Mint,
                ),
            )
        }
        if (apps == null) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = HC.Mint)
                }
            }
        } else {
            item { SectionLabel("${shown.size} apps") }
            items(shown, key = { it.pkg }) { app ->
                AppRow(
                    app = app,
                    isProtected = app.pkg in protectedApps,
                    onToggle = { toggle(app.pkg) },
                    onShortcut = {
                        val ok = Shortcuts.requestPin(ctx, app.pkg)
                        val msg = when {
                            !ok -> "Your launcher doesn't support pinned shortcuts"
                            Perms.isXiaomi -> "Nothing appeared? App info > Other permissions > allow Home screen shortcuts"
                            else -> "Shortcut requested"
                        }
                        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                    },
                )
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun AppRow(app: AppEntry, isProtected: Boolean, onToggle: () -> Unit, onShortcut: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isProtected) HC.Mint.copy(alpha = 0.06f) else HC.Surface1)
            .border(1.dp, if (isProtected) HC.Mint.copy(alpha = 0.45f) else HC.Stroke, shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (app.icon != null) {
                Image(app.icon, null, Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)))
            } else {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(HC.Surface3))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        app.label,
                        color = HC.TextHi, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                    )
                    if (app.suggested && !isProtected) {
                        Spacer(Modifier.width(6.dp))
                        Tag("bank?", HC.Amber)
                    }
                }
                Text(app.pkg, fontFamily = Mono, fontSize = 11.sp, color = HC.TextLo, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = isProtected,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = HC.Bg,
                    checkedTrackColor = HC.Mint,
                    uncheckedThumbColor = HC.TextLo,
                    uncheckedTrackColor = HC.Surface3,
                    uncheckedBorderColor = HC.Stroke,
                ),
            )
        }
        if (isProtected) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(HC.Surface2)
                    .clickable(onClick = onShortcut)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.AddToHomeScreen, null, tint = HC.Mint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add lock shortcut to home screen", color = HC.Mint, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
