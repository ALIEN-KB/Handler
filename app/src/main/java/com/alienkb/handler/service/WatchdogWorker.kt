package com.alienkb.handler.service

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alienkb.handler.core.Engine
import com.alienkb.handler.core.Prefs

/** Runs every ~15 minutes even if the watchdog service was killed. */
class WatchdogWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val app = applicationContext
        Engine.ensureHealthy(app, "15-min check")
        val p = Prefs(app)
        if (p.onboardingDone && p.guardEnabled && !GuardService.running) GuardService.start(app)
        return Result.success()
    }
}
