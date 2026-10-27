package com.voxcom.rollerrush.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.voxcom.rollerrush.R
import com.voxcom.rollerrush.utils.Constants

class GameOverActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_over)

        val score = intent.getIntExtra(Constants.EXTRA_SCORE, 0)
        val distance = intent.getIntExtra(Constants.EXTRA_DISTANCE, 0)
        val coins = intent.getIntExtra(Constants.EXTRA_COINS, 0)
        val best = intent.getIntExtra(Constants.EXTRA_HIGH_SCORE, 0)

        val mysteryCollected =
            intent.getBooleanExtra("mystery_collected", false)
        val mysteryType =
            intent.getStringExtra("mystery_type").orEmpty()
        val mysteryAmount =
            intent.getIntExtra("mystery_amount", 0)

        findViewById<TextView>(R.id.distanceValue).text =
            "$distance m"

        findViewById<TextView>(R.id.scoreValue).text =
            score.toString()

        findViewById<TextView>(R.id.bestValue).text =
            best.toString()

        findViewById<TextView>(R.id.totalCoinsValue).text =
            "${app.playerData.coins}"

        findViewById<TextView>(R.id.runCoinsValue).text =
            "+$coins COINS THIS RUN"

        // Reveal the mystery reward only when one was collected.
        val rewardCard =
            findViewById<View>(R.id.mysteryRewardCard)

        val rewardValue =
            findViewById<TextView>(R.id.mysteryRewardValue)

        if (mysteryCollected && mysteryAmount > 0) {
            rewardCard.visibility = View.VISIBLE

            rewardValue.text = when (mysteryType) {
                "coins" -> "+$mysteryAmount COINS"
                "rubies" -> "+$mysteryAmount RUBIES"
                else -> "REWARD COLLECTED!"
            }

            rewardValue.setTextColor(
                if (mysteryType == "rubies") {
                    0xFFFF6F91.toInt()
                } else {
                    0xFFFFD66B.toInt()
                }
            )

            // Bubble pop animation.
            val bubble =
                findViewById<ImageView>(R.id.mysteryBubbleImage)

            bubble.alpha = 0f
            bubble.scaleX = 0.35f
            bubble.scaleY = 0.35f

            bubble.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(550)
                .setInterpolator(OvershootInterpolator(2.2f))
                .start()

            // Reward card entrance.
            rewardCard.alpha = 0f
            rewardCard.translationY = 24f

            rewardCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(180)
                .setDuration(420)
                .start()
        } else {
            rewardCard.visibility = View.GONE
        }

        val title = findViewById<ImageView>(R.id.gameOverTitle)

        title.alpha = 0f
        title.translationY = -18f

        title.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(450)
            .start()

        findViewById<Button>(R.id.playAgainButton)
            .setOnClickListener {
                startActivity(
                    Intent(this, GameActivity::class.java)
                )
                finish()
            }

        findViewById<Button>(R.id.homeButton)
            .setOnClickListener {
                goHome()
            }
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onBackPressed() {
        goHome()
    }

    private fun goHome() {
        startActivity(
            Intent(this, MainMenuActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        finish()
    }
}
