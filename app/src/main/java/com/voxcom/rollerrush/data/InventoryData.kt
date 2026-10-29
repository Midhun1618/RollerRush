
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
            particleName = "particle_redstreet.png",
            description = "Your original street setup.",
            rarity = SkateRarity.SPEED
        ),
        ShopItem(
            "skate_ice", "Ice Glide", 50, ShopItemType.SKATE,
            "skate2.png",
            particleName = "particle_ice.png",
            description = "Smooth and cold.",
            rarity = SkateRarity.SPEED
        ),
        ShopItem(
            "skate_sunset_rush", "Sunset Rush", 90, ShopItemType.SKATE,
            "skate_sunset_rush.webp",
            particleName = "particle_sunset_rush.png",
            description = "Bright colors and racing energy.",
            rarity = SkateRarity.SPEED
        ),
        ShopItem(
            "skate_pixel_pop", "Pixel Pop", 11, ShopItemType.SKATE,
            "skate_pixel_pop.png",
            particleName = "particle_pixel_pop.png",
            description = "An arcade-inspired design.",
            rarity = SkateRarity.SPEED
        ),
        ShopItem(
            "skate_night_shift", "Night Shift", 13, ShopItemType.SKATE,
            "skate_night_shift.webp",
            particleName = "particle_night_shift.png",
            description = "A stealthy black-and-silver style.",
            rarity = SkateRarity.SPEED
        ),

        // PRO
        ShopItem(
            "skate_aero", "Aero Blue", 18, ShopItemType.SKATE,
            "skate3.png",
            particleName = "particle_aero.png",
            description = "Lightweight racing setup.",
            rarity = SkateRarity.PRO
        ),
        ShopItem(
            "skate_nitro_edge", "Nitro Edge", 21, ShopItemType.SKATE,
            "skate_nitro_edge.webp",
            particleName = "particle_nitro_edge.png",
            description = "Motorsport-inspired racing style.",
            rarity = SkateRarity.PRO
        ),
        ShopItem(
            "skate_sonic_wave", "Sonic Wave", 24, ShopItemType.SKATE,
            "skate_sonic_wave.webp",
            particleName = "particle_sonic_wave.png",
            description = "A streamlined speed design.",
            rarity = SkateRarity.PRO
        ),
        ShopItem(
            "skate_gold_standard", "Gold Standard", 27, ShopItemType.SKATE,
            "skate_gold_standard.webp",
            particleName = "particle_gold_standard.png",
            description = "Premium metallic detailing.",
            rarity = SkateRarity.PRO
        ),
        ShopItem(
            "skate_drift_king", "Drift King", 33, ShopItemType.SKATE,
            "skate_drift_king.webp",
            particleName = "particle_drift_king.png",
            description = "Made for a drifting-inspired look.",
            rarity = SkateRarity.PRO
        ),
        ShopItem(
            "skate_cross_rider", "Cross Rider", 20, ShopItemType.SKATE,
            "skate_cross_rider.webp",
            particleName = "particle_cross_rider.png",
            description = "A bold street-racing design.",
            rarity = SkateRarity.PRO
        ),

        // APEX
        ShopItem(
            "skate_volt", "Volt Runner", 30, ShopItemType.SKATE,
            "skate5.png",
            particleName = "particle_volt.png",
            description = "Electric racing wheels.",
            rarity = SkateRarity.APEX
        ),
        ShopItem(
            "skate_street_tox", "Street Tox", 38, ShopItemType.SKATE,
            "skate_street_tox.webp",
            particleName = "particle_street_tox.png",
            description = "An aggressive urban skate style.",
            rarity = SkateRarity.APEX
        ),
        ShopItem(
            "skate_inferno", "Inferno", 40, ShopItemType.SKATE,
            "skate4.png",
            particleName = "particle_inferno.png",
            description = "Built for aggressive runs.",
            rarity = SkateRarity.APEX
        ),
        ShopItem(
            "skate_thunderbolt", "Thunderbolt", 45, ShopItemType.SKATE,
            "skate_thunderbolt.png",
            particleName = "particle_thunderbolt.png",
            description = "An electric lightning design.",
            rarity = SkateRarity.APEX
        ),
        ShopItem(
            "skate_titanium_x", "Titanium X", 50, ShopItemType.SKATE,
            "skate_titanium.webp",
            particleName = "particle_titanium_x.png",
            description = "A high-tech metallic finish.",
            rarity = SkateRarity.APEX
        ),
        ShopItem(
            "skate_neon_phantom", "Neon Phantom", 55, ShopItemType.SKATE,
            "skate_neon_phantom.png",
            particleName = "particle_neon_phantom.png",
            description = "A mysterious neon racing style.",
            rarity = SkateRarity.APEX
        ),

        // MYTHIC
        ShopItem(
            "skate_skull_ride", "Skull Ride", 60, ShopItemType.SKATE,
            "skate_skull_ride.webp",
            particleName = "particle_skull_ride.png",
            description = "A dark design built to stand out.",
            rarity = SkateRarity.MYTHIC
        ),
        ShopItem(
            "skate_cosmic_drift", "Cosmic Drift", 65, ShopItemType.SKATE,
            "skate_cosmic_drift.webp",
            particleName = "particle_cosmic_drift.png",
            description = "A galaxy-inspired legendary design.",
            rarity = SkateRarity.MYTHIC
        ),
        ShopItem(
            "skate_solar_flare", "Solar Flare", 70, ShopItemType.SKATE,
            "skate_solar_flare.webp",
            particleName = "particle_solar_flare.png",
            description = "A radiant celestial design.",
            rarity = SkateRarity.MYTHIC
        ),
        ShopItem(
            "skate_void_reaper", "Void Reaper", 75, ShopItemType.SKATE,
            "skate_void_reaper.webp",
            particleName = "particle_void_reaper.png",
            description = "A dark, crimson energy style.",
            rarity = SkateRarity.MYTHIC
        ),
        ShopItem(
            "skate_infinity", "Infinity", 90, ShopItemType.SKATE,
            "skate_infinity.webp",
            particleName = "particle_infinity.png",
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
