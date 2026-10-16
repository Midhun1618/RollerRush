
package com.voxcom.rollerrush.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.BounceInterpolator
import android.widget.ImageView
import android.widget.TextView
import com.voxcom.rollerrush.R

/**
 * Shows the splash title animation, loads game assets in the background,
 * then navigates to the main menu.
 */
class SplashActivity : BaseActivity() {

    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var splashDestroyed = false

    private var assetsLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        animateTitle()

        val loadingLabel =
            findViewById<TextView?>(R.id.loadingLabel)

        val startTime = System.currentTimeMillis()

        Thread {
            try {
                app.gameAssets.ensureLoaded()
                assetsLoaded = true

                val elapsed = System.currentTimeMillis() - startTime
                val wait = (1200L - elapsed).coerceAtLeast(0L)

                handler.postDelayed({
                    if (!isFinishing && !isDestroyed && !splashDestroyed) {
                        loadingLabel?.text = "Ready!"

                        startActivity(
                            Intent(this, MainMenuActivity::class.java)
                        )
                        finish()
                    }
                }, wait)

            } catch (e: Exception) {
                handler.post {
                    if (!isFinishing && !isDestroyed && !splashDestroyed) {
                        loadingLabel?.text = "Unable to load game assets"
                    }
                }
            }
        }.start()
    }

    private fun animateTitle() {
        val title1 = findViewById<ImageView>(R.id.title1)
        val title2 = findViewById<ImageView>(R.id.title2)

        title1.post {
            if (isFinishing || isDestroyed || splashDestroyed) {
                return@post
            }

            // Use the actual screen width so both images start off-screen.
            val screenWidth =
                resources.displayMetrics.widthPixels.toFloat()

            title1.animate().cancel()
            title2.animate().cancel()

            title1.translationX = -screenWidth
            title2.translationX = -screenWidth

            title1.scaleX = 0.85f
            title1.scaleY = 0.85f
            title2.scaleX = 0.85f
            title2.scaleY = 0.85f

            title1.visibility = View.VISIBLE
            title2.visibility = View.VISIBLE

            // First title image enters quickly and bounces into position.
            title1.animate()
                .translationX(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(650L)
                .setInterpolator(BounceInterpolator())
                .start()

            // Second image follows with a slight delay.
            title2.animate()
                .translationX(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(180L)
                .setDuration(700L)
                .setInterpolator(BounceInterpolator())
                .start()
        }
    }

    override fun onDestroy() {
        splashDestroyed = true

        handler.removeCallbacksAndMessages(null)

        findViewById<ImageView?>(R.id.title1)?.animate()?.cancel()
        findViewById<ImageView?>(R.id.title2)?.animate()?.cancel()

        super.onDestroy()
    }
}
