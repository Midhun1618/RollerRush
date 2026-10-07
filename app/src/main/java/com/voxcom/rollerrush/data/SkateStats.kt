package com.voxcom.rollerrush.data

import com.voxcom.rollerrush.utils.Constants

data class SkateStats(
    val speedLevel: Int = 1,
    val jumpLevel: Int = 1,
    val controlLevel: Int = 1
) {
    val speedMultiplier: Float get() = 1f + 0.08f * (speedLevel - 1)
    val jumpHeight: Float get() = Constants.JUMP_HEIGHT_BASE * (1f + 0.05f * (jumpLevel - 1))
    val jumpBufferTime: Float get() = 0.10f + 0.03f * (controlLevel - 1)

    companion object {
        fun forSkate(skateId: String): SkateStats = when (skateId) {
            "skate_ice" -> SkateStats(speedLevel = 2)
            "skate_aero" -> SkateStats(speedLevel = 3, controlLevel = 2)
            "skate_inferno" -> SkateStats(speedLevel = 4, jumpLevel = 2)
            "skate_volt" -> SkateStats(speedLevel = 5, jumpLevel = 2, controlLevel = 2)
            "skate_speed" -> SkateStats(speedLevel = 2) // old id compatibility
            else -> SkateStats()
        }
    }
}
