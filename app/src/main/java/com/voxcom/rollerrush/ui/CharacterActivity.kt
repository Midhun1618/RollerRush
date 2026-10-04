package com.voxcom.rollerrush.ui

import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.voxcom.rollerrush.data.SkinCatalog
import com.voxcom.rollerrush.utils.ScreenUtils

class CharacterActivity : BaseActivity() {
    private lateinit var preview: PlayerPreviewView
    private lateinit var info: TextView
    private var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        index = app.playerData.characterPreset
        preview = PlayerPreviewView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ScreenUtils.dp(this@CharacterActivity, 220), ScreenUtils.dp(this@CharacterActivity, 170))
        }
        info = UiKit.label(this, "")
        val buttons = UiKit.row(
            this,
            UiKit.button(this, "PREVIOUS", 110) { select(index - 1) },
            UiKit.button(this, "EQUIP", 110) { equip() },
            UiKit.button(this, "NEXT", 110) { select(index + 1) }
        )
        setContentView(UiKit.screen(
            this,
            UiKit.title(this, "CHARACTER", 28f),
            preview, info, buttons,
            UiKit.button(this, "BACK", 160) { finish() }
        ))
        select(index)
    }

    private fun select(newIndex: Int) {
        val n = SkinCatalog.presets.size
        index = ((newIndex % n) + n) % n
        preview.setCharacter(SkinCatalog.presets[index])
        refreshInfo()
    }

    private fun refreshInfo() {
        val owned = app.inventory.owns(SkinCatalog.presetItemIds[index])
        val equipped = index == app.playerData.characterPreset
        info.text = "Skin ${index + 1}/${SkinCatalog.presets.size}  -  " +
            when { equipped -> "EQUIPPED"; owned -> "Owned"; else -> "Locked (buy in Shop)" }
    }

    private fun equip() {
        if (app.inventory.owns(SkinCatalog.presetItemIds[index])) {
            app.playerData.characterPreset = index
            refreshInfo()
        } else {
            Toast.makeText(this, "Locked - buy it in the Shop first", Toast.LENGTH_SHORT).show()
        }
    }
}
