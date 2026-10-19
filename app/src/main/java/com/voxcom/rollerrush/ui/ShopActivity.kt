
package com.voxcom.rollerrush.ui

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.voxcom.rollerrush.R
import com.voxcom.rollerrush.audio.GameSoundEffects
import com.voxcom.rollerrush.data.GamePreferences
import com.voxcom.rollerrush.data.InventoryData
import com.voxcom.rollerrush.data.PlayerData
import com.voxcom.rollerrush.data.ShopCatalog
import com.voxcom.rollerrush.data.SkateStats

class ShopActivity : AppCompatActivity() {

    /** One entry per skate card in the horizontal selector. */
    private class Slot(
        val id: String,
        val cardId: Int,
        val nameId: Int,
        val skateImageId: Int,
        val skateDrawable: Int,
        val textureDrawable: Int
    ) {
        lateinit var card: View
        lateinit var label: TextView
        lateinit var image: ImageView
    }

    private val slots = listOf(
        Slot(
            "skate_basic",
            R.id.selector_card1,
            R.id.selector_name1,
            R.id.selector_skate1,
            R.drawable.skate1,
            R.drawable.card_texture_bg_1
        ),
        Slot(
            "skate_ice",
            R.id.selector_card2,
            R.id.selector_name2,
            R.id.selector_skate2,
            R.drawable.skate2,
            R.drawable.card_texture_bg_2
        ),
        Slot(
            "skate_aero",
            R.id.selector_card3,
            R.id.selector_name3,
            R.id.selector_skate3,
            R.drawable.skate3,
            R.drawable.card_texture_bg_3
        ),
        Slot(
            "skate_inferno",
            R.id.selector_card4,
            R.id.selector_name4,
            R.id.selector_skate4,
            R.drawable.skate4,
            R.drawable.card_texture_bg_4
        ),
        Slot(
            "skate_volt",
            R.id.selector_card5,
            R.id.selector_name5,
            R.id.selector_skate5,
            R.drawable.skate5,
            R.drawable.card_texture_bg_5
        )
    )

    private lateinit var playerData: PlayerData
    private lateinit var inventory: InventoryData

    private lateinit var coinsLabel: TextView

    // Info popup
    private lateinit var overlay: View
    private lateinit var infoCard: View
    private lateinit var infoName: TextView
    private lateinit var infoSubtitle: TextView
    private lateinit var infoTexture: ImageView
    private lateinit var infoSkate: ImageView
    private lateinit var speedMeter: ProgressBar
    private lateinit var powerMeter: ProgressBar
    private lateinit var priceRow: View
    private lateinit var priceLabel: TextView
    private lateinit var actionButton: Button

    // Lazily initialized so sounds are allocated only when needed.
    private val gameSoundEffectsDelegate = lazy {
        GameSoundEffects(applicationContext)
    }

    private val gameSoundEffects by gameSoundEffectsDelegate

    private var selectedId = "skate_basic"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)

        val prefs = GamePreferences(this)
        playerData = PlayerData(prefs)
        inventory = InventoryData(prefs)

        bindViews()
        setupSelector()
        setupActionButton()
        setupOverlay()

        findViewById<View>(R.id.shop_back_button).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Back closes the popup first, then the screen.
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (overlay.isVisible) {
                        hideInfo()
                    } else {
                        finish()
                    }
                }
            }
        )

        selectedId = playerData.equippedSkateId
        if (slots.none { it.id == selectedId }) {
            selectedId = "skate_basic"
        }

        updateCoins()
        refreshCards()
    }

    // ---------------------------------------------------------------- setup

    private fun bindViews() {
        coinsLabel = findViewById(R.id.coins_label)

        overlay = findViewById(R.id.info_overlay)
        infoCard = findViewById(R.id.skate_info_card)
        infoName = findViewById(R.id.name_skate_info)
        infoSubtitle = findViewById(R.id.subtitle_skate_info)
        infoTexture = findViewById(R.id.bgcard_skateinfo)
        infoSkate = findViewById(R.id.skateimg_skateinfo)
        speedMeter = findViewById(R.id.speed_meter)
        powerMeter = findViewById(R.id.power_meter)
        priceRow = findViewById(R.id.linearLayout)
        priceLabel = findViewById(R.id.price_skate_info)
        actionButton = findViewById(R.id.buyBtn)

        slots.forEach {
            it.card = findViewById(it.cardId)
            it.label = findViewById(it.nameId)
            it.image = findViewById(it.skateImageId)
        }
    }

    private fun setupSelector() {
        slots.forEach { slot ->
            slot.image.setImageResource(slot.skateDrawable)
            slot.card.setOnClickListener {
                showInfo(slot.id)
            }
        }
    }

    private fun setupOverlay() {
        overlay.visibility = View.GONE

        // Tap outside the card closes the popup.
        overlay.setOnClickListener {
            hideInfo()
        }

        // Taps on the card itself must not fall through to the overlay.
        infoCard.setOnClickListener { }
    }

    private fun setupActionButton() {
        actionButton.setOnClickListener {
            val item = ShopCatalog.skate(selectedId)
                ?: return@setOnClickListener

            // Already equipped: do nothing and play no sound.
            if (playerData.equippedSkateId == selectedId) {
                return@setOnClickListener
            }

            if (inventory.owns(selectedId)) {
                // EQUIP an already-owned skate.
                playerData.equippedSkateId = selectedId

                gameSoundEffects.playSkateEquipped()
            } else {
                // BUY a locked skate.
                if (!playerData.spendCoins(item.price)) {
                    Toast.makeText(
                        this,
                        "Not enough coins",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                inventory.unlock(selectedId)
                playerData.equippedSkateId = selectedId

                // Play only after the purchase succeeds.
                gameSoundEffects.playSkatePurchased()
            }

            updateCoins()
            refreshCards()
            bindInfo(selectedId, animateMeters = false)
            playPop()
        }
    }

    // ------------------------------------------------------------ popup

    private fun showInfo(id: String) {
        selectedId = id
        bindInfo(id, animateMeters = true)

        overlay.alpha = 0f
        overlay.visibility = View.VISIBLE
        overlay.animate()
            .alpha(1f)
            .setDuration(180)
            .start()

        infoCard.scaleX = 0.9f
        infoCard.scaleY = 0.9f

        infoCard.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun hideInfo() {
        overlay.animate()
            .alpha(0f)
            .setDuration(140)
            .withEndAction {
                overlay.visibility = View.GONE
            }
            .start()
    }

    private fun bindInfo(id: String, animateMeters: Boolean) {
        val slot = slots.first { it.id == id }
        val item = ShopCatalog.skate(id) ?: return
        val stats = SkateStats.forSkate(id)

        val owned = inventory.owns(id)
        val equipped = playerData.equippedSkateId == id

        infoName.text = item.name.uppercase()
        infoSubtitle.text = item.description
        infoTexture.setImageResource(slot.textureDrawable)
        infoSkate.setImageResource(slot.skateDrawable)

        // Speed: level out of 5.
        // Power: average of jump + control (each out of 3).
        val speedPct = (stats.speedLevel / 5f * 100f).toInt()
        val powerPct =
            ((stats.jumpLevel + stats.controlLevel) / 6f * 100f).toInt()

        setMeter(speedMeter, speedPct, animateMeters)
        setMeter(powerMeter, powerPct, animateMeters)

        priceLabel.text = String.format("%,d", item.price)
        priceRow.visibility = if (owned) View.INVISIBLE else View.VISIBLE

        when {
            equipped -> {
                actionButton.text = "EQUIPPED"
                actionButton.isEnabled = false
                actionButton.alpha = 0.55f
            }

            owned -> {
                actionButton.text = "EQUIP"
                actionButton.isEnabled = true
                actionButton.alpha = 1f
            }

            else -> {
                actionButton.text = "BUY NOW"
                actionButton.isEnabled = true
                actionButton.alpha = 1f
            }
        }

        // Locked skates are shown as black silhouettes.
        if (owned) {
            infoSkate.clearColorFilter()
        } else {
            infoSkate.setColorFilter(Color.BLACK)
        }
    }

    private fun setMeter(
        bar: ProgressBar,
        value: Int,
        animate: Boolean
    ) {
        bar.max = 100

        if (animate) {
            ObjectAnimator.ofInt(bar, "progress", 0, value).apply {
                duration = 450
                interpolator = DecelerateInterpolator()
                start()
            }
        } else {
            bar.progress = value
        }
    }

    private fun playPop() {
        infoSkate.scaleX = 0.88f
        infoSkate.scaleY = 0.88f

        infoSkate.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    // --------------------------------------------------------- selector

    /** Refreshes each card's background, label, and silhouette. */
    private fun refreshCards() {
        val equippedId = playerData.equippedSkateId

        slots.forEach { slot ->
            val item = ShopCatalog.skate(slot.id)
            val owned = inventory.owns(slot.id)
            val equipped = slot.id == equippedId

            slot.card.setBackgroundResource(
                if (equipped) {
                    R.drawable.shop_equipped_item_card_bg
                } else {
                    R.drawable.shop_item_card_bg
                }
            )

            slot.label.text = when {
                equipped -> "EQUIPPED"
                owned -> item?.name?.uppercase() ?: ""
                else -> "LOCKED"
            }

            slot.label.setBackgroundResource(
                if (owned) 0 else R.drawable.grey_container
            )

            if (owned) {
                slot.image.clearColorFilter()
            } else {
                slot.image.setColorFilter(Color.BLACK)
            }
        }
    }

    private fun updateCoins() {
        // The coin icon is a separate ImageView in the layout.
        coinsLabel.text = String.format("%,d", playerData.coins)
    }

    override fun onDestroy() {
        // Release SoundPool only if it was initialized.
        if (gameSoundEffectsDelegate.isInitialized()) {
            gameSoundEffects.release()
        }

        super.onDestroy()
    }
}
