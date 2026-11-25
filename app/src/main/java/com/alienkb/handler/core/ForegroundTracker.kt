package com.alienkb.handler.core

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

/** Figures out which app is on screen using Usage Access (no accessibility needed). */
object ForegroundTracker {

    /** Screens that briefly appear on top but don't mean "the user left the app". */
    val NEUTRAL = setOf(
        "android",
        "com.android.systemui",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.google.android.inputmethod.latin",
        "com.miui.securityinputmethod",
    )

    val SETTINGS = setOf(
        "com.android.settings",
        "com.miui.securitycenter",
        "com.android.settings.intelligence",
    )

    // UsageEvents.Event.ACTIVITY_RESUMED (== the old MOVE_TO_FOREGROUND)
    private const val RESUMED = 1

    private var lastPkg: String? = null
    private var lastQuery = 0L

    @Synchronized
    fun current(ctx: Context): String? {
        if (!Perms.usageAccess(ctx)) return null
        val usm = ctx.getSystemService(UsageStatsManager::class.java) ?: return lastPkg
        val now = System.currentTimeMillis()
        val from = if (lastQuery == 0L) now - 3_600_000L else lastQuery - 3_000L
        var bestTs = -1L
        var best: String? = null
        try {
            val events = usm.queryEvents(from, now)
            val e = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(e)
                if (e.eventType == RESUMED && e.timeStamp >= bestTs) {
                    bestTs = e.timeStamp
                    best = e.packageName
                }
            }
        } catch (_: Exception) {
        }
        if (best != null) lastPkg = best
        lastQuery = now
        return lastPkg
    }
}
