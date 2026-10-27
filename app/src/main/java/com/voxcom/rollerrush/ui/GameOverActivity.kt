package com.voxcom.rollerrush.ui

import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import com.voxcom.rollerrush.R
import com.voxcom.rollerrush.utils.Constants

class GameOverActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        val score = intent.getIntExtra(Constants.EXTRA_SCORE, 0)
        val dist = intent.getIntExtra(Constants.EXTRA_DISTANCE, 0)
        val coins = intent.getIntExtra(Constants.EXTRA_COINS, 0)
        val best = intent.getIntExtra(Constants.EXTRA_HIGH_SCORE, 0)

        val mysteryCollected = intent.getBooleanExtra(
            "mystery_collected",
            false
        )
        val mysteryType = intent.getStringExtra("mystery_type").orEmpty()
        val mysteryAmount = intent.getIntExtra("mystery_amount", 0)

        val views = mutableListOf<android.view.View>(
            UiKit.title(this, "GAME OVER", 40f),
            UiKit.label(this, "Distance: $dist m", 20f),
            UiKit.label(this, "Coins collected: $coins", 20f),
            UiKit.label(this, "Score: $score    (Best: $best)", 20f),
            UiKit.label(
                this,
                "Total coins: ${app.playerData.coins}",
                16f
            )
        )

        if (mysteryCollected && mysteryAmount > 0) {
            val mysterySection = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(12, 12, 12, 12)
                setBackgroundColor(Color.rgb(42, 48, 83))
            }

            val bubbleImage = ImageView(this).apply {
                setImageResource(R.drawable.mystery_bubble)
                contentDescription = "Mystery reward"
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
            }

            mysterySection.addView(
                bubbleImage,
                LinearLayout.LayoutParams(180, 180)
            )

            mysterySection.addView(
                UiKit.title(this, "MYSTERY REWARD!", 24f)
            )

            val rewardText = when (mysteryType) {
                "coins" -> "+$mysteryAmount COINS"
                "rubies" -> "+$mysteryAmount RUBIES"
                else -> "Reward collected!"
            }

            mysterySection.addView(
                UiKit.label(this, rewardText, 24f)
            )

            views.add(mysterySection)
        }

        views.add(
            UiKit.button(this, "PLAY AGAIN") {
                startActivity(Intent(this, GameActivity::class.java))
                finish()
            }
        )

        views.add(
            UiKit.button(this, "HOME") {
                goHome()
            }
        )

        setContentView(UiKit.screen(this, *views.toTypedArray()))
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onBackPressed() = goHome()

    private fun goHome() {
        startActivity(
            Intent(this, MainMenuActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        finish()
    }
}
