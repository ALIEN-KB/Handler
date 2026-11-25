package com.alienkb.handler

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.alienkb.handler.core.Prefs
import com.alienkb.handler.service.GuardService
import com.alienkb.handler.service.WatchdogWorker
import java.util.concurrent.TimeUnit

class HandlerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(GuardService.CHANNEL_ID, "Watchdog", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Keeps Handler alive so it can guard your accessibility service"
                setShowBadge(false)
            },
        )
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "handler-watchdog",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WatchdogWorker>(15, TimeUnit.MINUTES).build(),
        )
        val p = Prefs(this)
        if (p.onboardingDone && p.guardEnabled) GuardService.start(this)
    }
}
