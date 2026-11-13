package com.voxcom.rollerrush.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
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

    private val sounds by lazy { GameSoundEffects(applicationContext) }

    private enum class Category { ALL, STARTER, PRO, APEX, MYTHIC }
    private var selectedCategory = Category.ALL

    // Some catalog items temporarily reuse similar existing skate artwork.
    private val drawableByItemId = mapOf(
        "skate_basic" to "skate1",
        "skate_ice" to "skate2",
        "skate_aero" to "skate3",
        "skate_inferno" to "skate4",
        "skate_volt" to "skate5",
        "skate_sunset_rush" to "skate_sunset_rush",
        "skate_pixel_pop" to "skate_pixel_pop",
        "skate_night_shift" to "skate_void_reaper",
        "skate_nitro_edge" to "skate_nitro_edge",
        "skate_sonic_wave" to "skate_sonic_wave",
        "skate_gold_standard" to "skate_gold_standard",
        "skate_drift_king" to "skate_cross_rider",
        "skate_cross_rider" to "skate_cross_rider",
        "skate_street_tox" to "skate_street_tox",
        "skate_thunderbolt" to "skate_thunderbolt",
        "skate_titanium_x" to "skate_titanium",
        "skate_neon_phantom" to "skate_sonic_wave",
        "skate_skull_ride" to "skate_skull_ride",
        "skate_cosmic_drift" to "skate_cosmic_drift",
        "skate_solar_flare" to "skate_solar_flare",
        "skate_void_reaper" to "skate_void_reaper",
        "skate_infinity" to "skate_infinity"
    )

    private data class CardReferences(
        val image: ImageView,
        val price: TextView,
        val status: TextView,
        val card: LinearLayout
    )

    private val cardViews = mutableMapOf<String, CardReferences>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)

        val preferences = GamePreferences(this)
        playerData = PlayerData(preferences)
        inventory = InventoryData(preferences)

        coinsLabel = findViewById(R.id.tvShopCoins)
        rubiesLabel = findViewById(R.id.tvShopRubies)
        shopContent = findViewById(R.id.shopContent)

        findViewById<View>(R.id.btnShopBack).setOnClickListener { returnToHome() }
        findViewById<View>(R.id.btnAll).setOnClickListener { selectCategory(Category.ALL) }
        findViewById<View>(R.id.btnStarter).setOnClickListener { selectCategory(Category.STARTER) }
        findViewById<View>(R.id.btnPro).setOnClickListener { selectCategory(Category.PRO) }
        findViewById<View>(R.id.btnApex).setOnClickListener { selectCategory(Category.APEX) }
        findViewById<View>(R.id.btnMythic).setOnClickListener { selectCategory(Category.MYTHIC) }

        buildSkateCollections()
        updateCurrency()
        updateCategoryButtons()
    }

    private fun returnToHome() {
        val intent = Intent(this, MainMenuActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun selectCategory(category: Category) {
        selectedCategory = category
        updateCategoryButtons()
        buildSkateCollections()
        findViewById<ScrollView>(R.id.shopScroll).scrollTo(0, 0)
    }

    private fun updateCategoryButtons() {
        setCategoryButton(R.id.btnAll, selectedCategory == Category.ALL)
        setCategoryButton(R.id.btnStarter, selectedCategory == Category.STARTER)
        setCategoryButton(R.id.btnPro, selectedCategory == Category.PRO)
        setCategoryButton(R.id.btnApex, selectedCategory == Category.APEX)
        setCategoryButton(R.id.btnMythic, selectedCategory == Category.MYTHIC)
    }

    private fun setCategoryButton(id: Int, selected: Boolean) {
        val button = findViewById<Button>(id)
        button.setTextColor(if (selected) Color.rgb(17, 24, 39) else Color.WHITE)
        button.backgroundTintList = ColorStateList.valueOf(
            if (selected) Color.rgb(255, 208, 68) else Color.rgb(38, 53, 77)
        )
    }

    private fun updateCurrency() {
        coinsLabel.text = String.format(Locale.US, "%,d", playerData.coins)
        rubiesLabel.text = playerData.rushRubies.toString()
    }

    private fun buildSkateCollections() {
        shopContent.removeAllViews()
        cardViews.clear()

        val screenWidth = resources.configuration.screenWidthDp

        // Five cards on wider landscape screens, four on compact screens.
        val columns = if (screenWidth >= 850) 5 else 4

        val rarities = when (selectedCategory) {
            Category.ALL -> listOf(
                SkateRarity.MYTHIC,
                SkateRarity.APEX,
                SkateRarity.PRO,
                SkateRarity.SPEED
            )

            Category.STARTER -> listOf(SkateRarity.SPEED)
            Category.PRO -> listOf(SkateRarity.PRO)
            Category.APEX -> listOf(SkateRarity.APEX)
            Category.MYTHIC -> listOf(SkateRarity.MYTHIC)
        }

        // Portrait collectible-card proportions.
        val availableWidth = (screenWidth - 32).coerceAtLeast(480)
        val estimatedCardWidth = availableWidth / columns
        val cardHeight = (estimatedCardWidth * 1.48f)
            .toInt()
            .coerceIn(205, 250)

        rarities.forEach { rarity ->
            val items = ShopCatalog.items.filter {
                it.type == ShopItemType.SKATE && it.rarity == rarity
            }

            if (items.isEmpty()) return@forEach

            addCollectionHeading(rarity, items.size)

            items.chunked(columns).forEach { rowItems ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.TOP
                }

                rowItems.forEach { item ->
                    val params = LinearLayout.LayoutParams(
                        0,
                        dp(cardHeight),
                        1f
                    ).apply {
                        marginStart = dp(4)
                        marginEnd = dp(4)
                    }

                    row.addView(createSkateCard(item, cardHeight), params)
                }

                // Keep the same column widths in the final incomplete row.
                repeat(columns - rowItems.size) {
                    row.addView(
                        View(this),
                        LinearLayout.LayoutParams(0, dp(1), 1f).apply {
                            marginStart = dp(4)
                            marginEnd = dp(4)
                        }
                    )
                }

                shopContent.addView(
                    row,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(10)
                    }
                )
            }
        }
    }

    private fun addCollectionHeading(rarity: SkateRarity, count: Int) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(3), dp(10), dp(3), dp(7))
        }

        val title = TextView(this).apply {
            text = when (rarity) {
                SkateRarity.SPEED -> "STARTER SERIES"
                SkateRarity.PRO -> "PRO SERIES"
                SkateRarity.APEX -> "APEX SERIES"
                SkateRarity.MYTHIC -> "MYTHIC SERIES"
            }
            setTextColor(rarityColor(rarity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            typeface = Typeface.DEFAULT_BOLD
        }

        val countLabel = TextView(this).apply {
            text = "$count RIDES"
            setTextColor(Color.rgb(178, 190, 207))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
        }

        row.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )
        row.addView(countLabel)

        shopContent.addView(
            row,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun createSkateCard(item: ShopItem, cardHeight: Int): View {
        val stats = SkateStats.forSkate(item.id)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(6), dp(6), dp(6), dp(7))
            background = resourceDrawable("shop_item_card_bg")
                ?: roundedBackground(
                    Color.rgb(28, 42, 63),
                    dp(14),
                    Color.argb(100, 255, 255, 255)
                )
            elevation = dp(2).toFloat()
            isClickable = true
            isFocusable = true
            contentDescription =
                "${item.name}, overall ${stats.overallRating} out of 10"
        }

        val artworkFrame = FrameLayout(this).apply {
            background = rarityCardBackground(item.rarity)
                ?: roundedBackground(
                    Color.rgb(35, 55, 80),
                    dp(11),
                    rarityColor(item.rarity)
                )
        }

        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            contentDescription = item.name
            setImageResource(skateDrawableResource(item))
        }

        artworkFrame.addView(
            image,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(dp(12), dp(7), dp(12), dp(7))
            }
        )

        val overallBadge = TextView(this).apply {
            text = "${stats.overallRating}\nOVR"
            setTextColor(Color.rgb(22, 30, 43))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = roundedBackground(
                Color.rgb(255, 211, 75),
                dp(10),
                Color.rgb(255, 239, 170)
            )
            setLineSpacing(0f, 0.9f)
        }

        artworkFrame.addView(
            overallBadge,
            FrameLayout.LayoutParams(
                dp(43),
                dp(43),
                Gravity.TOP or Gravity.START
            ).apply {
                setMargins(dp(7), dp(7), 0, 0)
            }
        )

        val rarityBadge = TextView(this).apply {
            text = rarityLabel(item.rarity)
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(7), dp(5), dp(7), dp(5))
            background = roundedBackground(
                Color.argb(205, 13, 22, 37),
                dp(8),
                Color.argb(120, 255, 255, 255)
            )
        }

        artworkFrame.addView(
            rarityBadge,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END
            ).apply {
                setMargins(0, dp(7), dp(7), 0)
            }
        )

        val artworkHeight = dp((cardHeight - 62).coerceAtLeast(130))

        card.addView(
            artworkFrame,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                artworkHeight
            )
        )

        val name = TextView(this).apply {
            text = item.name.uppercase(Locale.getDefault())
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(0, dp(8), 0, dp(2))
        }

        card.addView(name)

        // Keep the card clean. Detailed stats are displayed in the dialog.
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(7), 0, 0)
        }

        val price = TextView(this).apply {
            setTextColor(Color.rgb(255, 211, 75))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        val status = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(4), dp(4), dp(4))
            background = roundedBackground(
                Color.argb(45, 255, 255, 255),
                dp(8),
                Color.argb(70, 255, 255, 255)
            )
            maxLines = 1
        }

        footer.addView(
            price,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )
        footer.addView(status)
        card.addView(footer)

        cardViews[item.id] = CardReferences(image, price, status, card)

        refreshCard(item)
        card.setOnClickListener { showProductDialog(item) }

        return card
    }

    private fun addStatRow(
        parent: LinearLayout,
        label: String,
        value: Int,
        tint: Int,
        compact: Boolean
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(if (compact) 3 else 5), 0, 0)
        }

        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val labelView = TextView(this).apply {
            text = label
            setTextColor(Color.rgb(205, 215, 229))
            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                if (compact) 9f else 12f
            )
            typeface = Typeface.DEFAULT_BOLD
        }

        val valueView = TextView(this).apply {
            text = "$value/10"
            setTextColor(Color.WHITE)
            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                if (compact) 9f else 12f
            )
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.END
        }

        labels.addView(
            labelView,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )
        labels.addView(valueView)
        row.addView(labels)

        val progress = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            isIndeterminate = false
            max = 10
            this.progress = value.coerceIn(0, 10)
            progressTintList = ColorStateList.valueOf(tint)
            progressBackgroundTintList =
                ColorStateList.valueOf(Color.rgb(62, 76, 96))
        }

        row.addView(
            progress,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(if (compact) 5 else 7)
            ).apply {
                topMargin = dp(2)
            }
        )

        parent.addView(row)
    }

    private fun refreshCard(item: ShopItem) {
        val refs = cardViews[item.id] ?: return

        val owned = inventory.owns(item.id)
        val equipped = playerData.equippedSkateId == item.id
        val levelLocked = !owned && playerData.level < item.unlockLevel

        refs.image.alpha = if (levelLocked) 0.38f else 1f
        refs.card.alpha = if (levelLocked) 0.78f else 1f

        refs.status.text = when {
            equipped -> "EQUIPPED"
            owned -> "OWNED"
            levelLocked -> "LEVEL ${item.unlockLevel}"
            item.price == 0 -> "FREE"
            else -> "LOCKED"
        }

        refs.status.setTextColor(
            when {
                equipped -> Color.rgb(255, 220, 105)
                owned -> Color.rgb(112, 238, 177)
                levelLocked -> Color.rgb(180, 190, 205)
                else -> Color.rgb(205, 215, 229)
            }
        )

        refs.price.text = when {
            equipped -> "READY TO ROLL"
            owned -> "UNLOCKED"
            levelLocked -> "UNLOCKS AT LEVEL ${item.unlockLevel}"
            item.price == 0 -> "FREE"
            else -> String.format(Locale.US, "%,d COINS", item.price)
        }

        refs.card.background = resourceDrawable(
            if (equipped) "shop_equipped_item_card_bg" else "shop_item_card_bg"
        ) ?: roundedBackground(
            Color.rgb(28, 42, 63),
            dp(14),
            if (equipped) Color.rgb(255, 211, 75) else Color.argb(100, 255, 255, 255)
        )
    }

    private fun refreshAllCards() {
        ShopCatalog.items
            .filter { it.type == ShopItemType.SKATE }
            .forEach(::refreshCard)
    }

    private fun showProductDialog(item: ShopItem) {
        val stats = SkateStats.forSkate(item.id)

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(16), dp(20), dp(14))
            background = roundedBackground(
                Color.rgb(23, 34, 55),
                dp(18),
                rarityColor(item.rarity)
            )
        }

        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = item.name
            setImageResource(skateDrawableResource(item))
        }

        panel.addView(
            image,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(150)
            )
        )

        val title = TextView(this).apply {
            text = item.name.uppercase(Locale.getDefault())
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(7), 0, dp(3))
        }

        panel.addView(title)

        val rarity = TextView(this).apply {
            text = "${rarityLabel(item.rarity)} CLASS  •  OVR ${stats.overallRating}/10"
            setTextColor(rarityColor(item.rarity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        panel.addView(rarity)

        val description = TextView(this).apply {
            text = item.description.ifBlank {
                "Customize your Roller Rush setup."
            }
            setTextColor(Color.rgb(220, 228, 242))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(7))
        }

        panel.addView(description)

        // Detailed stat bars appear only inside this dialog.
        addStatRow(
            panel,
            "SPEED",
            stats.speedRating,
            Color.rgb(73, 205, 255),
            false
        )

        addStatRow(
            panel,
            "JUMP",
            stats.jumpRating,
            Color.rgb(255, 205, 75),
            false
        )

        addStatRow(
            panel,
            "CONTROL",
            stats.controlRating,
            Color.rgb(99, 225, 166),
            false
        )

        val priceText = TextView(this).apply {
            text = when {
                playerData.equippedSkateId == item.id -> "CURRENTLY EQUIPPED"
                inventory.owns(item.id) -> "ALREADY IN YOUR COLLECTION"
                playerData.level < item.unlockLevel -> "UNLOCKS AT LEVEL ${item.unlockLevel}"
                item.price == 0 -> "FREE"
                else -> String.format(Locale.US, "%,d COINS", item.price)
            }

            setTextColor(Color.rgb(255, 211, 75))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(10))
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
            text = "CLOSE"
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList =
                ColorStateList.valueOf(Color.rgb(73, 86, 108))
        }

        val owned = inventory.owns(item.id)
        val equipped = playerData.equippedSkateId == item.id

        val actionButton = Button(this).apply {
            text = when {
                equipped -> "EQUIPPED"
                owned -> "EQUIP"
                playerData.level < item.unlockLevel -> "LOCKED"
                item.price == 0 -> "CLAIM"
                else -> "BUY"
            }

            isAllCaps = false
            setTextColor(Color.rgb(20, 28, 40))
            backgroundTintList =
                ColorStateList.valueOf(Color.rgb(255, 208, 68))
            isEnabled = !equipped && (owned || playerData.level >= item.unlockLevel)
        }

        buttons.addView(
            closeButton,
            LinearLayout.LayoutParams(0, dp(46), 1f).apply {
                marginEnd = dp(8)
            }
        )

        buttons.addView(
            actionButton,
            LinearLayout.LayoutParams(0, dp(46), 1f)
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
            if (!inventory.owns(item.id) && playerData.level < item.unlockLevel) {
                RollerRushNotification.show(
                    this,
                    "Reach Level ${item.unlockLevel} to unlock this skate.",
                    RollerRushNotification.Type.WARNING
                )
                return@setOnClickListener
            } else {
                if (item.price > 0 && !playerData.spendCoins(item.price)) {
                    RollerRushNotification.show(
                        this,
                        "Not enough coins! Keep skating to earn more.",
                        RollerRushNotification.Type.WARNING
                    )

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

        dialog.show()

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

    private fun skateDrawableResource(item: ShopItem): Int {
        val name = drawableByItemId[item.id]
            ?: item.assetName
                .substringAfterLast('/')
                .substringBeforeLast('.')

        val id = resources.getIdentifier(
            name,
            "drawable",
            packageName
        )

        return if (id != 0) id else R.drawable.skate1
    }

    private fun rarityCardBackground(rarity: SkateRarity): Drawable? {
        val name = when (rarity) {
            SkateRarity.SPEED -> "speed_card_bg"
            SkateRarity.PRO -> "pro_card_bg"
            SkateRarity.APEX -> "apex_card_bg"
            SkateRarity.MYTHIC -> "mythic_card_bg"
        }

        return resourceDrawable(name)
    }

    private fun resourceDrawable(name: String): Drawable? {
        val id = resources.getIdentifier(
            name,
            "drawable",
            packageName
        )

        return if (id != 0) {
            AppCompatResources.getDrawable(this, id)
        } else {
            null
        }
    }

    private fun rarityLabel(rarity: SkateRarity): String = when (rarity) {
        SkateRarity.SPEED -> "STARTER"
        SkateRarity.PRO -> "PRO"
        SkateRarity.APEX -> "APEX"
        SkateRarity.MYTHIC -> "MYTHIC"
    }

    private fun rarityColor(rarity: SkateRarity): Int = when (rarity) {
        SkateRarity.SPEED -> Color.rgb(92, 202, 235)
        SkateRarity.PRO -> Color.rgb(104, 224, 165)
        SkateRarity.APEX -> Color.rgb(255, 173, 76)
        SkateRarity.MYTHIC -> Color.rgb(210, 135, 255)
    }

    private fun roundedBackground(
        fillColor: Int,
        cornerRadius: Int,
        strokeColor: Int
    ): GradientDrawable = GradientDrawable().apply {
        setColor(fillColor)
        setCornerRadius(cornerRadius.toFloat())
        setStroke(dp(1), strokeColor)
    }

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        resources.displayMetrics
    ).toInt()

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