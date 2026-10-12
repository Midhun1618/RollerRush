package com.voxcom.rollerrush.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.voxcom.rollerrush.R
import com.voxcom.rollerrush.data.GamePreferences
import com.voxcom.rollerrush.data.InventoryData
import com.voxcom.rollerrush.data.PlayerData
import com.voxcom.rollerrush.data.ShopCatalog
import com.voxcom.rollerrush.data.ShopItem
import com.voxcom.rollerrush.data.SkateStats

class ShopActivity : AppCompatActivity() {

    private lateinit var playerData: PlayerData
    private lateinit var inventory: InventoryData

    private lateinit var root: View
    private lateinit var coinsLabel: TextView

    private lateinit var skateImage: ImageView
    private lateinit var particleImage: ImageView

    private lateinit var skateName: TextView
    private lateinit var skateDescription: TextView

    private lateinit var speedValue: TextView
    private lateinit var jumpValue: TextView
    private lateinit var controlValue: TextView

    private lateinit var actionButton: TextView

    private lateinit var skateSelector: HorizontalScrollView

    private var selectedId = "skate_basic"

    private val skateIds = listOf(
        "skate_basic",
        "skate_ice",
        "skate_aero",
        "skate_inferno",
        "skate_volt"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_shop)

        val prefs = GamePreferences(this)

        playerData = PlayerData(prefs)
        inventory = InventoryData(prefs)

        root = findViewById(R.id.shop_root)
        coinsLabel = findViewById(R.id.coins_label)

        skateImage = findViewById(R.id.shop_skate_image)
        particleImage = findViewById(R.id.shop_particle_image)

        skateName = findViewById(R.id.shop_skate_name)
        skateDescription = findViewById(R.id.shop_skate_description)

        speedValue = findViewById(R.id.shop_speed_value)
        jumpValue = findViewById(R.id.shop_jump_value)
        controlValue = findViewById(R.id.shop_control_value)

        actionButton = findViewById(R.id.shop_action_button)

        skateSelector = findViewById(R.id.shop_skate_selector)

        findViewById<View>(R.id.shop_back_button).setOnClickListener {
            finish()
        }

        setupSelector()
        setupActionButton()

        selectedId = playerData.equippedSkateId

        if (selectedId !in skateIds) {
            selectedId = "skate_basic"
        }

        updateCoins()
        showSkate(selectedId, true)
    }

    private fun setupSelector() {

        val selectorIds = listOf(
            R.id.selector_red to "skate_basic",
            R.id.selector_ice to "skate_ice",
            R.id.selector_aero to "skate_aero",
            R.id.selector_inferno to "skate_inferno",
            R.id.selector_volt to "skate_volt"
        )

        val imageIds = listOf(
            R.id.selector_red_image,
            R.id.selector_ice_image,
            R.id.selector_aero_image,
            R.id.selector_inferno_image,
            R.id.selector_volt_image
        )

        selectorIds.forEachIndexed { index, (viewId, id) ->

            val view = findViewById<View>(viewId)

            val image =
                findViewById<ImageView>(imageIds[index])

            image.setImageBitmap(
                loadSkateBitmap(id)
            )

            view.setOnClickListener {
                selectSkate(id)
            }
        }
    }

    private fun setupActionButton() {

        actionButton.setOnClickListener {

            val item = ShopCatalog.skate(selectedId)
                ?: return@setOnClickListener

            if (playerData.equippedSkateId == selectedId) {
                return@setOnClickListener
            }

            if (inventory.owns(selectedId)) {

                playerData.equippedSkateId = selectedId

                updateShop()

                playSelectionExplosion(
                    selectedId,
                    true
                )

                return@setOnClickListener
            }

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

            updateCoins()
            updateShop()

            playSelectionExplosion(
                selectedId,
                true
            )
        }
    }

    private fun selectSkate(id: String) {

        if (id !in skateIds) {
            return
        }

        val changed =
            selectedId != id

        selectedId = id

        showSkate(
            id,
            false
        )

        if (changed) {
            playSelectionExplosion(
                id,
                false
            )
        }
    }

    private fun showSkate(
        id: String,
        initial: Boolean
    ) {

        selectedId = id

        val item =
            ShopCatalog.skate(id)
                ?: return

        val stats =
            SkateStats.forSkate(id)

        skateImage.setImageBitmap(
            loadSkateBitmap(id)
        )

        particleImage.setImageBitmap(
            loadParticleBitmap(id)
        )

        skateName.text =
            item.name.uppercase()

        skateDescription.text =
            item.description

        speedValue.text =
            buildStatText(
                stats.speedLevel,
                5
            )

        jumpValue.text =
            buildStatText(
                stats.jumpLevel,
                3
            )

        controlValue.text =
            buildStatText(
                stats.controlLevel,
                3
            )

        updateActionButton()

        applyTheme(id)

        updateSelector()

        if (initial) {
            particleImage.alpha = 0f
        }
    }

    private fun updateShop() {

        updateCoins()

        updateActionButton()

        updateSelector()

        applyTheme(selectedId)
    }

    private fun updateCoins() {

        coinsLabel.text =
            "●  ${playerData.coins}"

        coinsLabel.setTextColor(
            Color.rgb(
                255,
                211,
                75
            )
        )
    }

    private fun updateActionButton() {

        val item =
            ShopCatalog.skate(selectedId)
                ?: return

        val owned =
            inventory.owns(selectedId)

        val equipped =
            playerData.equippedSkateId == selectedId

        when {

            equipped -> {
                actionButton.text = "EQUIPPED"
                actionButton.alpha = 0.55f
            }

            owned -> {
                actionButton.text = "EQUIP"
                actionButton.alpha = 1f
            }

            else -> {
                actionButton.text =
                    "UNLOCK   •   ${item.price}"
                actionButton.alpha = 1f
            }
        }
    }

    private fun updateSelector() {

        val selectorIds = listOf(
            R.id.selector_red to "skate_basic",
            R.id.selector_ice to "skate_ice",
            R.id.selector_aero to "skate_aero",
            R.id.selector_inferno to "skate_inferno",
            R.id.selector_volt to "skate_volt"
        )

        selectorIds.forEach { (viewId, id) ->

            val view =
                findViewById<View>(viewId)

            val selected =
                id == selectedId

            view.animate()
                .scaleX(
                    if (selected) 1.12f else 1f
                )
                .scaleY(
                    if (selected) 1.12f else 1f
                )
                .alpha(
                    if (selected) 1f else 0.62f
                )
                .setDuration(160)
                .start()
        }
    }

    private fun buildStatText(
        level: Int,
        max: Int
    ): String {

        val result = StringBuilder()

        for (i in 1..max) {

            if (i <= level) {
                result.append("●")
            } else {
                result.append("○")
            }

            if (i < max) {
                result.append(" ")
            }
        }

        return result.toString()
    }

    private fun applyTheme(id: String) {

        val colors = when (id) {

            "skate_ice" -> intArrayOf(
                Color.rgb(13, 38, 52),
                Color.rgb(33, 105, 125)
            )

            "skate_aero" -> intArrayOf(
                Color.rgb(11, 32, 50),
                Color.rgb(31, 89, 125)
            )

            "skate_inferno" -> intArrayOf(
                Color.rgb(48, 15, 17),
                Color.rgb(115, 43, 23)
            )

            "skate_volt" -> intArrayOf(
                Color.rgb(23, 17, 48),
                Color.rgb(67, 39, 103)
            )

            else -> intArrayOf(
                Color.rgb(43, 20, 24),
                Color.rgb(100, 43, 39)
            )
        }

        root.background =
            GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                colors
            )
    }

    private fun playSelectionExplosion(
        id: String,
        strong: Boolean
    ) {

        particleImage.setImageBitmap(
            loadParticleBitmap(id)
        )

        particleImage.visibility =
            View.VISIBLE

        particleImage.alpha = 0f
        particleImage.scaleX =
            if (strong) 0.25f else 0.35f
        particleImage.scaleY =
            if (strong) 0.25f else 0.35f
        particleImage.rotation = 0f

        skateImage.scaleX = 0.92f
        skateImage.scaleY = 0.92f
        skateImage.alpha = 0.82f

        val particleIn =
            ObjectAnimator.ofFloat(
                particleImage,
                View.ALPHA,
                0f,
                1f
            )

        val particleScaleX =
            ObjectAnimator.ofFloat(
                particleImage,
                View.SCALE_X,
                if (strong) 0.25f else 0.35f,
                1.18f
            )

        val particleScaleY =
            ObjectAnimator.ofFloat(
                particleImage,
                View.SCALE_Y,
                if (strong) 0.25f else 0.35f,
                1.18f
            )

        val particleRotate =
            ObjectAnimator.ofFloat(
                particleImage,
                View.ROTATION,
                0f,
                if (strong) 20f else 12f
            )

        val particleOut =
            ObjectAnimator.ofFloat(
                particleImage,
                View.ALPHA,
                1f,
                0f
            )

        val skateScaleX =
            ObjectAnimator.ofFloat(
                skateImage,
                View.SCALE_X,
                0.92f,
                1.04f,
                1f
            )

        val skateScaleY =
            ObjectAnimator.ofFloat(
                skateImage,
                View.SCALE_Y,
                0.92f,
                1.04f,
                1f
            )

        val skateAlpha =
            ObjectAnimator.ofFloat(
                skateImage,
                View.ALPHA,
                0.82f,
                1f
            )

        val explosionIn =
            AnimatorSet().apply {

                playTogether(
                    particleIn,
                    particleScaleX,
                    particleScaleY,
                    particleRotate,
                    skateScaleX,
                    skateScaleY,
                    skateAlpha
                )

                duration =
                    if (strong) 420L else 300L

                interpolator =
                    DecelerateInterpolator()
            }

        val explosionOut =
            AnimatorSet().apply {

                playTogether(
                    particleOut
                )

                duration = 380L

                interpolator =
                    DecelerateInterpolator()
            }

        explosionIn.start()

        explosionIn.addListener(
            object :
                android.animation.AnimatorListenerAdapter() {

                override fun onAnimationEnd(
                    animation: android.animation.Animator
                ) {
                    explosionOut.start()
                }
            }
        )
    }

    private fun loadSkateBitmap(
        id: String
    ) =
        assets.open(
            "shop/skates/" +
                    when (id) {

                        "skate_ice" ->
                            "skate2.png"

                        "skate_aero" ->
                            "skate3.png"

                        "skate_inferno" ->
                            "skate4.png"

                        "skate_volt" ->
                            "skate5.png"

                        else ->
                            "skate1.png"
                    }
        ).use {
            BitmapFactory.decodeStream(it)
        }

    private fun loadParticleBitmap(
        id: String
    ) =
        assets.open(
            "shop/particles/" +
                    when (id) {

                        "skate_ice" ->
                            "particle_ice.png"

                        "skate_aero" ->
                            "particle_aero.png"

                        "skate_inferno" ->
                            "particle_inferno.png"

                        "skate_volt" ->
                            "particle_volt.png"

                        else ->
                            "particle_red.png"
                    }
        ).use {
            BitmapFactory.decodeStream(it)
        }
}