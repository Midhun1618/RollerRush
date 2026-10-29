package com.voxcom.rollerrush.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.voxcom.rollerrush.R
import com.voxcom.rollerrush.RollerRushApp
import com.voxcom.rollerrush.audio.GameSoundEffects
import com.voxcom.rollerrush.data.GamePreferences
import com.voxcom.rollerrush.data.InventoryData
import com.voxcom.rollerrush.data.PlayerData
import com.voxcom.rollerrush.data.ShopCatalog
import com.voxcom.rollerrush.data.ShopItem
import com.voxcom.rollerrush.data.ShopItemType
import com.voxcom.rollerrush.data.SkateRarity
import com.voxcom.rollerrush.data.SkateStats
import java.util.Locale
import kotlin.math.min

class ShopActivity : AppCompatActivity() {

    private lateinit var playerData: PlayerData
    private lateinit var inventory: InventoryData
    private lateinit var coinsLabel: TextView
    private lateinit var rubiesLabel: TextView
    private lateinit var shopContent: LinearLayout

    private val sounds by lazy {
        GameSoundEffects(applicationContext)
    }

    private enum class Category {
        SKATES,
        HELMETS,
        SUITS
    }

    private var selectedCategory = Category.SKATES

    /*
     * These names refer to Android drawable resources, not files inside assets/.
     * Android drawable resources can be PNG or WebP.
     */
    private val drawableByItemId = mapOf(
        "skate_basic" to "skate1",
        "skate_ice" to "skate2",
        "skate_aero" to "skate3",
        "skate_inferno" to "skate4",
        "skate_volt" to "skate5",
        "skate_sunset_rush" to "skate_sunset_rush",
        "skate_pixel_pop" to "skate_pixel_pop",
        "skate_night_shift" to "skate_night_shift",
        "skate_nitro_edge" to "skate_nitro_edge",
        "skate_sonic_wave" to "skate_sonic_wave",
        "skate_gold_standard" to "skate_gold_standard",
        "skate_drift_king" to "skate_drift_king",
        "skate_cross_rider" to "skate_cross_rider",
        "skate_street_tox" to "skate_street_tox",
        "skate_thunderbolt" to "skate_thunderbolt",
        "skate_titanium_x" to "skate_titanium",
        "skate_neon_phantom" to "skate_neon_phantom",
        "skate_skull_ride" to "skate_skull_ride",
        "skate_cosmic_drift" to "skate_cosmic_drift",
        "skate_solar_flare" to "skate_solar_flare",
        "skate_void_reaper" to "skate_void_reaper",
        "skate_infinity" to "skate_infinity"
    )

    private val cardViews = mutableMapOf<String, CardReferences>()

    private data class CardReferences(
        val image: ImageView,
        val status: TextView,
        val card: LinearLayout
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)

        val preferences = GamePreferences(this)
        playerData = PlayerData(preferences)
        inventory = InventoryData(preferences)

        coinsLabel = findViewById(R.id.tvShopCoins)
        rubiesLabel = findViewById(R.id.tvShopRubies)
        shopContent = findViewById(R.id.shopContent)

        findViewById<View>(R.id.btnShopBack).setOnClickListener {
            returnToHome()
        }

        findViewById<View>(R.id.btnSkates).setOnClickListener {
            selectCategory(Category.SKATES)
        }

        findViewById<View>(R.id.btnHelmets).setOnClickListener {
            selectCategory(Category.HELMETS)
        }

        findViewById<View>(R.id.btnSuits).setOnClickListener {
            selectCategory(Category.SUITS)
        }

        buildSkateCollections()
        updateCurrency()
        updateCategoryButtons()
    }

    private fun returnToHome() {
        val intent = Intent(this, MainMenuActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        startActivity(intent)
        finish()
    }

    private fun selectCategory(category: Category) {
        selectedCategory = category
        updateCategoryButtons()

        if (category != Category.SKATES) {
            Toast.makeText(
                this,
                "${category.name.lowercase(Locale.getDefault())} collection coming soon",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        findViewById<ScrollView>(R.id.shopScroll).scrollTo(0, 0)
    }

    private fun updateCategoryButtons() {
        setCategoryButton(
            R.id.btnSkates,
            selectedCategory == Category.SKATES
        )
        setCategoryButton(
            R.id.btnHelmets,
            selectedCategory == Category.HELMETS
        )
        setCategoryButton(
            R.id.btnSuits,
            selectedCategory == Category.SUITS
        )
    }

    private fun setCategoryButton(id: Int, selected: Boolean) {
        val button = findViewById<Button>(id)

        button.setTextColor(
            if (selected) Color.rgb(17, 24, 39) else Color.WHITE
        )

        button.backgroundTintList = ColorStateList.valueOf(
            if (selected) Color.rgb(255, 208, 68)
            else Color.rgb(38, 53, 77)
        )
    }

    private fun updateCurrency() {
        coinsLabel.text = String.format(
            Locale.US,
            "%,d",
            playerData.coins
        )
        rubiesLabel.text = playerData.rushRubies.toString()
    }

    private fun buildSkateCollections() {
        shopContent.removeAllViews()
        cardViews.clear()

        val collections = listOf(
            SkateRarity.MYTHIC to "MYTHIC SKATES",
            SkateRarity.APEX to "APEX SKATES",
            SkateRarity.PRO to "PRO SKATES",
            SkateRarity.SPEED to "STARTER SKATES"
        )

        for ((rarity, title) in collections) {
            val items = ShopCatalog.items.filter {
                it.type == ShopItemType.SKATE && it.rarity == rarity
            }

            if (items.isEmpty()) continue

            val heading = TextView(this).apply {
                text = title
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(4), dp(10), dp(4), dp(6))
            }

            shopContent.addView(
                heading,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            val horizontalScroll = HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                clipToPadding = false
                setPadding(0, dp(2), 0, dp(8))
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            for (item in items) {
                row.addView(
                    createSkateCard(item),
                    LinearLayout.LayoutParams(
                        cardWidth(),
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginEnd = dp(10)
                    }
                )
            }

            horizontalScroll.addView(row)
            shopContent.addView(
                horizontalScroll,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun cardWidth(): Int {
        val screenWidthDp = resources.configuration.screenWidthDp
        val available = if (screenWidthDp > 0) screenWidthDp else 800

        // Adapt the card to the available screen width while keeping
        // enough space for the skate artwork and its price.
        val widthDp = (available * 0.31f).toInt().coerceIn(150, 220)
        return dp(widthDp)
    }

    private fun createSkateCard(item: ShopItem): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = roundedBackground(
                Color.rgb(38, 53, 77),
                dp(14),
                Color.argb(90, 255, 255, 255)
            )
            isClickable = true
            isFocusable = true
            contentDescription = "${item.name}, skate shop item"
        }

        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            contentDescription = item.name
        }

        val drawableName = drawableByItemId[item.id]
        val drawableId = drawableName?.let {
            resources.getIdentifier(it, "drawable", packageName)
        } ?: 0

        if (drawableId != 0) {
            image.setImageDrawable(
                AppCompatResources.getDrawable(this, drawableId)
            )
        } else {
            image.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        card.addView(
            image,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(112)
            )
        )

        val name = TextView(this).apply {
            text = item.name
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(0, dp(6), 0, dp(4))
        }

        card.addView(
            name,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val price = TextView(this).apply {
            setTextColor(Color.rgb(255, 211, 75))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            maxLines = 1
        }

        card.addView(
            price,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val status = TextView(this).apply {
            setTextColor(Color.rgb(170, 255, 202))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, 0)
            maxLines = 1
        }

        card.addView(
            status,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val refs = CardReferences(image, status, card)
        cardViews[item.id] = refs

        refreshCard(item)

        card.setOnClickListener {
            showProductDialog(item)
        }

        return card
    }

    private fun refreshCard(item: ShopItem) {
        val refs = cardViews[item.id] ?: return
        val owned = inventory.owns(item.id)
        val equipped = playerData.equippedSkateId == item.id

        refs.image.alpha = if (owned) 1f else 0.92f

        refs.status.text = when {
            equipped -> "EQUIPPED"
            owned -> "OWNED"
            item.price == 0 -> "FREE"
            else -> "TAP TO VIEW"
        }

        val priceView = (refs.card.getChildAt(2) as? TextView)
        priceView?.text = when {
            equipped -> "READY TO ROLL"
            owned -> "UNLOCKED"
            item.price == 0 -> "FREE"
            else -> String.format(Locale.US, "%,d COINS", item.price)
        }

        refs.card.alpha = 1f
    }

    private fun refreshAllCards() {
        for (item in ShopCatalog.items.filter {
            it.type == ShopItemType.SKATE
        }) {
            refreshCard(item)
        }
    }

    private fun showProductDialog(item: ShopItem) {
        val owned = inventory.owns(item.id)
        val equipped = playerData.equippedSkateId == item.id
        val stats = SkateStats.forSkate(item.id)

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(16), dp(20), dp(12))
            background = roundedBackground(
                Color.rgb(23, 34, 55),
                dp(18),
                Color.rgb(255, 208, 68)
            )
        }

        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            contentDescription = item.name
        }

        val drawableName = drawableByItemId[item.id]
        val drawableId = drawableName?.let {
            resources.getIdentifier(it, "drawable", packageName)
        } ?: 0

        if (drawableId != 0) {
            image.setImageResource(drawableId)
        } else {
            image.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        panel.addView(
            image,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(125)
            )
        )

        val title = TextView(this).apply {
            text = item.name.uppercase(Locale.getDefault())
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 21f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(4))
        }

        panel.addView(title)

        val rarity = TextView(this).apply {
            text = "${item.rarity.name} CLASS"
            setTextColor(Color.rgb(255, 208, 68))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        panel.addView(rarity)

        val description = TextView(this).apply {
            text = item.description.ifBlank {
                "Customize your RollerRush setup."
            }
            setTextColor(Color.rgb(220, 228, 242))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(10))
        }

        panel.addView(description)

        val statsText = TextView(this).apply {
            text = "SPEED   ${stats.speedLevel}/5\n" +
                    "JUMP      ${stats.jumpLevel}/3\n" +
                    "CONTROL ${stats.controlLevel}/3"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(10))
        }

        panel.addView(statsText)

        val priceText = TextView(this).apply {
            text = if (item.price == 0) {
                "FREE"
            } else {
                String.format(Locale.US, "%,d COINS", item.price)
            }
            setTextColor(Color.rgb(255, 211, 75))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(12))
        }

        panel.addView(priceText)

        val dialog = AlertDialog.Builder(this)
            .setView(panel)
            .create()

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val closeButton = Button(this).apply {
            text = "CANCEL"
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(
                Color.rgb(73, 86, 108)
            )
        }

        val actionButton = Button(this).apply {
            text = when {
                equipped -> "EQUIPPED"
                owned -> "EQUIP"
                else -> "BUY"
            }
            isAllCaps = false
            setTextColor(Color.rgb(20, 28, 40))
            backgroundTintList = ColorStateList.valueOf(
                Color.rgb(255, 208, 68)
            )
            isEnabled = !equipped
        }

        buttons.addView(
            closeButton,
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginEnd = dp(8)
            }
        )

        buttons.addView(
            actionButton,
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )

        panel.addView(
            buttons,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        actionButton.setOnClickListener {
            // Check the current state again when the player confirms.
            if (inventory.owns(item.id)) {
                playerData.equippedSkateId = item.id
                sounds.playSkateEquipped()
            } else {
                if (item.price > 0 && !playerData.spendCoins(item.price)) {
                    Toast.makeText(
                        this,
                        "Not enough coins!",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateCurrency()
                    return@setOnClickListener
                }

                inventory.unlock(item.id)
                playerData.equippedSkateId = item.id
                sounds.playSkatePurchased()
            }

            updateCurrency()
            refreshAllCards()
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            val width = min(
                resources.configuration.screenWidthDp - 32,
                520
            ).coerceAtLeast(280)

            dialog.window?.setBackgroundDrawableResource(
                android.R.color.transparent
            )

            dialog.window?.setLayout(
                dp(width),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        dialog.show()

        // Apply the desired width after the dialog has been attached.
        val width = min(
            resources.configuration.screenWidthDp - 32,
            520
        ).coerceAtLeast(280)

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        dialog.window?.setLayout(
            dp(width),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun roundedBackground(
        fillColor: Int,
        cornerRadius: Int,
        strokeColor: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fillColor)
            cornerRadius.toFloat().also { radius ->
                setCornerRadius(radius)
            }
            setStroke(dp(1), strokeColor)
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onResume() {
        super.onResume()

        if (::playerData.isInitialized) {
            updateCurrency()
            refreshAllCards()
        }
    }


    override fun onDestroy() {
        sounds.release()
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        (application as RollerRushApp).bgmManager.playLobby()
    }

}
