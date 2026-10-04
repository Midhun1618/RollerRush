package com.voxcom.rollerrush.ui

import android.os.Bundle
import android.widget.CheckBox
import android.widget.Toast

class SettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sound = CheckBox(this).apply {
            text = "Sound (placeholder - no audio yet)"; textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            isChecked = app.prefs.soundEnabled
            setOnCheckedChangeListener { _, v -> app.prefs.soundEnabled = v }
        }
        val debug = CheckBox(this).apply {
            text = "Show hitboxes (debug)"; textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            isChecked = app.prefs.debugHitboxes
            setOnCheckedChangeListener { _, v -> app.prefs.debugHitboxes = v }
        }
        setContentView(UiKit.screen(
            this,
            UiKit.title(this, "SETTINGS", 28f),
            sound, debug,
            UiKit.button(this, "RESET PROGRESS", 240) {
                app.playerData.resetAll()
                Toast.makeText(this, "Progress reset", Toast.LENGTH_SHORT).show()
            },
            UiKit.button(this, "BACK", 160) { finish() }
        ))
    }
}
