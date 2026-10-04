package com.voxcom.rollerrush.systems

import com.voxcom.rollerrush.utils.Constants
import kotlin.math.min
import kotlin.random.Random

/** Ramps speed and spawn frequency over time. All numbers live in Constants. */
class DifficultySystem {
    var elapsed = 0f
        private set
    var speed = Constants.BASE_SPEED
        private set

    /** 0 at the start .. 1 at MAX_SPEED. */
    val level: Float get() =
        ((speed - Constants.BASE_SPEED) / (Constants.MAX_SPEED - Constants.BASE_SPEED)).coerceIn(0f, 1f)

    fun reset() { elapsed = 0f; speed = Constants.BASE_SPEED }

    fun update(dt: Float) {
        elapsed += dt
        speed = min(Constants.MAX_SPEED, Constants.BASE_SPEED + Constants.SPEED_INCREASE * elapsed)
    }

    /** Seconds until the next obstacle: shrinks with difficulty, with random jitter. */
    fun nextSpawnInterval(rng: Random): Float {
        val base = Constants.SPAWN_INTERVAL_START +
            (Constants.SPAWN_INTERVAL_MIN - Constants.SPAWN_INTERVAL_START) * level
        val jitter = 1f + (rng.nextFloat() * 2f - 1f) * Constants.SPAWN_JITTER
        return base * jitter
    }
}
