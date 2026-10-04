package com.voxcom.rollerrush.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper

/** Shows the title, loads assets on a background thread, then goes to the main menu. */
class SplashActivity : BaseActivity() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(UiKit.screen(this, UiKit.title(this, "ROLLER RUSH", 48f), UiKit.label(this, "Loading...")))

        val start = System.currentTimeMillis()
        Thread {
            app.gameAssets.ensureLoaded()
            val wait = (1200 - (System.currentTimeMillis() - start)).coerceAtLeast(0)
            handler.postDelayed({
                if (!isFinishing) {
                    startActivity(Intent(this, MainMenuActivity::class.java))
                    finish()
                }
            }, wait)
        }.start()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
