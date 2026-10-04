package com.voxcom.rollerrush.data

import android.graphics.Color

/**
 * Which asset id is used for each customisable slot.
 * The renderer (AssetManager.createPlayerSprites) loads art by these ids, so new art
 * can be dropped in without touching gameplay code.
 */
data class CharacterData(
    val headSkin: String = "character_skin_default",
    val hairSkin: String = "hair_default",
    val torsoSkin: String = "shirt_default",
    val legSkin: String = "pants_default",
    val skateSkin: String = "skate_default"
)

/** Placeholder skin catalogue: ids + the flat colours used by the placeholder renderer. */
object SkinCatalog {
    val presets: List<CharacterData> = listOf(
        CharacterData(),
        CharacterData("character_skin_2", "hair_2", "shirt_2", "pants_2", "skate_2")
    )

    /** Inventory item that must be owned to equip each preset (same order as [presets]). */
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
        "skate_default" -> Color.rgb(231, 76, 60)
        "skate_2" -> Color.rgb(26, 188, 156)
        else -> Color.MAGENTA // unknown id: obvious placeholder colour
    }
}
