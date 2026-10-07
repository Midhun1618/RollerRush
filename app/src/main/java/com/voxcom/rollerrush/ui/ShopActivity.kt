package com.voxcom.rollerrush.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.voxcom.rollerrush.R
import com.voxcom.rollerrush.data.ShopCatalog
import com.voxcom.rollerrush.data.ShopItem
import com.voxcom.rollerrush.data.ShopItemType
import com.voxcom.rollerrush.data.SkinCatalog
import kotlin.math.min

/**
 * XML-based landscape shop.
 *
 * The screen layout is completely defined in res/layout/activity_shop.xml.
 * Kotlin only handles shop state, clicks and the purchase celebration animation.
 */
class ShopActivity : BaseActivity() {

    private lateinit var coinsLabel: TextView

    private data class SkateViewRefs(
        val itemId: String,
        val button: TextView
    )

    private val skateViews = mutableListOf<SkateViewRefs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Roller Rush is a landscape-only game. Keep the shop in the same orientation.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.activity_shop)

        coinsLabel = findViewById(R.id.coins_label)

        bindSkate("skate_basic", R.id.skate_basic_button)
        bindSkate("skate_ice", R.id.skate_ice_button)
        bindSkate("skate_aero", R.id.skate_aero_button)
        bindSkate("skate_inferno", R.id.skate_inferno_button)
        bindSkate("skate_volt", R.id.skate_volt_button)

        findViewById<TextView>(R.id.character_basic_button).setOnClickListener {
            ShopCatalog.items.firstOrNull { it.id == "char_basic" }?.let(::onItemTapped)
        }
        findViewById<TextView>(R.id.character_2_button).setOnClickListener {
            ShopCatalog.items.firstOrNull { it.id == "char_skin_2" }?.let(::onItemTapped)
        }

        findViewById<TextView>(R.id.back_button).setOnClickListener { finish() }

        refresh()
    }

    private fun bindSkate(itemId: String, buttonId: Int) {
        val button = findViewById<TextView>(buttonId)
        button.setOnClickListener {
            ShopCatalog.skate(itemId)?.let(::onItemTapped)
        }
        skateViews += SkateViewRefs(itemId, button)
    }

    private fun refresh() {
        coinsLabel.text = "COINS  ${app.playerData.coins}"

        skateViews.forEach { ref ->
            val item = ShopCatalog.skate(ref.itemId) ?: return@forEach
            ref.button.text = actionText(item)
            ref.button.isEnabled = !isEquipped(item)
        }

        updateCharacterButton(
            findViewById(R.id.character_basic_button),
            ShopCatalog.items.first { it.id == "char_basic" }
        )
        updateCharacterButton(
            findViewById(R.id.character_2_button),
            ShopCatalog.items.first { it.id == "char_skin_2" }
        )
    }

    private fun updateCharacterButton(button: TextView, item: ShopItem) {
        button.text = when {
            isEquipped(item) -> "${item.name.uppercase()}  •  EQUIPPED"
            app.inventory.owns(item.id) -> "${item.name.uppercase()}  •  EQUIP"
            else -> "${item.name.uppercase()}  •  ${item.price} COINS"
        }
        button.isEnabled = !isEquipped(item)
    }

    private fun isEquipped(item: ShopItem): Boolean = when (item.type) {
        ShopItemType.SKATE -> app.playerData.equippedSkateId == item.id
        ShopItemType.CHARACTER ->
            SkinCatalog.presetItemIds.indexOf(item.id) == app.playerData.characterPreset
    }

    private fun actionText(item: ShopItem): String = when {
        isEquipped(item) -> "EQUIPPED"
        app.inventory.owns(item.id) -> "EQUIP"
        item.price == 0 -> "GET"
        else -> "BUY  ${item.price} COINS"
    }

    private fun onItemTapped(item: ShopItem) {
        if (isEquipped(item)) return

        val wasOwned = app.inventory.owns(item.id)

        if (!wasOwned) {
            if (!app.playerData.spendCoins(item.price)) {
                Toast.makeText(this, "NOT ENOUGH COINS", Toast.LENGTH_SHORT).show()
                return
            }
            app.inventory.unlock(item.id)
        }

        when (item.type) {
            ShopItemType.SKATE -> app.playerData.equippedSkateId = item.id
            ShopItemType.CHARACTER -> {
                SkinCatalog.presetItemIds.indexOf(item.id)
                    .takeIf { it >= 0 }
                    ?.let { app.playerData.characterPreset = it }
            }
        }

        refresh()

        if (!wasOwned && item.type == ShopItemType.SKATE) {
            showPurchaseDialog(item)
        } else {
            Toast.makeText(this, "${item.name} EQUIPPED", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPurchaseDialog(item: ShopItem) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_purchase)
        dialog.setCanceledOnTouchOutside(true)

        val title = dialog.findViewById<TextView>(R.id.dialog_title)
        val itemName = dialog.findViewById<TextView>(R.id.dialog_item_name)
        val skateImage = dialog.findViewById<ImageView>(R.id.dialog_skate_image)
        val continueButton = dialog.findViewById<TextView>(R.id.dialog_continue)
        val particleField = dialog.findViewById<FrameLayout>(R.id.dialog_particle_field)

        title.text = "UNLOCKED!"
        itemName.text = item.name.uppercase()
        skateImage.setImageResource(skateDrawable(item.id))

        continueButton.setOnClickListener { dialog.dismiss() }

        dialog.setOnShowListener {
            val window = dialog.window ?: return@setOnShowListener
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.attributes = window.attributes.apply { dimAmount = 0.72f }

            val width = min(dp(560), resources.displayMetrics.widthPixels - dp(48))
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)

            val root = dialog.findViewById<View>(R.id.purchase_dialog_root)
            root.scaleX = 0.86f
            root.scaleY = 0.86f
            root.alpha = 0f
            root.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(220)
                .start()

            animateParticles(particleField, particleDrawable(item.id))
        }

        dialog.show()

        // Android applies the dialog window dimensions after show().
        dialog.window?.setLayout(
            min(dp(560), resources.displayMetrics.widthPixels - dp(48)),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun animateParticles(field: FrameLayout, drawableRes: Int) {
        // Supplied particle sprites are tiny. The ImageViews deliberately render
        // them larger so the celebration is clearly visible on the landscape dialog.
        val positions = arrayOf(
            intArrayOf(-105, -8),
            intArrayOf(-78, -48),
            intArrayOf(-38, -66),
            intArrayOf(10, -70),
            intArrayOf(55, -54),
            intArrayOf(95, -18),
            intArrayOf(-82, 38),
            intArrayOf(75, 42)
        )

        positions.forEachIndexed { index, pos ->
            val particle = ImageView(this).apply {
                setImageResource(drawableRes)
                scaleType = ImageView.ScaleType.FIT_CENTER
                alpha = 0f
            }

            val size = dp(42)
            val params = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
            field.addView(particle, params)

            val endX = dp(pos[0] )
            val endY = dp(pos[1] )

            particle.postDelayed({
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(particle, View.ALPHA, 0f, 1f, 0f),
                        ObjectAnimator.ofFloat(particle, View.SCALE_X, 0.25f, 1.25f, 0.55f),
                        ObjectAnimator.ofFloat(particle, View.SCALE_Y, 0.25f, 1.25f, 0.55f),
                        ObjectAnimator.ofFloat(particle, View.TRANSLATION_X, 0f, endX.toFloat()),
                        ObjectAnimator.ofFloat(particle, View.TRANSLATION_Y, 0f, endY.toFloat()),
                        ObjectAnimator.ofFloat(particle, View.ROTATION, -45f, 45f)
                    )
                    duration = 820
                    start()
                }
            }, index * 45L)
        }
    }

    private fun skateDrawable(id: String): Int = when (id) {
        "skate_basic" -> R.drawable.skate1
        "skate_ice" -> R.drawable.skate2
        "skate_aero" -> R.drawable.skate3
        "skate_inferno" -> R.drawable.skate4
        "skate_volt" -> R.drawable.skate5
        else -> R.drawable.skate1
    }

    private fun particleDrawable(id: String): Int = when (id) {
        "skate_basic" -> R.drawable.particle_red
        "skate_ice" -> R.drawable.particle_ice
        "skate_aero" -> R.drawable.particle_aero
        "skate_inferno" -> R.drawable.particle_inferno
        "skate_volt" -> R.drawable.particle_volt
        else -> R.drawable.particle_red
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
}
