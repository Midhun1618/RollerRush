
package com.voxcom.rollerrush.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import com.voxcom.rollerrush.R

class MainMenuActivity : BaseActivity() {

    private lateinit var coinsLabel: TextView
    private lateinit var rushRubiesLabel: TextView
    private lateinit var highScoreLabel: TextView
    private lateinit var playButton: ImageView
    private lateinit var characterButton: ImageView
    private lateinit var shopButton: ImageView
    private lateinit var rushTitle: ImageView
    private lateinit var settingsButton: ImageView

    private var titleFrameIndex = 0
    private var titleAnimator: ValueAnimator? = null
    private var playPulseAnimator: AnimatorSet? = null

    private val titleFrames by lazy {
        intArrayOf(
            R.drawable.rush_title1,
            R.drawable.rush_title2,
            R.drawable.rush_title3,
            R.drawable.rush_title4
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        coinsLabel = findViewById(R.id.coinsLabel)
        rushRubiesLabel = findViewById(R.id.rubiesLabel)
        highScoreLabel = findViewById(R.id.scoreLabel)
        playButton = findViewById(R.id.playButton)
        characterButton = findViewById(R.id.characterButton)
        shopButton = findViewById(R.id.shopButton)
        rushTitle = findViewById(R.id.rushTitle)
        settingsButton = findViewById(R.id.settingsButton)

        playButton.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }

        characterButton.setOnClickListener {
            startActivity(Intent(this, CharacterActivity::class.java))
        }

        shopButton.setOnClickListener {
            startActivity(Intent(this, ShopActivity::class.java))
        }

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Start animations after the views are ready.
        startPlayButtonPulse()
        startTitleAnimation()
    }

    private fun startPlayButtonPulse() {
        playPulseAnimator?.cancel()

        val zoomInX = ObjectAnimator.ofFloat(
            playButton, "scaleX", 1f, 1.05f
        ).apply {
            duration = 650L
            repeatCount = 1
            repeatMode = ValueAnimator.REVERSE
        }

        val zoomInY = ObjectAnimator.ofFloat(
            playButton, "scaleY", 1f, 1.05f
        ).apply {
            duration = 650L
            repeatCount = 1
            repeatMode = ValueAnimator.REVERSE
        }

        playPulseAnimator = AnimatorSet().apply {
            playTogether(zoomInX, zoomInY)
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!isFinishing && !isDestroyed) {
                        playButton.postDelayed({
                            if (!isFinishing && !isDestroyed) {
                                startPlayButtonPulse()
                            }
                        }, 250L)
                    }
                }
            })
            start()
        }
    }

    private fun startTitleAnimation() {
        titleAnimator?.cancel()

        titleFrameIndex = 0
        rushTitle.setImageResource(titleFrames[titleFrameIndex])

        // Show each title frame for 140 ms, then advance in sequence.
        titleAnimator = ValueAnimator.ofInt(0, titleFrames.size - 1).apply {
            duration = 420L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = android.view.animation.LinearInterpolator()

            addUpdateListener { animator ->
                val frame = animator.animatedValue as Int

                if (frame != titleFrameIndex) {
                    titleFrameIndex = frame
                    rushTitle.setImageResource(titleFrames[frame])
                }
            }

            start()
        }

        // A separate, subtle pulse gives the title a little energy.
        rushTitle.animate()
            .scaleX(1.04f)
            .scaleY(1.04f)
            .setDuration(650L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                if (!isFinishing && !isDestroyed) {
                    rushTitle.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(650L)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .withEndAction {
                            if (!isFinishing && !isDestroyed) {
                                startTitlePulseAgain()
                            }
                        }
                        .start()
                }
            }
            .start()
    }

    private fun startTitlePulseAgain() {
        if (isFinishing || isDestroyed) return

        rushTitle.animate()
            .scaleX(1.01f)
            .scaleY(1.01f)
            .setDuration(650L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                if (!isFinishing && !isDestroyed) {
                    rushTitle.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(650L)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .withEndAction {
                            startTitlePulseAgain()
                        }
                        .start()
                }
            }
            .start()
    }

    override fun onResume() {
        super.onResume()

        coinsLabel.text = "${app.playerData.coins}"
        highScoreLabel.text = "${app.playerData.highScore}"
        rushRubiesLabel.text = "${app.playerData.rushRubies}"
    }

    override fun onDestroy() {
        playPulseAnimator?.cancel()
        playPulseAnimator = null

        titleAnimator?.cancel()
        titleAnimator = null

        rushTitle.animate().cancel()
        playButton.animate().cancel()

        super.onDestroy()
    }
}
