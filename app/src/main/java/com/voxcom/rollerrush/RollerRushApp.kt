package com.voxcom.rollerrush

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.voxcom.rollerrush.audio.BgmManager
import com.voxcom.rollerrush.data.FirebaseGameService
import com.voxcom.rollerrush.data.GamePreferences
import com.voxcom.rollerrush.data.InventoryData
import com.voxcom.rollerrush.data.PlayerData
import com.voxcom.rollerrush.utils.AssetManager
import com.google.android.gms.ads.MobileAds


class RollerRushApp : Application() {
    lateinit var prefs: GamePreferences
        private set
    lateinit var firebaseGameService: FirebaseGameService
        private set
    lateinit var playerData: PlayerData
        private set
    lateinit var inventory: InventoryData
        private set
    lateinit var gameAssets: AssetManager
        private set
    lateinit var bgmManager: BgmManager
        private set

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
        prefs = GamePreferences(this)
        playerData = PlayerData(prefs)
        inventory = InventoryData(prefs)
        gameAssets = AssetManager(this)
        bgmManager = BgmManager(this)

        firebaseGameService = FirebaseGameService(prefs)

        firebaseGameService.initialize { success, error ->
            if (success) {
                android.util.Log.d(
                    "RollerRushFirebase",
                    """
            Player initialized!
            UID: ${firebaseGameService.currentUid}
            Username: ${firebaseGameService.displayName}
            """.trimIndent()
                )
                firebaseGameService.syncPendingRuns()
            } else {
                android.util.Log.e(
                    "RollerRushFirebase",
                    "Player initialization failed: $error"
                )
            }
        }

        registerMusicLifecycle()
    }

    private var startedActivityCount = 0

    private fun registerMusicLifecycle() {
        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {

                override fun onActivityStarted(activity: Activity) {
                    startedActivityCount++

                    // The foreground Activity will select its own track
                    // through its existing onStart() method.
                }

                override fun onActivityStopped(activity: Activity) {
                    startedActivityCount =
                        (startedActivityCount - 1).coerceAtLeast(0)

                    if (startedActivityCount == 0) {
                        bgmManager.pause()
                    }
                }

                override fun onActivityCreated(
                    activity: Activity,
                    savedInstanceState: Bundle?
                ) {}

                override fun onActivityResumed(activity: Activity) {}
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: Bundle
                ) {}

                override fun onActivityDestroyed(activity: Activity) {}
            }
        )
    }
}
