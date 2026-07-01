package com.llamadroid.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.llamadroid.domain.server.OpenAiApi
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ServerTileService : TileService() {

    @Inject lateinit var api: OpenAiApi

    override fun onStartListening() {
        val tile = qsTile ?: return
        tile.state = if (api.isRunning()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onClick() {
        val tile = qsTile ?: return
        if (api.isRunning()) {
            stopService(Intent(this, ServerService::class.java))
            api.stop()
            tile.state = Tile.STATE_INACTIVE
        } else {
            startForegroundService(ServerService.intent(this))
            tile.state = Tile.STATE_ACTIVE
        }
        tile.updateTile()
    }
}
