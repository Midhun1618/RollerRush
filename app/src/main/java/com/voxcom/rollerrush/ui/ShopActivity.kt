package com.voxcom.rollerrush.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.voxcom.rollerrush.R
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

/** Uses the hand-authored XML cards directly. No RecyclerView-generated replacement UI. */
class ShopActivity : AppCompatActivity() {
    private lateinit var playerData: PlayerData
    private lateinit var inventory: InventoryData
    private lateinit var coinsLabel: TextView
    private lateinit var rubiesLabel: TextView
    private lateinit var shopContent: ViewGroup
    private val sounds by lazy { GameSoundEffects(applicationContext) }

    private enum class Category { SKATES, HELMETS, SUITS }
    private var selectedCategory = Category.SKATES

    // Drawable names are mapped to catalog IDs, so your custom image assets remain untouched.
    private val drawableToItem = mapOf(
        "skate1" to "skate_basic",
        "skate2" to "skate_ice",
        "skate3" to "skate_aero",
        "skate4" to "skate_inferno",
        "skate5" to "skate_volt",
        "skate_infinity" to "skate_infinity",
        "skate_void_reaper" to "skate_void_reaper",
        "skate_solar_flare" to "skate_solar_flare",
        "skate_cosmic_drift" to "skate_cosmic_drift",
        "skate_neon_phantom" to "skate_neon_phantom",
        "skate_cross_rider" to "skate_cross_rider",
        "skate_street_tox" to "skate_street_tox",
        "skate_nitro_edge" to "skate_nitro_edge",
        "skate_sunset_rush" to "skate_sunset_rush",
        "skate_gold_standard" to "skate_gold_standard",
        "skate_sonic_wave" to "skate_sonic_wave",
        "skate_titanium" to "skate_titanium_x",
        "skate_skull_ride" to "skate_skull_ride"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)
        val prefs = GamePreferences(this)
        playerData = PlayerData(prefs)
        inventory = InventoryData(prefs)
        coinsLabel = findViewById(R.id.tvShopCoins)
        rubiesLabel = findViewById(R.id.tvShopRubies)
        shopContent = findViewById(R.id.shopContent)

        findViewById<View>(R.id.btnSkates).setOnClickListener { selectCategory(Category.SKATES) }
        findViewById<View>(R.id.btnHelmets).setOnClickListener { selectCategory(Category.HELMETS) }
        findViewById<View>(R.id.btnSuits).setOnClickListener { selectCategory(Category.SUITS) }

        bindExistingCards(shopContent)
        updateCurrency()
        updateCategoryButtons()
    }

    private fun selectCategory(category: Category) {
        selectedCategory = category
        updateCategoryButtons()
        if (category != Category.SKATES) {
            Toast.makeText(this, "${category.name.lowercase().replaceFirstChar { it.uppercase() }} collection is coming soon", Toast.LENGTH_SHORT).show()
        }
        // Keep the custom skate artwork visible only for the Skates tab.
        shopContent.visibility = if (category == Category.SKATES) View.VISIBLE else View.GONE
    }

    private fun updateCategoryButtons() {
        setCategoryButton(R.id.btnSkates, selectedCategory == Category.SKATES)
        setCategoryButton(R.id.btnHelmets, selectedCategory == Category.HELMETS)
        setCategoryButton(R.id.btnSuits, selectedCategory == Category.SUITS)
    }

    private fun setCategoryButton(id: Int, selected: Boolean) {
        val button = findViewById<Button>(id)
        button.setTextColor(if (selected) Color.rgb(17, 24, 39) else Color.WHITE)
        button.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (selected) Color.rgb(255, 208, 68) else Color.rgb(38, 53, 77)
        )
    }

    private fun updateCurrency() {
        coinsLabel.text = String.format(Locale.US, "%,d", playerData.coins)
        rubiesLabel.text = playerData.rushRubies.toString()
    }

    private fun bindExistingCards(parent: ViewGroup) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child !is ViewGroup) continue

            // Each hand-designed product card is a ConstraintLayout. Do not bind its
            // surrounding collection row, otherwise one click would cover the whole section.
            if (child.javaClass.name == "androidx.constraintlayout.widget.ConstraintLayout") {
                val image = findProductImage(child)
                val drawableId = image?.tag as? Int
                val drawableName = drawableId?.let { resources.getResourceEntryName(it) }
                val itemId = drawableName?.let { drawableToItem[it] }
                val item = itemId?.let { id -> ShopCatalog.items.firstOrNull { it.id == id } }
                if (image != null && item != null && item.type == ShopItemType.SKATE) {
                    bindCard(child, image, item)
                } else if (image != null && itemId != null) {
                    child.setOnClickListener {
                        Toast.makeText(this, "Add '$itemId' to ShopCatalog to enable purchasing this card", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            bindExistingCards(child)
        }
    }

    private fun findProductImage(root: ViewGroup): ImageView? {
        val supported = setOf(
            "skate1", "skate2", "skate3", "skate4", "skate5",
            "skate_infinity", "skate_void_reaper", "skate_solar_flare",
            "skate_cosmic_drift", "skate_neon_phantom", "skate_cross_rider",
            "skate_street_tox", "skate_nitro_edge", "skate_sunset_rush",
            "skate_gold_standard", "skate_sonic_wave", "skate_titanium",
            "skate_skull_ride"
        )
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (child is ImageView && child.drawable != null) {
                for (name in supported) {
                    val resourceId = resources.getIdentifier(name, "drawable", packageName)
                    if (resourceId == 0) continue
                    val candidate = runCatching {
                        androidx.appcompat.content.res.AppCompatResources.getDrawable(this, resourceId)
                    }.getOrNull() ?: continue
                    if (child.drawable.constantState == candidate.constantState) {
                        child.tag = resourceId
                        return child
                    }
                }
            }
            if (child is ViewGroup) findProductImage(child)?.let { return it }
        }
        return null
    }

    private fun bindCard(card: ViewGroup, image: ImageView, item: ShopItem) {
        val nameView = findTextView(card) { it.text.toString().equals(item.name, ignoreCase = true) }
        val priceView = findPriceTextView(card)
        fun refresh() {
            val owned = inventory.owns(item.id)
            val equipped = playerData.equippedSkateId == item.id
            nameView?.alpha = if (owned) 1f else 0.9f
            image.alpha = if (owned) 1f else 0.86f
            priceView?.text = when {
                equipped -> "EQUIPPED"
                owned -> "OWNED"
                item.price == 0 -> "FREE"
                else -> String.format(Locale.US, "%,d", item.price)
            }
        }
        refresh()
        card.isClickable = true
        card.setOnClickListener {
            showProductDialog(item) {
                refresh()
            }
        }
    }

    private fun findTextView(root: ViewGroup, predicate: (TextView) -> Boolean): TextView? {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (child is TextView && predicate(child)) return child
            if (child is ViewGroup) findTextView(child, predicate)?.let { return it }
        }
        return null
    }


    private fun findPriceTextView(root: ViewGroup): TextView? {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)

            if (child is ViewGroup) {
                val hasCoin = (0 until child.childCount).any { index ->
                    val view = child.getChildAt(index)
                    view is ImageView && isRushCoinImage(view)
                }

                if (hasCoin) {
                    for (j in 0 until child.childCount) {
                        val view = child.getChildAt(j)
                        if (view is TextView) return view
                    }
                }

                findPriceTextView(child)?.let { return it }
            }
        }

        return null
    }

    private fun isRushCoinImage(imageView: ImageView): Boolean {
        val expected = AppCompatResources.getDrawable(
            this,
            R.drawable.rush_coin
        ) ?: return false

        val actual = imageView.drawable ?: return false

        return actual.constantState != null &&
                actual.constantState == expected.constantState
    }


    private fun showProductDialog(item: ShopItem, refreshCard: () -> Unit) {
        val owned = inventory.owns(item.id)
        val equipped = playerData.equippedSkateId == item.id
        val stats = SkateStats.forSkate(item.id)
        val message = buildString {
            append(item.description.ifBlank { "Customize your RollerRush setup." })
            append("\n\nRarity: ${item.rarity.name}")
            append("\nSpeed: ${stats.speedLevel}/5")
            append("\nJump: ${stats.jumpLevel}/3")
            append("\nControl: ${stats.controlLevel}/3")
            append("\n\nPrice: ${if (item.price == 0) "FREE" else String.format(Locale.US, "%,d coins", item.price)}")
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(item.name.uppercase(Locale.getDefault()))
            .setMessage(message)
            .setNegativeButton("CLOSE", null)
            .setPositiveButton(when { equipped -> "EQUIPPED"; owned -> "EQUIP"; else -> "BUY" }, null)
            .create()
        dialog.setOnShowListener {
            val action = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            if (equipped) action.isEnabled = false
            else action.setOnClickListener {
                if (inventory.owns(item.id)) {
                    playerData.equippedSkateId = item.id
                    sounds.playSkateEquipped()
                } else {
                    if (item.price > 0 && !playerData.spendCoins(item.price)) {
                        Toast.makeText(this, "Not enough coins", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    inventory.unlock(item.id)
                    playerData.equippedSkateId = item.id
                    sounds.playSkatePurchased()
                }
                updateCurrency()
                refreshCard()
                dialog.dismiss()
                // Refresh all cards so the previous equipped item no longer says EQUIPPED.
                bindExistingCards(shopContent)
            }
        }
        dialog.show()
    }

    override fun onDestroy() {
        sounds.release()
        super.onDestroy()
    }
}
