package com.voxcom.rollerrush.systems

import com.voxcom.rollerrush.utils.Constants
import kotlin.random.Random

/**
 * Ambient roadside pigeons.
 *
 * They are decorative only:
 * - No collision or gameplay effects.
 * - Idle near the roadside.
 * - Fly diagonally up/right when the player approaches.
 */
class PigeonSystem {

    class Pigeon {
        var active = false
        var flying = false

        var x = 0f
        var y = 0f
        var size = 52f

        var idlePose = 0
        var idleTimer = 0f

        var flightTimer = 0f
        var flightDelay = 0f
        var flightFrameOffset = 0

        var horizontalSpeed = 260f
        var verticalSpeed = 125f
    }

    val birds = Array(MAX_PIGEONS) { Pigeon() }

    private var spawnTimer = FIRST_SPAWN_DELAY

    fun reset() {
        birds.forEach {
            it.active = false
            it.flying = false
        }

        spawnTimer = FIRST_SPAWN_DELAY
    }

    fun update(
        dt: Float,
        worldSpeed: Float,
        playerX: Float,
        visibleRight: Float
    ) {
        // Update existing pigeons.
        for (bird in birds) {
            if (!bird.active) continue

            if (!bird.flying) {
                // Idle pose changes.
                bird.idleTimer -= dt

                if (bird.idleTimer <= 0f) {
                    bird.idlePose = Random.nextInt(3)
                    bird.idleTimer = Random.nextFloat() * 1.6f + 0.8f
                }

                // The world scrolls towards the player.
                bird.x -= worldSpeed * dt

                // Scatter when the player approaches.
                if (bird.x - playerX <= TRIGGER_DISTANCE) {
                    bird.flying = true
                    bird.flightTimer = 0f
                    bird.flightDelay = Random.nextFloat() * 0.22f

                    bird.flightFrameOffset = Random.nextInt(4)
                    bird.horizontalSpeed = Random.nextFloat() * 90f + 230f
                    bird.verticalSpeed = Random.nextFloat() * 55f + 105f
                }
            } else {
                bird.flightTimer += dt

                // Slight delay gives the flock a natural staggered takeoff.
                if (bird.flightTimer >= bird.flightDelay) {
                    bird.x += (bird.horizontalSpeed - worldSpeed) * dt
                    bird.y -= bird.verticalSpeed * dt
                } else {
                    bird.x -= worldSpeed * dt
                }
            }

            // Recycle birds after they leave the visible area.
            if (
                bird.x < -100f ||
                bird.x > visibleRight + 220f ||
                bird.y < -100f
            ) {
                bird.active = false
            }
        }

        // Spawn the next flock.
        spawnTimer -= dt

        if (spawnTimer <= 0f) {
            spawnFlock(visibleRight + Random.nextFloat() * 100f + 80f)

            spawnTimer = Random.nextFloat() * 3.0f + 4.5f
        }
    }

    private fun spawnFlock(startX: Float) {
        val available = birds.filter { !it.active }
        if (available.isEmpty()) return

        val count = Random.nextInt(2, 5)
            .coerceAtMost(available.size)

        var offset = 0f

        for (i in 0 until count) {
            val bird = available[i]

            bird.active = true
            bird.flying = false

            bird.size = Random.nextFloat() * 13f + 45f

            bird.x = startX + offset
            bird.y = Constants.GROUND_Y - 5f

            bird.idlePose = Random.nextInt(3)
            bird.idleTimer = Random.nextFloat() * 1.5f + 0.5f

            bird.flightTimer = 0f
            bird.flightDelay = 0f
            bird.flightFrameOffset = 0

            offset += Random.nextFloat() * 24f + 24f
        }
    }

    companion object {
        private const val MAX_PIGEONS = 16
        private const val FIRST_SPAWN_DELAY = 0.85f
        private const val TRIGGER_DISTANCE = 180f
    }
}
