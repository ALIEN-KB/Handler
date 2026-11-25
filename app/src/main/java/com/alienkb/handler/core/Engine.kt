package com.alienkb.handler.core

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.TileService
import android.widget.Toast
import com.alienkb.handler.service.A11yTileService
import com.alienkb.handler.service.GuardService

/**
 * The brain: pause accessibility for protected apps, bring it back afterwards,
 * and repair it when the system (hello, HyperOS) switches it off behind our back.
 */
object Engine {
    const val MANUAL = "@manual"
    const val REASON_SETTING = "setting changed"

    private const val STUCK_AFTER_MS = 60_000L
    private const val STUCK_COOLDOWN_MS = 30 * 60_000L
    private const val STALE_PAUSE_MS = 30 * 60_000L

    private val main = Handler(Looper.getMainLooper())
    private const val SETTLE_MS = 2_500L

    private var notBoundSince = 0L
    private var lastKick = 0L

    @Volatile
    private var lastEnabledAt = 0L

    private fun now() = System.currentTimeMillis()

    private fun nap(ms: Long) = try {
        Thread.sleep(ms)
    } catch (_: InterruptedException) {
    }

    /** Switch services on and remember when, so we never switch them off mid-startup. */
    private fun enable(app: Context, targets: Collection<String>) {
        A11y.setEnabled(app, targets, true)
        lastEnabledAt = now()
    }

    /**
     * Some accessibility apps (Curbox included) crash if they're switched off before
     * onServiceConnected() has run. After switching a service on, wait until the system
     * has bound it (plus a small margin) before switching it off again. Max ~2.5 s.
     */
    private fun waitUntilStarted(app: Context, targets: Collection<String>) {
        val deadline = lastEnabledAt + SETTLE_MS
        if (now() >= deadline) return
        val wanted = targets.mapNotNull { A11y.parse(it) }
        while (now() < deadline) {
            val bound = A11y.boundComponents(app) ?: break
            if (wanted.all { it in bound }) {
                nap(400)
                return
            }
            nap(100)
        }
    }

    /** Wait (max 2 s) until the system has really unbound the services. */
    private fun waitUntilStopped(app: Context, targets: Collection<String>) {
        val wanted = targets.mapNotNull { A11y.parse(it) }
        val deadline = now() + 2_000L
        while (now() < deadline) {
            val bound = A11y.boundComponents(app) ?: return
            if (wanted.none { it in bound }) return
            nap(100)
        }
    }

    fun label(ctx: Context, pkg: String?): String = when (pkg) {
        null -> "-"
        MANUAL -> "manual pause"
        else -> try {
            val pm = ctx.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (_: Exception) {
            pkg
        }
    }

    fun targetNames(ctx: Context, targets: Collection<String>): String =
        targets.mapNotNull { A11y.parse(it) }.joinToString { label(ctx, it.packageName) }.ifEmpty { "-" }

    private fun isInstalled(ctx: Context, pkg: String) = try {
        ctx.packageManager.getApplicationInfo(pkg, 0); true
    } catch (_: Exception) {
        false
    }

    /** Turn the managed services off because [by] (a package or MANUAL) is on screen. */
    @Synchronized
    fun pause(ctx: Context, by: String, reason: String): Boolean {
        val app = ctx.applicationContext
        val p = Prefs(app)
        if (!A11y.canWrite(app)) {
            p.log("Can't pause: secure settings permission missing")
            return false
        }
        if (p.targets.isEmpty()) {
            p.log("Can't pause: no service selected")
            return false
        }
        val previous = p.suspendedBy
        if (previous == null) waitUntilStarted(app, p.targets)
        A11y.setEnabled(app, p.targets, false)
        p.suspendedBy = by
        p.suspendedAt = System.currentTimeMillis()
        p.snoozedPkg = null
        if (previous == null) {
            if (p.hideDevOptions) DevOptions.hide(app, p)
            p.pauseCount = p.pauseCount + 1
            p.log("Paused for ${label(app, by)} · $reason")
            toast(app, p, "Accessibility paused for ${label(app, by)}")
        } else if (previous != by) {
            p.log("Pause handed over to ${label(app, by)}")
        }
        afterChange(app)
        return true
    }

    /** Turn the managed services back on. */
    @Synchronized
    fun resume(ctx: Context, reason: String, manual: Boolean = false) {
        val app = ctx.applicationContext
        val p = Prefs(app)
        if (p.suspendedBy == null) {
            if (manual) {
                enable(app, p.targets)
                afterChange(app)
            }
            return
        }
        p.suspendedBy = null
        p.suspendedAt = 0L
        enable(app, p.targets)
        DevOptions.restore(app, p)
        if (manual) {
            p.snoozedPkg = ForegroundTracker.current(app)?.takeIf { it in p.protectedApps }
        }
        p.log("Back ON · $reason")
        toast(app, p, "Accessibility back ON")
        notBoundSince = 0L
        afterChange(app)
    }

    /** Watchdog: make sure the managed services are on (unless they should be paused). */
    @Synchronized
    fun ensureHealthy(ctx: Context, reason: String) {
        val app = ctx.applicationContext
        val p = Prefs(app)
        if (!A11y.canWrite(app) || p.targets.isEmpty()) return

        val pausedBy = p.suspendedBy
        if (pausedBy != null) {
            // The user switched it back on by hand after a manual pause: the pause is over.
            if (pausedBy == MANUAL && A11y.enabledCount(app, p.targets) == p.targets.size) {
                p.suspendedBy = null
                p.suspendedAt = 0L
                DevOptions.restore(app, p)
                p.log("Manual pause ended (turned on in Settings)")
                afterChange(app)
            } else {
                checkStale(app, p)
            }
            return
        }
        if (!p.guardEnabled) return

        val targets = p.targets.mapNotNull { A11y.parse(it) }.filter { isInstalled(app, it.packageName) }
        if (targets.isEmpty()) return
        val enabled = A11y.enabledComponents(app)
        val missing = targets.filter { it !in enabled }

        if (missing.isNotEmpty()) {
            val fg = ForegroundTracker.current(app)
            if (fg != null && fg in p.protectedApps && fg != p.snoozedPkg) {
                pause(app, fg, "protected app open")
                return
            }
            if (fg != null && fg in ForegroundTracker.SETTINGS && reason == REASON_SETTING) {
                // You turned it off yourself: treat it as a manual pause, back ON when you lock the phone.
                pause(app, MANUAL, "turned off in Settings")
                return
            }
            enable(app, p.targets)
            p.restoreCount = p.restoreCount + 1
            p.log("Watchdog restored ${missing.joinToString { label(app, it.packageName) }} · $reason")
            toast(app, p, "Handler switched accessibility back ON")
            notBoundSince = 0L
            afterChange(app)
            return
        }

        // Enabled in Settings but not actually running (crashed / killed): kick it.
        val bound = A11y.boundComponents(app) ?: return
        val stuck = targets.filter { it !in bound }
        val now = System.currentTimeMillis()
        if (stuck.isEmpty()) {
            notBoundSince = 0L
            return
        }
        if (notBoundSince == 0L) {
            notBoundSince = now
            return
        }
        if (now - notBoundSince >= STUCK_AFTER_MS && now - lastKick >= STUCK_COOLDOWN_MS) {
            lastKick = now
            notBoundSince = 0L
            val flat = stuck.map { it.flattenToString() }
            waitUntilStarted(app, flat)
            A11y.setEnabled(app, flat, false)
            waitUntilStopped(app, flat)
            nap(300)
            enable(app, flat)
            p.restoreCount = p.restoreCount + 1
            p.log("Watchdog restarted ${stuck.joinToString { label(app, it.packageName) }} (it was stuck)")
            afterChange(app)
        }
    }

    /** Safety net: never stay paused forever if something went wrong. */
    private fun checkStale(app: Context, p: Prefs) {
        val at = p.suspendedAt
        if (at == 0L || System.currentTimeMillis() - at < STALE_PAUSE_MS) return
        val fg = ForegroundTracker.current(app)
        if (fg != null && fg in p.protectedApps) return
        resume(app, "pause was over 30 min old")
    }

    /** If no service is chosen yet, pick anything that looks like Curbox. */
    fun autoPickTarget(ctx: Context) {
        val p = Prefs(ctx)
        if (p.targets.isNotEmpty()) return
        val match = A11y.installed(ctx).filter {
            it.pkg.contains("curb", true) || it.label.contains("curb", true) || it.appLabel.contains("curb", true)
        }
        if (match.isNotEmpty()) {
            p.targets = match.map { it.component }.toSet()
            p.log("Found ${match.joinToString { it.appLabel }}: now managed")
        }
    }

    private fun afterChange(app: Context) {
        GuardService.updateNotification(app)
        try {
            TileService.requestListeningState(app, ComponentName(app, A11yTileService::class.java))
        } catch (_: Exception) {
        }
    }

    private fun toast(app: Context, p: Prefs, msg: String) {
        if (!p.showToasts) return
        main.post { Toast.makeText(app, msg, Toast.LENGTH_SHORT).show() }
    }
}
