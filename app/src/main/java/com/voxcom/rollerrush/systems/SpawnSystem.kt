package com.voxcom.rollerrush.systems

import com.voxcom.rollerrush.entities.Obstacle
import com.voxcom.rollerrush.entities.ObstacleType
import com.voxcom.rollerrush.game.GameWorld
import com.voxcom.rollerrush.utils.Constants
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * PROCEDURAL SPAWNING.
 *
 * A countdown timer (seconds, so it is frame-rate independent and freezes while paused)
 * triggers a "spawn event" just off the right edge of the screen:
 *   1. pick an obstacle type from those unlocked by distance (variety grows over time)
 *   2. place it from the pool
 *   3. decorate it with coins: an ARC over the obstacle (reward for jumping) or a LINE on the
 *      ground halfway to the next obstacle (reward for skating).
 * Entities come from fixed pools, so no objects are allocated while playing.
 */
class SpawnSystem {
    private val rng = Random(System.nanoTime())
    private val normalTypes = arrayOf(ObstacleType.GROUND, ObstacleType.TALL, ObstacleType.MOVING)
    private var timer = Constants.FIRST_SPAWN_DELAY
    private var powerUpTimer = Constants.SPEED_BOOST_SPAWN_MIN

    fun reset() { timer = Constants.FIRST_SPAWN_DELAY; powerUpTimer = Constants.SPEED_BOOST_SPAWN_MIN }

    fun update(dt: Float, world: GameWorld) {
        timer -= dt
        powerUpTimer -= dt
        if (powerUpTimer <= 0f) {
            if (world.scoreSystem.distanceMeters >= Constants.SPEED_BOOST_UNLOCK_METERS) {
                val pu = world.acquirePowerUp()
                if (pu != null) pu.init(com.voxcom.rollerrush.entities.PowerUpType.SPEED,
                    world.camera.viewWidth + Constants.SPAWN_MARGIN + 80f, Constants.GROUND_Y - 72f)
            }
            powerUpTimer = Constants.SPEED_BOOST_SPAWN_MIN +
                rng.nextFloat() * (Constants.SPEED_BOOST_SPAWN_MAX - Constants.SPEED_BOOST_SPAWN_MIN)
        }

        if (timer > 0f) return

        val interval = world.difficulty.nextSpawnInterval(rng)
        timer += interval

        val spawnX = world.camera.viewWidth + Constants.SPAWN_MARGIN
        val meters = world.scoreSystem.distanceMeters
        val maxIndex = when {
            meters >= Constants.MOVING_UNLOCK_METERS -> 2
            meters >= Constants.TALL_UNLOCK_METERS -> 1
            else -> 0
        }

        // Overhead hazards are deliberately uncommon: they teach the player to
        // use the new swipe-down slide without making the run feel random.
        val type = if (meters >= Constants.OVERHEAD_UNLOCK_METERS && rng.nextFloat() < 0.20f) {
            ObstacleType.OVERHEAD
        } else {
            normalTypes[rng.nextInt(maxIndex + 1)]
        }

        val obstacle = world.acquireObstacle() ?: return
        obstacle.init(type, spawnX)

        if (type != ObstacleType.MOVING && rng.nextFloat() < 0.6f) {
            spawnCoinArc(world, obstacle)
        } else {
            // Midway between this obstacle and the next one (they are speed * interval apart).
            spawnCoinLine(world, spawnX + 0.5f * world.worldSpeed * interval)
        }
    }

    /** Coins following a jump arc above the obstacle. Peak is high enough that jumping is needed. */
    private fun spawnCoinArc(world: GameWorld, o: Obstacle) {
        val n = 5
        val spacing = 22f
        val centerX = o.x + o.width / 2f
        val peak = min(o.height + 70f, 130f)
        val base = 28f
        for (i in 0 until n) {
            val coin = world.acquireCoin() ?: return
            val frac = i / (n - 1).toFloat()
            val h = base + (peak - base) * sin(frac * PI.toFloat())
            coin.init(centerX + (i - n / 2) * spacing, Constants.GROUND_Y - h, i * 0.5f)
        }
    }

    private fun spawnCoinLine(world: GameWorld, centerX: Float) {
        val n = 5
        for (i in 0 until n) {
            val coin = world.acquireCoin() ?: return
            coin.init(centerX + (i - n / 2) * 24f, Constants.GROUND_Y - 26f, i * 0.5f)
        }
    }
}
