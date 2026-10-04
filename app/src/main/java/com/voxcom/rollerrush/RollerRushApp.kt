package com.voxcom.rollerrush

import android.app.Application
import com.voxcom.rollerrush.data.GamePreferences
import com.voxcom.rollerrush.data.InventoryData
import com.voxcom.rollerrush.data.PlayerData
import com.voxcom.rollerrush.utils.AssetManager

/**
 * Application-scoped holder for long-lived objects (instead of scattered global singletons).
 * Activities reach it through (application as RollerRushApp).
 */
class RollerRushApp : Application() {
    lateinit var prefs: GamePreferences
        private set
    lateinit var playerData: PlayerData
        private set
    lateinit var inventory: InventoryData
        private set
    // Not called "assets": that name already exists on every Context.
    lateinit var gameAssets: AssetManager
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = GamePreferences(this)
        playerData = PlayerData(prefs)
        inventory = InventoryData(prefs)
        gameAssets = AssetManager(this)
    }
}
