package com.voxcom.rollerrush.data

enum class ShopItemType { SKATE, CHARACTER }

data class ShopItem(val id: String, val name: String, val price: Int, val type: ShopItemType)

/** Static shop catalogue (placeholder). Replace with remote/config data later. */
object ShopCatalog {
    val items: List<ShopItem> = listOf(
        ShopItem("skate_basic", "Basic Skate", 0, ShopItemType.SKATE),
        ShopItem("skate_speed", "Speed Skate", 100, ShopItemType.SKATE),
        ShopItem("char_basic", "Basic Character", 0, ShopItemType.CHARACTER),
        ShopItem("char_skin_2", "Character Skin 2", 50, ShopItemType.CHARACTER)
    )
}

/** Which shop items the player owns. */
class InventoryData(private val prefs: GamePreferences) {

    fun owns(id: String): Boolean =
        id in DEFAULT_OWNED || id in prefs.getStringSet(KEY_OWNED)

    fun unlock(id: String) {
        prefs.putStringSet(KEY_OWNED, prefs.getStringSet(KEY_OWNED) + id)
    }

    private companion object {
        const val KEY_OWNED = "inventory_owned"
        val DEFAULT_OWNED = setOf("skate_basic", "char_basic")
    }
}
