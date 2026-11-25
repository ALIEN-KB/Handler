package com.alienkb.handler.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.alienkb.handler.core.A11y
import com.alienkb.handler.core.Engine
import com.alienkb.handler.core.Prefs

/** Quick Settings tile: tap to pause / resume accessibility by hand. */
class A11yTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        render()
    }

    override fun onClick() {
        super.onClick()
        val p = Prefs(this)
        if (!A11y.canWrite(this) || p.targets.isEmpty()) return
        if (p.suspendedBy != null) Engine.resume(this, "quick tile", manual = true)
        else Engine.suspend(this, Engine.MANUAL, "quick tile")
        render()
    }

    private fun render() {
        val tile = qsTile ?: return
        val p = Prefs(this)
        val ready = A11y.canWrite(this) && p.targets.isNotEmpty()
        val paused = p.suspendedBy != null
        tile.state = when {
            !ready -> Tile.STATE_UNAVAILABLE
            paused -> Tile.STATE_INACTIVE
            else -> Tile.STATE_ACTIVE
        }
        tile.label = "A11y Guard"
        if (Build.VERSION.SDK_INT >= 29) {
            tile.subtitle = when {
                !ready -> "Setup needed"
                paused -> "Paused"
                else -> "On"
            }
        }
        tile.updateTile()
    }
}
