package com.voxcom.rollerrush.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import com.voxcom.rollerrush.R
import com.voxcom.rollerrush.data.MissionSystem
import com.voxcom.rollerrush.game.GameState
import com.voxcom.rollerrush.game.GameView
import com.voxcom.rollerrush.utils.Constants

@Suppress("DEPRECATION")
class GameActivity : BaseActivity(), GameView.Listener {

    private lateinit var gameView: GameView
    private lateinit var root: FrameLayout

    private lateinit var boosterAlert: ImageView
    private lateinit var boosterBurst: ImageView
    private lateinit var mysteryFrames: List<ImageView>

    private var pauseScreenOpen = false
    private var gameOverHandled = false
    private var boosterIsAvailable = false

    private var boosterAlertAnimator: AnimatorSet? = null
    private var mysteryAnimator: AnimatorSet? = null

    // -------------------------------------------------------------------------
    // Activity creation and overlay setup
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        app.gameAssets.ensureLoaded()

        root = FrameLayout(this)

        // The game remains the background layer.
        gameView = GameView(this).also {
            it.listener = this
        }

        root.addView(
            gameView,
            fullScreenParams()
        )

        // Booster burst overlay.
        boosterBurst = createOverlay(
            R.drawable.booster_alert_burst
        ).apply {
            visibility = View.GONE
            alpha = 0f
        }

        root.addView(
            boosterBurst,
            fullScreenParams()
        )

        // Double-tap activation alert.
        boosterAlert = createOverlay(
            R.drawable.booster_tap_alert
        ).apply {
            visibility = View.GONE
            alpha = 0f
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
        }

        val alertWidth = (
                resources.displayMetrics.widthPixels * 0.3f
                ).toInt().coerceAtLeast(260)

        val alertParams = FrameLayout.LayoutParams(
            alertWidth,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL
        ).apply {
            topMargin = (
                    resources.displayMetrics.heightPixels * 0.06f
                    ).toInt()
        }

        root.addView(
            boosterAlert,
            alertParams
        )

        // Mystery reward cinematic: four transparent PNG/WebP frames.
        mysteryFrames = listOf(
            createOverlay(R.drawable.mystery_overlay_1),
            createOverlay(R.drawable.mystery_overlay_2),
            createOverlay(R.drawable.mystery_overlay_3),
            createOverlay(R.drawable.mystery_overlay_4)
        )

        mysteryFrames.forEach { frame ->
            frame.visibility = View.GONE
            frame.alpha = 0f

            root.addView(
                frame,
                fullScreenParams()
            )
        }

        setContentView(root)
    }

    private fun createOverlay(resourceId: Int): ImageView {
        return ImageView(this).apply {
            setImageResource(resourceId)
            scaleType = ImageView.ScaleType.FIT_XY
            isClickable = false
            isFocusable = false

            importantForAccessibility =
                View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
    }

    private fun fullScreenParams(): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
    }

    // -------------------------------------------------------------------------
    // Booster alert
    // -------------------------------------------------------------------------

    override fun onBoosterAvailabilityChanged(
        available: Boolean
    ) {
        runOnUiThread {
            boosterIsAvailable = available

            if (
                available &&
                gameView.state == GameState.PLAYING
            ) {
                showBoosterAlert()
            } else {
                hideBoosterAlert()
            }
        }
    }


    private fun showBoosterAlert() {
        boosterAlertAnimator?.cancel()
        boosterAlert.animate().cancel()
        boosterBurst.animate().cancel()

        // Full-screen burst image.
        boosterBurst.scaleType = ImageView.ScaleType.FIT_XY
        boosterBurst.visibility = View.VISIBLE
        boosterBurst.alpha = 0.8f
        boosterBurst.scaleX = 1f
        boosterBurst.scaleY = 1f
        boosterBurst.translationX = 0f
        boosterBurst.translationY = 0f

        val burstParams =
            boosterBurst.layoutParams as FrameLayout.LayoutParams

        burstParams.width = FrameLayout.LayoutParams.MATCH_PARENT
        burstParams.height = FrameLayout.LayoutParams.MATCH_PARENT
        burstParams.gravity = Gravity.TOP or Gravity.START
        burstParams.leftMargin = 0
        burstParams.topMargin = 0
        boosterBurst.layoutParams = burstParams

        boosterBurst.bringToFront()
        boosterAlert.bringToFront()

        // Zoom pulse: the image starts full-screen and grows beyond
        // the screen edges, then returns to full-screen scale.
        val burstZoomX = ObjectAnimator.ofFloat(
            boosterBurst,
            View.SCALE_X,
            1f,
            1.18f,
            1f
        ).apply {
            duration = 850L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
        }

        val burstZoomY = ObjectAnimator.ofFloat(
            boosterBurst,
            View.SCALE_Y,
            1f,
            1.18f,
            1f
        ).apply {
            duration = 850L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
        }

        val burstAlpha = ObjectAnimator.ofFloat(
            boosterBurst,
            View.ALPHA,
            0.65f,
            1f,
            0.65f
        ).apply {
            duration = 850L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
        }

        // Pulsing double-tap prompt.
        boosterAlert.visibility = View.VISIBLE
        boosterAlert.alpha = 1f
        boosterAlert.scaleX = 1f
        boosterAlert.scaleY = 1f
        boosterAlert.translationX = 0f
        boosterAlert.translationY = 0f

        val alertPulse = ObjectAnimator.ofFloat(
            boosterAlert,
            View.ALPHA,
            1f,
            0.6f,
            1f
        ).apply {
            duration = 450L
            repeatCount = ValueAnimator.INFINITE
        }

        boosterAlertAnimator = AnimatorSet().apply {
            playTogether(
                burstZoomX,
                burstZoomY,
                burstAlpha,
                alertPulse
            )
            start()
        }
    }

    private fun hideBoosterAlert() {
        boosterAlertAnimator?.cancel()
        boosterAlertAnimator = null

        boosterAlert.animate().cancel()
        boosterBurst.animate().cancel()

        boosterAlert.visibility = View.GONE
        boosterAlert.alpha = 0f
        boosterAlert.scaleX = 1f
        boosterAlert.scaleY = 1f
        boosterAlert.translationX = 0f
        boosterAlert.translationY = 0f

        boosterBurst.visibility = View.GONE
        boosterBurst.alpha = 0f
        boosterBurst.scaleX = 1f
        boosterBurst.scaleY = 1f
        boosterBurst.translationX = 0f
        boosterBurst.translationY = 0f
    }

    override fun onBoosterActivated() {
        runOnUiThread {
            boosterIsAvailable = false
            hideBoosterAlert()
        }
    }

    // -------------------------------------------------------------------------
    // Mystery reward cinematic
    // -------------------------------------------------------------------------


    override fun onMysteryRewardCollected() {
        runOnUiThread {
            mysteryAnimator?.cancel()
            mysteryAnimator = null

            hideBoosterAlert()

            mysteryFrames.forEach { frame ->
                frame.animate().cancel()
                frame.visibility = View.GONE
                frame.alpha = 0f
                frame.scaleX = 1f
                frame.scaleY = 1f
            }

            val stages = mutableListOf<Animator>()

            // Duration for each frame.
            val durations = longArrayOf(
                130L,
                150L,
                180L,
                240L
            )

            mysteryFrames.forEachIndexed { index, frame ->
                val startScale = when (index) {
                    0 -> 0.35f
                    1 -> 0.65f
                    2 -> 1f
                    else -> 1.5f
                }

                val fadeIn = ObjectAnimator.ofFloat(
                    frame,
                    View.ALPHA,
                    0f,
                    1f
                ).apply {
                    duration = 45L
                }

                val zoom = ObjectAnimator.ofPropertyValuesHolder(
                    frame,
                    PropertyValuesHolder.ofFloat(
                        View.SCALE_X,
                        startScale,
                        4f
                    ),
                    PropertyValuesHolder.ofFloat(
                        View.SCALE_Y,
                        startScale,
                        4f
                    )
                ).apply {
                    duration = durations[index]
                }

                val stage = AnimatorSet().apply {
                    playTogether(fadeIn, zoom)

                    addListener(
                        object : AnimatorListenerAdapter() {
                            override fun onAnimationStart(
                                animation: Animator
                            ) {
                                if (index > 0) {
                                    mysteryFrames[index - 1]
                                        .visibility = View.GONE
                                }

                                frame.visibility = View.VISIBLE
                            }
                        }
                    )
                }

                stages.add(stage)
            }

            // The final frame stays enlarged while it fades completely out.
            val finalFrame = mysteryFrames.last()

            val fadeOut = ObjectAnimator.ofFloat(
                finalFrame,
                View.ALPHA,
                1f,
                0f
            ).apply {
                duration = 300L
            }

            val sequence = AnimatorSet()
            sequence.playSequentially(stages + fadeOut)

            sequence.addListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        resetMysteryFrames()
                        mysteryAnimator = null
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        resetMysteryFrames()
                    }
                }
            )

            mysteryAnimator = sequence
            sequence.start()
        }
    }

    private fun resetMysteryFrames() {
        mysteryFrames.forEach { frame ->
            frame.animate().cancel()
            frame.visibility = View.GONE
            frame.alpha = 0f
            frame.scaleX = 1f
            frame.scaleY = 1f
        }
    }


    // -------------------------------------------------------------------------
    // Activity lifecycle
    // -------------------------------------------------------------------------

    override fun onPause() {
        super.onPause()

        if (::gameView.isInitialized) {
            gameView.pauseGame()
        }

        boosterAlertAnimator?.pause()
        mysteryAnimator?.pause()
    }

    override fun onResume() {
        super.onResume()

        if (
            ::gameView.isInitialized &&
            gameView.state == GameState.PAUSED &&
            !pauseScreenOpen
        ) {
            openPauseScreen()
        }

        if (
            boosterIsAvailable &&
            ::gameView.isInitialized &&
            gameView.state == GameState.PLAYING
        ) {
            showBoosterAlert()
        }
    }

    override fun onDestroy() {
        boosterAlertAnimator?.cancel()
        boosterAlertAnimator = null

        mysteryAnimator?.cancel()
        mysteryAnimator = null

        if (::gameView.isInitialized) {
            gameView.release()
        }

        super.onDestroy()
    }

    // -------------------------------------------------------------------------
    // GameView.Listener
    // -------------------------------------------------------------------------

    override fun onPauseRequested() = runOnUiThread {
        gameView.pauseGame()

        if (!pauseScreenOpen) {
            openPauseScreen()
        }
    }

    override fun onGameOver(
        score: Int,
        distanceMeters: Int,
        coins: Int,
        mysteryCollected: Boolean,
        mysteryType: String,
        mysteryAmount: Int
    ) = runOnUiThread {
        if (gameOverHandled) return@runOnUiThread

        gameOverHandled = true
        boosterIsAvailable = false

        hideBoosterAlert()

        mysteryAnimator?.cancel()
        mysteryAnimator = null

        mysteryFrames.forEach { frame ->
            frame.visibility = View.GONE
            frame.alpha = 0f
        }

        val pd = app.playerData

// Add this completed run's score to career progression.
// gameOverHandled prevents duplicate additions from repeated callbacks.
        pd.addCareerScore(score)

// Bank ordinary run coins exactly once.
        pd.addCoins(coins)

        // Grant the mystery reward exactly once.
        if (mysteryCollected) {
            when (mysteryType) {
                "coins" -> pd.addCoins(mysteryAmount)
                "rubies" -> pd.addRushRubies(mysteryAmount)
            }
        }
        // Update mission progress and grant newly earned mission rewards.
        MissionSystem(app.prefs, pd).recordCompletedRun(
            score = score,
            distanceMeters = distanceMeters,
            coins = coins
        )

        if (score > pd.highScore) {
            pd.highScore = score
        }

        startActivity(
            Intent(this, GameOverActivity::class.java)
                .putExtra(Constants.EXTRA_SCORE, score)
                .putExtra(Constants.EXTRA_DISTANCE, distanceMeters)
                .putExtra(Constants.EXTRA_COINS, coins)
                .putExtra(Constants.EXTRA_HIGH_SCORE, pd.highScore)
                .putExtra("mystery_collected", mysteryCollected)
                .putExtra("mystery_type", mysteryType)
                .putExtra("mystery_amount", mysteryAmount)
        )

        finish()
    }

    // -------------------------------------------------------------------------
    // Pause menu and navigation
    // -------------------------------------------------------------------------

    private fun openPauseScreen() {
        pauseScreenOpen = true

        startActivityForResult(
            Intent(this, PauseActivity::class.java),
            REQ_PAUSE
        )
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode != REQ_PAUSE) return

        pauseScreenOpen = false

        when (resultCode) {
            PauseActivity.RESULT_HOME -> {
                goHome()
            }

            PauseActivity.RESULT_RESTART -> {
                gameOverHandled = false
                boosterIsAvailable = false

                hideBoosterAlert()

                mysteryAnimator?.cancel()
                mysteryAnimator = null

                mysteryFrames.forEach { frame ->
                    frame.visibility = View.GONE
                    frame.alpha = 0f
                    frame.scaleX = 1f
                    frame.scaleY = 1f
                }

                gameView.restartGame()
            }

            else -> {
                gameView.resumeGame()
            }
        }
    }

    override fun onBackPressed() {
        gameView.pauseGame()

        if (!pauseScreenOpen) {
            openPauseScreen()
        }
    }

    private fun goHome() {
        startActivity(
            Intent(
                this,
                MainMenuActivity::class.java
            ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )

        finish()
    }

    private companion object {
        const val REQ_PAUSE = 1
    }
}
