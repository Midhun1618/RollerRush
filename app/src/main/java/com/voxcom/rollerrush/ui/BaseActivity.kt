package com.voxcom.rollerrush.ui

import android.app.Activity
import com.voxcom.rollerrush.RollerRushApp
import com.voxcom.rollerrush.utils.ScreenUtils

/** Shared immersive-mode handling + quick access to app-level objects. */
abstract class BaseActivity : Activity() {
    protected val app: RollerRushApp get() = application as RollerRushApp

    override fun onResume() {
        super.onResume()
        ScreenUtils.hideSystemUi(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) ScreenUtils.hideSystemUi(this)
    }
}
