package com.alienkb.handler.core

import android.content.Context
import android.provider.Settings

/** Optional: hide Developer options / USB debugging while a protected app is open. */
object DevOptions {
    fun hide(ctx: Context, p: Prefs) {
        val cr = ctx.contentResolver
        val adb = Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0)
        val dev = Settings.Global.getInt(cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0)
        if (adb == 0 && dev == 0) return
        if (p.savedAdb < 0) {
            p.savedAdb = adb
            p.savedDev = dev
        }
        try {
            if (adb == 1) Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 0)
            if (dev == 1) Settings.Global.putInt(cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0)
            p.log("Developer options hidden")
        } catch (_: SecurityException) {
            p.log("Couldn't hide developer options")
        }
    }

    fun restore(ctx: Context, p: Prefs) {
        if (p.savedAdb < 0) return
        val cr = ctx.contentResolver
        try {
            if (p.savedDev == 1) Settings.Global.putInt(cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1)
            if (p.savedAdb == 1) Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
        } catch (_: SecurityException) {
        }
        p.savedAdb = -1
        p.savedDev = -1
    }
}
