package com.alienkb.handler.core

import android.content.Context
import androidx.core.content.edit

/** Everything Handler remembers, in one small SharedPreferences file. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("handler", Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = sp.getBoolean("onboarding_done", false)
        set(v) = sp.edit { putBoolean("onboarding_done", v) }

    /** Accessibility services Handler manages, as flattened ComponentNames. */
    var targets: Set<String>
        get() = sp.getStringSet("targets", emptySet())!!.toSet()
        set(v) = sp.edit { putStringSet("targets", v.toSet()) }

    /** Apps that must never see an accessibility service turned on. */
    var protectedApps: Set<String>
        get() = sp.getStringSet("protected", emptySet())!!.toSet()
        set(v) = sp.edit { putStringSet("protected", v.toSet()) }

    var guardEnabled: Boolean
        get() = sp.getBoolean("guard", true)
        set(v) = sp.edit { putBoolean("guard", v) }

    var autoDetect: Boolean
        get() = sp.getBoolean("auto_detect", true)
        set(v) = sp.edit { putBoolean("auto_detect", v) }

    var resumeOnScreenOff: Boolean
        get() = sp.getBoolean("resume_screen_off", true)
        set(v) = sp.edit { putBoolean("resume_screen_off", v) }

    var graceSeconds: Int
        get() = sp.getInt("grace", 8)
        set(v) = sp.edit { putInt("grace", v) }

    var hideDevOptions: Boolean
        get() = sp.getBoolean("hide_dev", false)
        set(v) = sp.edit { putBoolean("hide_dev", v) }

    var showToasts: Boolean
        get() = sp.getBoolean("toasts", true)
        set(v) = sp.edit { putBoolean("toasts", v) }

    /** Package (or Engine.MANUAL) that accessibility is currently paused for. Null = not paused. */
    var suspendedBy: String?
        get() = sp.getString("suspended_by", null)
        set(v) = sp.edit { putString("suspended_by", v) }

    var suspendedAt: Long
        get() = sp.getLong("suspended_at", 0L)
        set(v) = sp.edit { putLong("suspended_at", v) }

    /** App the user explicitly resumed inside; don't auto-pause it again until they leave it. */
    var snoozedPkg: String?
        get() = sp.getString("snoozed", null)
        set(v) = sp.edit { putString("snoozed", v) }

    var savedAdb: Int
        get() = sp.getInt("saved_adb", -1)
        set(v) = sp.edit { putInt("saved_adb", v) }

    var savedDev: Int
        get() = sp.getInt("saved_dev", -1)
        set(v) = sp.edit { putInt("saved_dev", v) }

    var restoreCount: Int
        get() = sp.getInt("restore_count", 0)
        set(v) = sp.edit { putInt("restore_count", v) }

    var pauseCount: Int
        get() = sp.getInt("pause_count", 0)
        set(v) = sp.edit { putInt("pause_count", v) }

    @Synchronized
    fun log(message: String) {
        val clean = message.replace('\n', ' ').replace('\t', ' ')
        val lines = (sp.getString("log", "") ?: "").lines().filter { it.isNotBlank() }
        val next = (lines + "${System.currentTimeMillis()}\t$clean").takeLast(MAX_LOG)
        sp.edit { putString("log", next.joinToString("\n")) }
    }

    fun logs(): List<Pair<Long, String>> =
        (sp.getString("log", "") ?: "").lines().mapNotNull { line ->
            val i = line.indexOf('\t')
            if (i <= 0) null else line.substring(0, i).toLongOrNull()?.let { it to line.substring(i + 1) }
        }.reversed()

    fun clearLog() = sp.edit { remove("log") }

    companion object {
        private const val MAX_LOG = 60
    }
}
