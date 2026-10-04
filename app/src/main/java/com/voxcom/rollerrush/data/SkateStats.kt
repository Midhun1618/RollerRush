package com.voxcom.rollerrush.data

import com.voxcom.rollerrush.utils.Constants

/**
 * Upgrade levels. PlayerController / GameWorld read ONLY these derived values,
 * so a future shop can change the levels without touching movement code.
 */
data class SkateStats(
    val speedLevel: Int = 1,
    val jumpLevel: Int = 1,
    val controlLevel: Int = 1
) {
    /** Multiplies world scroll speed. */
    val speedMultiplier: Float get() = 1f + 0.08f * (speedLevel - 1)

    /** Peak jump height in world units. */
    val jumpHeight: Float get() = Constants.JUMP_HEIGHT_BASE * (1f + 0.05f * (jumpLevel - 1))

    /** How early a tap is remembered before landing (seconds). */
    val jumpBufferTime: Float get() = 0.10f + 0.03f * (controlLevel - 1)

    companion object {
        /** Placeholder mapping from equipped skate item to stats. */
        fun forSkate(skateId: String): SkateStats = when (skateId) {
            "skate_speed" -> SkateStats(speedLevel = 2)
            else -> SkateStats()
        }
    }
}
