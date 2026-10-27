
package com.voxcom.rollerrush.data

enum class ShopItemType {
    SKATE,
    CHARACTER
}

enum class SkateRarity {
    SPEED,
    PRO,
    APEX,
    MYTHIC
}

data class ShopItem(
    val id: String,
    val name: String,
    val price: Int,
    val type: ShopItemType,
    val assetName: String = "",
    val particleName: String = "",
    val description: String = "",
    val rarity: SkateRarity = SkateRarity.SPEED
)

object ShopCatalog {

    val items: List<ShopItem> = listOf(

        // SPEED
        ShopItem(
            "skate_basic", "Red Street", 0, ShopItemType.SKATE,
            "skate1.png",
            description = "Your original street setup.",
            rarity = SkateRarity.SPEED
        ),
        ShopItem(
            "skate_ice", "Ice Glide", 500, ShopItemType.SKATE,
            "skate2.png",
            description = "Smooth and cold.",
            rarity = SkateRarity.SPEED
        ),
        ShopItem(
            "skate_sunset_rush", "Sunset Rush", 900, ShopItemType.SKATE,
            "skate_sunset_rush.png",
            description = "Bright colors and racing energy.",
            rarity = SkateRarity.SPEED
        ),
        ShopItem(
            "skate_pixel_pop", "Pixel Pop", 1100, ShopItemType.SKATE,
            "skate_pixel_pop.png",
            description = "An arcade-inspired design.",
            rarity = SkateRarity.SPEED
        ),
        ShopItem(
            "skate_night_shift", "Night Shift", 1300, ShopItemType.SKATE,
            "skate_night_shift.png",
            description = "A stealthy black-and-silver style.",
            rarity = SkateRarity.SPEED
        ),

        // PRO
        ShopItem(
            "skate_aero", "Aero Blue", 1800, ShopItemType.SKATE,
            "skate3.png",
            description = "Lightweight racing setup.",
            rarity = SkateRarity.PRO
        ),
        ShopItem(
            "skate_nitro_edge", "Nitro Edge", 2100, ShopItemType.SKATE,
            "skate_nitro_edge.png",
            description = "Motorsport-inspired racing style.",
            rarity = SkateRarity.PRO
        ),
        ShopItem(
            "skate_sonic_wave", "Sonic Wave", 2400, ShopItemType.SKATE,
            "skate_sonic_wave.png",
            description = "A streamlined speed design.",
            rarity = SkateRarity.PRO
        ),
        ShopItem(
            "skate_gold_standard", "Gold Standard", 2700, ShopItemType.SKATE,
            "skate_gold_standard.png",
            description = "Premium metallic detailing.",
            rarity = SkateRarity.PRO
        ),
        ShopItem(
            "skate_drift_king", "Drift King", 3300, ShopItemType.SKATE,
            "skate_drift_king.png",
            description = "Made for a drifting-inspired look.",
            rarity = SkateRarity.PRO
        ),
        ShopItem(
            "skate_cross_rider", "Cross Rider", 2000, ShopItemType.SKATE,
            "skate_cross_rider.png",
            description = "A bold street-racing design.",
            rarity = SkateRarity.PRO
        ),

        // APEX
        ShopItem(
            "skate_volt", "Volt Runner", 3000, ShopItemType.SKATE,
            "skate5.png",
            description = "Electric racing wheels.",
            rarity = SkateRarity.APEX
        ),
        ShopItem(
            "skate_street_tox", "Street Tox", 3800, ShopItemType.SKATE,
            "skate_street_tox.png",
            description = "An aggressive urban skate style.",
            rarity = SkateRarity.APEX
        ),
        ShopItem(
            "skate_inferno", "Inferno", 4000, ShopItemType.SKATE,
            "skate4.png",
            description = "Built for aggressive runs.",
            rarity = SkateRarity.APEX
        ),
        ShopItem(
            "skate_thunderbolt", "Thunderbolt", 4500, ShopItemType.SKATE,
            "skate_thunderbolt.png",
            description = "An electric lightning design.",
            rarity = SkateRarity.APEX
        ),
        ShopItem(
            "skate_titanium_x", "Titanium X", 5000, ShopItemType.SKATE,
            "skate_titanium.png",
            description = "A high-tech metallic finish.",
            rarity = SkateRarity.APEX
        ),
        ShopItem(
            "skate_neon_phantom", "Neon Phantom", 5500, ShopItemType.SKATE,
            "skate_neon_phantom.png",
            description = "A mysterious neon racing style.",
            rarity = SkateRarity.APEX
        ),

        // MYTHIC
        ShopItem(
            "skate_skull_ride", "Skull Ride", 6000, ShopItemType.SKATE,
            "skate_skull_ride.png",
            description = "A dark design built to stand out.",
            rarity = SkateRarity.MYTHIC
        ),
        ShopItem(
            "skate_cosmic_drift", "Cosmic Drift", 6500, ShopItemType.SKATE,
            "skate_cosmic_drift.png",
            description = "A galaxy-inspired legendary design.",
            rarity = SkateRarity.MYTHIC
        ),
        ShopItem(
            "skate_solar_flare", "Solar Flare", 7000, ShopItemType.SKATE,
            "skate_solar_flare.png",
            description = "A radiant celestial design.",
            rarity = SkateRarity.MYTHIC
        ),
        ShopItem(
            "skate_void_reaper", "Void Reaper", 7500, ShopItemType.SKATE,
            "skate_void_reaper.png",
            description = "A dark, crimson energy style.",
            rarity = SkateRarity.MYTHIC
        ),
        ShopItem(
            "skate_infinity", "Infinity", 9000, ShopItemType.SKATE,
            "skate_infinity.png",
            description = "A holographic rainbow finish.",
            rarity = SkateRarity.MYTHIC
        ),

        // CHARACTERS
        ShopItem(
            "char_basic",
            "Basic Character",
            0,
            ShopItemType.CHARACTER
        ),
        ShopItem(
            "char_skin_2",
            "Character Skin 2",
            50,
            ShopItemType.CHARACTER
        )
    )

    fun skate(id: String): ShopItem? =
        items.firstOrNull {
            it.id == id && it.type == ShopItemType.SKATE
        }

    fun item(id: String): ShopItem? =
        items.firstOrNull { it.id == id }

    fun skatesByRarity(rarity: SkateRarity): List<ShopItem> =
        items.filter {
            it.type == ShopItemType.SKATE && it.rarity == rarity
        }

    fun allSkates(): List<ShopItem> =
        items.filter { it.type == ShopItemType.SKATE }
}

class InventoryData(private val prefs: GamePreferences) {

    fun owns(id: String): Boolean {
        if (id in DEFAULT_OWNED) return true

        if (ShopCatalog.item(id) == null) return false

        return id in prefs.getStringSet(KEY_OWNED)
    }

    fun unlock(id: String) {
        require(ShopCatalog.item(id) != null) {
            "Unknown shop item ID: $id"
        }

        if (owns(id)) return

        prefs.putStringSet(
            KEY_OWNED,
            prefs.getStringSet(KEY_OWNED) + id
        )
    }

    fun ownedItems(): Set<String> =
        DEFAULT_OWNED + prefs.getStringSet(KEY_OWNED)

    private companion object {
        const val KEY_OWNED = "inventory_owned"

        val DEFAULT_OWNED = setOf(
            "skate_basic",
            "char_basic"
        )
    }
}
