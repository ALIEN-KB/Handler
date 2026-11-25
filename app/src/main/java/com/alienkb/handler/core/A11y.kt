package com.alienkb.handler.core

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

/** Reads and writes the system list of enabled accessibility services. */
object A11y {

    data class ServiceEntry(
        val component: String,
        val label: String,
        val pkg: String,
        val appLabel: String,
    )

    fun canWrite(ctx: Context): Boolean =
        ctx.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    fun parse(flat: String): ComponentName? = ComponentName.unflattenFromString(flat)

    /** Services switched on in Settings (what banking apps look at). */
    fun enabledComponents(ctx: Context): LinkedHashSet<ComponentName> {
        val raw = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return raw.split(':')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { ComponentName.unflattenFromString(it) }
            .toCollection(LinkedHashSet())
    }

    fun enabledCount(ctx: Context, targets: Collection<String>): Int {
        val enabled = enabledComponents(ctx)
        return targets.mapNotNull { parse(it) }.count { it in enabled }
    }

    /** Services the system has actually bound (running). Null if unknown. */
    fun boundComponents(ctx: Context): Set<ComponentName>? = try {
        val am = ctx.getSystemService(AccessibilityManager::class.java)
        am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .mapNotNull { info -> info.resolveInfo?.serviceInfo?.let { ComponentName(it.packageName, it.name) } }
            .toSet()
    } catch (_: Exception) {
        null
    }

    /** Switch the given services on or off. Returns false if Handler lacks the permission. */
    fun setEnabled(ctx: Context, targets: Collection<String>, enabled: Boolean): Boolean {
        if (!canWrite(ctx)) return false
        val current = enabledComponents(ctx)
        val wanted = targets.mapNotNull { parse(it) }
        val next = LinkedHashSet(current)
        if (enabled) next.addAll(wanted) else next.removeAll(wanted.toSet())
        if (next == current) return true
        return try {
            val cr = ctx.contentResolver
            Settings.Secure.putString(
                cr,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                next.joinToString(":") { it.flattenToString() },
            )
            if (next.isNotEmpty()) Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun installed(ctx: Context): List<ServiceEntry> {
        val pm = ctx.packageManager
        val am = ctx.getSystemService(AccessibilityManager::class.java) ?: return emptyList()
        return am.installedAccessibilityServiceList.mapNotNull { info ->
            val si = info.resolveInfo?.serviceInfo ?: return@mapNotNull null
            val appLabel = try {
                pm.getApplicationLabel(pm.getApplicationInfo(si.packageName, 0)).toString()
            } catch (_: Exception) {
                si.packageName
            }
            ServiceEntry(
                component = ComponentName(si.packageName, si.name).flattenToString(),
                label = si.loadLabel(pm).toString(),
                pkg = si.packageName,
                appLabel = appLabel,
            )
        }.filter { it.pkg != ctx.packageName }.sortedBy { it.label.lowercase() }
    }
}
