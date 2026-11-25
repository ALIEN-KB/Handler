package com.alienkb.handler.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alienkb.handler.core.Engine
import com.alienkb.handler.core.Prefs
import kotlin.concurrent.thread

/** After a reboot or update: undo any leftover pause, repair, and restart the watchdog. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        thread {
            try {
                val p = Prefs(app)
                if (p.suspendedBy != null) Engine.resume(app, "phone restarted")
                Engine.ensureHealthy(app, "boot")
                if (p.onboardingDone && p.guardEnabled) GuardService.start(app)
            } finally {
                pending.finish()
            }
        }
    }
}
