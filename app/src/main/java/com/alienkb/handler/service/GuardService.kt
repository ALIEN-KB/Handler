package com.alienkb.handler.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.alienkb.handler.R
import com.alienkb.handler.core.A11y
import com.alienkb.handler.core.Engine
import com.alienkb.handler.core.ForegroundTracker
import com.alienkb.handler.core.Prefs
import com.alienkb.handler.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Long-running watchdog. Notices instantly when accessibility gets switched off,
 * tracks when you leave a protected app, and turns things back on.
 */
class GuardService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var observer: ContentObserver? = null
    private var screenOffJob: Job? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenOffJob?.cancel()
                    screenOffJob = scope.launch {
                        delay(SCREEN_OFF_DELAY_MS)
                        val p = Prefs(this@GuardService)
                        if (p.suspendedBy != null && p.resumeOnScreenOff && !isInteractive()) {
                            Engine.resume(this@GuardService, "phone locked")
                        }
                    }
                }
                Intent.ACTION_SCREEN_ON -> screenOffJob?.cancel()
                Intent.ACTION_USER_PRESENT -> scope.launch { Engine.ensureHealthy(this@GuardService, "unlock") }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        running = true
        goForeground()
        val obs = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                scope.launch {
                    delay(300)
                    Engine.ensureHealthy(this@GuardService, Engine.REASON_SETTING)
                    updateNotification(this@GuardService)
                }
            }
        }
        observer = obs
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES), false, obs,
        )
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        scope.launch { Engine.ensureHealthy(this@GuardService, "guard started") }
        scope.launch { monitorLoop() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        goForeground()
        when (intent?.action) {
            ACTION_PAUSE -> scope.launch { Engine.suspend(this@GuardService, Engine.MANUAL, "from notification") }
            ACTION_RESUME -> scope.launch { Engine.resume(this@GuardService, "from notification", manual = true) }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        observer?.let { contentResolver.unregisterContentObserver(it) }
        try {
            unregisterReceiver(receiver)
        } catch (_: Exception) {
        }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun isInteractive(): Boolean =
        getSystemService(PowerManager::class.java)?.isInteractive != false

    private fun goForeground() {
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        try {
            ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(this), type)
        } catch (_: Exception) {
            stopSelf()
        }
    }

    /** Polls the foreground app: pauses on protected apps, resumes after you leave. */
    private suspend fun monitorLoop() {
        var leftSince = 0L
        var lastHealth = 0L
        while (true) {
            val p = Prefs(this)
            val now = System.currentTimeMillis()
            if (now - lastHealth > 30_000L) {
                lastHealth = now
                Engine.ensureHealthy(this, "routine check")
            }
            if (!isInteractive()) {
                leftSince = 0L
                delay(2_000)
                continue
            }
            val fg = ForegroundTracker.current(this)
            val pausedBy = p.suspendedBy

            if (pausedBy != null) {
                if (pausedBy == Engine.MANUAL || fg == null) {
                    leftSince = 0L
                    delay(1_000)
                    continue
                }
                if (fg in p.protectedApps && fg != pausedBy) {
                    Engine.suspend(this, fg, "switched app")
                }
                val stillInside = fg in p.protectedApps || fg in ForegroundTracker.NEUTRAL || fg == packageName
                if (stillInside || now - p.suspendedAt < LAUNCH_GRACE_MS) {
                    leftSince = 0L
                } else {
                    if (leftSince == 0L) leftSince = now
                    if (now - leftSince >= p.graceSeconds * 1_000L) {
                        Engine.resume(this, "left ${Engine.label(this, pausedBy)}")
                        leftSince = 0L
                    }
                }
                delay(700)
            } else {
                leftSince = 0L
                val snoozed = p.snoozedPkg
                if (snoozed != null && fg != null && fg != snoozed && fg !in ForegroundTracker.NEUTRAL) {
                    p.snoozedPkg = null
                }
                if (p.autoDetect && fg != null && fg in p.protectedApps && fg != p.snoozedPkg &&
                    A11y.canWrite(this) && p.targets.isNotEmpty()
                ) {
                    Engine.suspend(this, fg, "auto-detected")
                    delay(700)
                } else {
                    delay(if (p.autoDetect && p.protectedApps.isNotEmpty()) 450 else 3_000)
                }
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "guard"
        const val NOTIF_ID = 42
        const val ACTION_PAUSE = "com.alienkb.handler.PAUSE"
        const val ACTION_RESUME = "com.alienkb.handler.RESUME"
        private const val SCREEN_OFF_DELAY_MS = 15_000L
        private const val LAUNCH_GRACE_MS = 4_000L

        @Volatile
        var running = false
            private set

        fun start(ctx: Context) {
            try {
                ContextCompat.startForegroundService(ctx, Intent(ctx, GuardService::class.java))
            } catch (_: Exception) {
                // Background start not allowed right now; the worker / next app open will retry.
            }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, GuardService::class.java))
        }

        fun updateNotification(ctx: Context) {
            if (!running) return
            try {
                ctx.getSystemService(NotificationManager::class.java)?.notify(NOTIF_ID, buildNotification(ctx))
            } catch (_: Exception) {
            }
        }

        fun buildNotification(ctx: Context): Notification {
            val p = Prefs(ctx)
            val paused = p.suspendedBy
            val title: String
            val text: String
            when {
                !A11y.canWrite(ctx) -> {
                    title = "Setup needed"
                    text = "Grant the one-time ADB permission so Handler can work"
                }
                p.targets.isEmpty() -> {
                    title = "Pick a service"
                    text = "Choose which accessibility service Handler should manage"
                }
                paused != null -> {
                    title = "Accessibility paused"
                    text = if (paused == Engine.MANUAL) "Manual pause · back ON when you lock the phone"
                    else "For ${Engine.label(ctx, paused)} · back ON when you leave"
                }
                else -> {
                    title = "Handler is on watch"
                    text = "Guarding ${Engine.targetNames(ctx, p.targets)} · ${p.protectedApps.size} protected apps"
                }
            }
            val open = PendingIntent.getActivity(
                ctx, 0, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val actionIntent = Intent(ctx, GuardService::class.java)
                .setAction(if (paused != null) ACTION_RESUME else ACTION_PAUSE)
            val action = PendingIntent.getService(
                ctx, 1, actionIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val b = NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_handler)
                .setContentTitle(title)
                .setContentText(text)
                .setColor(0xFF5EF2C6.toInt())
                .setOngoing(true)
                .setSilent(true)
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setContentIntent(open)
            if (A11y.canWrite(ctx) && p.targets.isNotEmpty()) {
                b.addAction(0, if (paused != null) "Turn back ON" else "Pause now", action)
            }
            return b.build()
        }
    }
}
