package com.umkt.ucopy.ui.tile

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.umkt.ucopy.R
import com.umkt.ucopy.ui.service.ClipboardListenerService

@RequiresApi(Build.VERSION_CODES.N)
class QuickSettingsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isRunning = prefs.getBoolean("clipboard_listener", false)

        if (isRunning) {
            ClipboardListenerService.stop(this)
            prefs.edit().putBoolean("clipboard_listener", false).apply()
        } else {
            ClipboardListenerService.start(this)
            prefs.edit().putBoolean("clipboard_listener", true).apply()
        }
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isRunning = prefs.getBoolean("clipboard_listener", false)

        if (isRunning) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "U Copy (ON)"
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "U Copy (OFF)"
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_quick_settings)
        tile.updateTile()
    }
}
