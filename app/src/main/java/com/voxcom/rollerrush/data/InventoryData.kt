package com.voxcom.rollerrush.data

enum class ShopItemType { SKATE, CHARACTER }

data class ShopItem(
    val id: String,
    val name: String,
    val price: Int,
    val type: ShopItemType,
    val assetName: String = "",
    val particleName: String = "",
    val description: String = ""
)

/**
 * Roller Rush shop catalogue.
 * Prices are intentionally kept here so balancing later only requires editing this file.
 */
object ShopCatalog {
    val items: List<ShopItem> = listOf(
        ShopItem(
            id = "skate_basic",
            name = "Red Street",
            price = 0,
            type = ShopItemType.SKATE,
            assetName = "skate1.png",
            particleName = "particle_red.png",
            description = "Your original street setup."
        ),
        ShopItem(
            id = "skate_ice",
            name = "Ice Glide",
            price = 500,
            type = ShopItemType.SKATE,
            assetName = "skate2.png",
            particleName = "particle_ice.png",
            description = "Smooth and cold. A little quicker."
        ),
        ShopItem(
            id = "skate_aero",
            name = "Aero Blue",
            price = 100,
            type = ShopItemType.SKATE,
            assetName = "skate3.png",
            particleName = "particle_aero.png",
            description = "Lightweight racing setup."
        ),
        ShopItem(
            id = "skate_inferno",
            name = "Inferno",
            price = 200,
            type = ShopItemType.SKATE,
            assetName = "skate4.png",
            particleName = "particle_inferno.png",
            description = "Built for aggressive runs."
        ),
        ShopItem(
            id = "skate_volt",
            name = "Volt Runner",
            price = 3500,
            type = ShopItemType.SKATE,
            assetName = "skate5.png",
            particleName = "particle_volt.png",
            description = "High-end electric racing wheels."
        ),
        ShopItem("char_basic", "Basic Character", 0, ShopItemType.CHARACTER),
        ShopItem("char_skin_2", "Character Skin 2", 50, ShopItemType.CHARACTER)
    )

    fun skate(id: String): ShopItem? = items.firstOrNull { it.id == id && it.type == ShopItemType.SKATE }
}

class InventoryData(private val prefs: GamePreferences) {

    fun owns(id: String): Boolean = id in DEFAULT_OWNED || id in prefs.getStringSet(KEY_OWNED)

    fun unlock(id: String) {
        prefs.putStringSet(KEY_OWNED, prefs.getStringSet(KEY_OWNED) + id)
    }

    private companion object {
        const val KEY_OWNED = "inventory_owned"
        val DEFAULT_OWNED = setOf("skate_basic", "char_basic")
    }
}
