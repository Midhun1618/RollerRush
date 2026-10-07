package com.voxcom.rollerrush.data

import android.graphics.Color

/**
 * Which asset id is used for each customisable slot.
 * Skate art is selected from the equipped shop skate id.
 */
data class CharacterData(
    val headSkin: String = "character_skin_default",
    val hairSkin: String = "hair_default",
    val torsoSkin: String = "shirt_default",
    val legSkin: String = "pants_default",
    val skateSkin: String = "skate_basic"
)

/** Character presets. The skate slot is overridden by PlayerData.equippedSkateId. */
object SkinCatalog {
    val presets: List<CharacterData> = listOf(
        CharacterData(),
        CharacterData("character_skin_2", "hair_2", "shirt_2", "pants_2", "skate_basic")
    )

    /** Inventory item that must be owned to equip each character preset. */
    val presetItemIds: List<String> = listOf("char_basic", "char_skin_2")

    fun colorFor(id: String): Int = when (id) {
        "character_skin_default" -> Color.rgb(241, 194, 125)
        "character_skin_2" -> Color.rgb(141, 85, 36)
        "hair_default" -> Color.rgb(59, 42, 32)
        "hair_2" -> Color.rgb(229, 184, 11)
        "shirt_default" -> Color.rgb(46, 134, 222)
        "shirt_2" -> Color.rgb(230, 126, 34)
        "pants_default" -> Color.rgb(44, 62, 80)
        "pants_2" -> Color.rgb(142, 68, 173)
        "skate_basic" -> Color.rgb(231, 76, 60)
        "skate_ice" -> Color.rgb(210, 240, 255)
        "skate_aero" -> Color.rgb(35, 130, 200)
        "skate_inferno" -> Color.rgb(35, 35, 45)
        "skate_volt" -> Color.rgb(235, 235, 235)
        // Backwards compatibility with the old placeholder ids.
        "skate_default" -> Color.rgb(231, 76, 60)
        "skate_2" -> Color.rgb(26, 188, 156)
        else -> Color.MAGENTA
    }
}
