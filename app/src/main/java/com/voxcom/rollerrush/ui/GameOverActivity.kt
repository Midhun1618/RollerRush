package com.voxcom.rollerrush.ui

import android.content.Intent
import android.os.Bundle
import com.voxcom.rollerrush.utils.Constants

class GameOverActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val score = intent.getIntExtra(Constants.EXTRA_SCORE, 0)
        val dist = intent.getIntExtra(Constants.EXTRA_DISTANCE, 0)
        val coins = intent.getIntExtra(Constants.EXTRA_COINS, 0)
        val best = intent.getIntExtra(Constants.EXTRA_HIGH_SCORE, 0)
        setContentView(UiKit.screen(
            this,
            UiKit.title(this, "GAME OVER", 40f),
            UiKit.label(this, "Distance: $dist m", 20f),
            UiKit.label(this, "Coins collected: $coins", 20f),
            UiKit.label(this, "Score: $score    (Best: $best)", 20f),
            UiKit.label(this, "Total coins: ${app.playerData.coins}", 16f),
            UiKit.button(this, "PLAY AGAIN") {
                startActivity(Intent(this, GameActivity::class.java))
                finish()
            },
            UiKit.button(this, "HOME") { goHome() }
        ))
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onBackPressed() = goHome()

    private fun goHome() {
        startActivity(Intent(this, MainMenuActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }
}
