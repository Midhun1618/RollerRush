package com.voxcom.rollerrush.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView

class MainMenuActivity : BaseActivity() {
    private lateinit var coinsLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        coinsLabel = UiKit.label(this, "")
        setContentView(UiKit.screen(
            this,
            UiKit.title(this, "ROLLER RUSH", 40f),
            coinsLabel,
            UiKit.button(this, "PLAY") { startActivity(Intent(this, GameActivity::class.java)) },
            UiKit.button(this, "CHARACTER") { startActivity(Intent(this, CharacterActivity::class.java)) },
            UiKit.button(this, "SHOP") { startActivity(Intent(this, ShopActivity::class.java)) },
            UiKit.button(this, "SETTINGS") { startActivity(Intent(this, SettingsActivity::class.java)) }
        ))
    }

    override fun onResume() {
        super.onResume()
        coinsLabel.text = "Coins: ${app.playerData.coins}    Best: ${app.playerData.highScore}"
    }
}
