package com.alienkb.handler.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.alienkb.handler.core.Engine
import com.alienkb.handler.core.Prefs
import com.alienkb.handler.service.GuardService

/** Invisible: pause accessibility first, then open the protected app. */
class SafeLaunchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_PKG)
        val launch = pkg?.let { packageManager.getLaunchIntentForPackage(it) }
        if (pkg == null || launch == null) {
            Toast.makeText(this, "That app isn't installed any more", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val p = Prefs(this)
        val paused = pkg in p.protectedApps && Engine.pause(this, pkg, "lock shortcut")
        GuardService.start(this)
        // Give the system a moment to unbind the service before the app checks for it.
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {
            }
            finish()
            overridePendingTransition(0, 0)
        }, if (paused) LAUNCH_DELAY_MS else 0L)
    }

    companion object {
        const val EXTRA_PKG = "pkg"
        private const val LAUNCH_DELAY_MS = 450L
    }
}
