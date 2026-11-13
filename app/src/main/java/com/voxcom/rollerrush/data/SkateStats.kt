package com.voxcom.rollerrush.data

import com.voxcom.rollerrush.utils.Constants
import kotlin.math.roundToInt

/**
 * Gameplay levels retain their original scales to preserve existing balance.
 * The rating properties expose those levels on a 0–10 scale for the shop UI.
 */
data class SkateStats(
    val speedLevel: Int = 1,
    val jumpLevel: Int = 1,
    val controlLevel: Int = 1
) {

    // Existing gameplay calculations — intentionally unchanged.
    val speedMultiplier: Float
        get() = 1f + 0.08f * (speedLevel - 1)

    val jumpHeight: Float
        get() = Constants.JUMP_HEIGHT_BASE *
                (1f + 0.05f * (jumpLevel - 1))

    val jumpBufferTime: Float
        get() = 0.10f + 0.03f * (controlLevel - 1)

    // Display ratings.
    val speedRating: Int
        get() = speedLevel.coerceIn(1, 5) * 2

    val jumpRating: Int
        get() = when (jumpLevel.coerceIn(1, 3)) {
            1 -> 3
            2 -> 6
            else -> 10
        }

    val controlRating: Int
        get() = when (controlLevel.coerceIn(1, 3)) {
            1 -> 3
            2 -> 6
            else -> 10
        }

    val overallRating: Int
        get() = (
                (speedRating + jumpRating + controlRating) / 3f
                ).roundToInt()

    companion object {

        fun forSkate(skateId: String): SkateStats = when (skateId) {
            "skate_ice" -> SkateStats(2, 1, 1)
            "skate_aero" -> SkateStats(3, 1, 2)
            "skate_inferno" -> SkateStats(4, 2, 1)
            "skate_volt" -> SkateStats(4, 2, 2)

            "skate_street_venom" -> SkateStats(2, 1, 2)
            "skate_sunset_rush" -> SkateStats(2, 2, 1)
            "skate_pixel_pop" -> SkateStats(2, 2, 2)
            "skate_night_shift" -> SkateStats(3, 1, 1)

            "skate_nitro_edge" -> SkateStats(3, 2, 2)
            "skate_sonic_wave" -> SkateStats(4, 1, 2)
            "skate_gold_standard" -> SkateStats(3, 2, 3)
            "skate_drift_king" -> SkateStats(3, 3, 2)
            "skate_cross_rider" -> SkateStats(3, 2, 2)

            "skate_thunderbolt" -> SkateStats(4, 2, 2)
            "skate_titanium_x" -> SkateStats(4, 3, 2)
            "skate_neon_phantom" -> SkateStats(4, 2, 3)
            "skate_street_tox" -> SkateStats(4, 2, 2)

            "skate_cosmic_drift" -> SkateStats(5, 2, 2)
            "skate_solar_flare" -> SkateStats(4, 3, 3)
            "skate_void_reaper" -> SkateStats(5, 3, 2)
            "skate_skull_ride" -> SkateStats(5, 2, 3)
            "skate_infinity" -> SkateStats(5, 3, 3)

            "skate_speed" -> SkateStats(2, 1, 1) // Legacy ID

            else -> SkateStats()
        }
    }
}