package com.voxcom.rollerrush.ui

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import com.voxcom.rollerrush.data.ShopCatalog
import com.voxcom.rollerrush.data.ShopItem
import com.voxcom.rollerrush.data.ShopItemType
import com.voxcom.rollerrush.data.SkinCatalog

/** Placeholder shop: tap an item to buy it; tap an owned item to equip it. */
class ShopActivity : BaseActivity() {
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = android.view.Gravity.CENTER }
        setContentView(UiKit.screen(this, content))
        refresh()
    }

    private fun refresh() {
        content.removeAllViews()
        content.addView(UiKit.title(this, "SHOP", 28f))
        content.addView(UiKit.label(this, "Current Coins: ${app.playerData.coins}", 20f))
        for (item in ShopCatalog.items) {
            content.addView(UiKit.button(this, itemText(item), 300) { onItemTapped(item) })
        }
        content.addView(UiKit.button(this, "BACK", 160) { finish() })
    }

    private fun isEquipped(item: ShopItem) = when (item.type) {
        ShopItemType.SKATE -> app.playerData.equippedSkateId == item.id
        ShopItemType.CHARACTER -> SkinCatalog.presetItemIds.indexOf(item.id) == app.playerData.characterPreset
    }

    private fun itemText(item: ShopItem): String = when {
        isEquipped(item) -> "${item.name}  (EQUIPPED)"
        app.inventory.owns(item.id) -> "${item.name}  (Owned - tap to equip)"
        else -> "${item.name}  -  ${item.price} coins"
    }

    private fun onItemTapped(item: ShopItem) {
        if (!app.inventory.owns(item.id)) {
            if (!app.playerData.spendCoins(item.price)) {
                Toast.makeText(this, "Not enough coins", Toast.LENGTH_SHORT).show()
                return
            }
            app.inventory.unlock(item.id)
        }
        when (item.type) {                       // equip (also right after buying)
            ShopItemType.SKATE -> app.playerData.equippedSkateId = item.id
            ShopItemType.CHARACTER -> SkinCatalog.presetItemIds.indexOf(item.id)
                .takeIf { it >= 0 }?.let { app.playerData.characterPreset = it }
        }
        refresh()
    }
}
