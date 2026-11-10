
package com.voxcom.rollerrush.systems

import com.voxcom.rollerrush.utils.Constants
import kotlin.random.Random

/**
 * Ambient roadside pigeons.
 *
 * Decorative only:
 * - No collision or gameplay effects.
 * - First flock is present during the intro.
 * - Pigeons scatter as the player approaches.
 * - Later flocks spawn rarely.
 */
class PigeonSystem {

    class Pigeon {
        var active = false
        var flying = false

        var x = 0f
        var y = 0f
        var size = PIGEON_SIZE

        var idlePose = 0
        var idleTimer = 0f

        var flightTimer = 0f
        var flightDelay = 0f

        var horizontalSpeed = 360f
        var verticalSpeed = 180f
    }

    val birds = Array(MAX_PIGEONS) { Pigeon() }

    private var spawnTimer = 0f

    /**
     * Called when a run starts.
     *
     * Places the first flock relative to the intro building,
     * so the pigeons already exist before the intro animation.
     */
    fun reset(initialScrollX: Float) {

        birds.forEach {
            it.active = false
            it.flying = false
        }

        // The intro building starts at:
        // -scrollX + INTRO_BACKGROUND_OFFSET_X
        val buildingX =
            -initialScrollX + Constants.INTRO_BACKGROUND_OFFSET_X

        // Place the first flock inside the visible part of the building.
        spawnFlock(buildingX + INTRO_FLOCK_LOCAL_X)

        // Keep subsequent flocks rare.
        spawnTimer = Random.nextFloat() * 8f + 22f
    }

    fun update(
        dt: Float,
        worldSpeed: Float,
        playerX: Float,
        visibleLeft: Float,
        visibleRight: Float
    ) {

        // --------------------------------------------------------
        // UPDATE EXISTING PIGEONS
        // --------------------------------------------------------

        for (bird in birds) {

            if (!bird.active) continue

            if (!bird.flying) {

                // Subtle idle pose changes.
                bird.idleTimer -= dt

                if (bird.idleTimer <= 0f) {
                    bird.idlePose = Random.nextInt(3)
                    bird.idleTimer = Random.nextFloat() * 1.6f + 0.8f
                }

                // Follow the movement of the world.
                bird.x -= worldSpeed * dt

                // Scatter when the player approaches.
                if (bird.x - playerX <= TRIGGER_DISTANCE) {

                    bird.flying = true
                    bird.flightTimer = 0f

                    // Staggered takeoff.
                    bird.flightDelay = Random.nextFloat() * 0.22f

                    bird.horizontalSpeed =
                        Random.nextFloat() * 110f + 330f

                    bird.verticalSpeed =
                        Random.nextFloat() * 70f + 155f
                }

            } else {

                bird.flightTimer += dt

                if (bird.flightTimer >= bird.flightDelay) {

                    bird.x +=
                        (bird.horizontalSpeed - worldSpeed) * dt

                    bird.y -= bird.verticalSpeed * dt

                } else {

                    // Wait briefly before taking off.
                    bird.x -= worldSpeed * dt
                }
            }

            // Recycle birds outside the useful area.
            if (
                bird.x < visibleLeft - 120f ||
                bird.x > visibleRight + 420f ||
                bird.y < -120f
            ) {
                bird.active = false
            }
        }

        // --------------------------------------------------------
        // RARE SUBSEQUENT FLOCKS
        // --------------------------------------------------------

        spawnTimer -= dt

        if (spawnTimer <= 0f) {

            spawnFlock(
                visibleRight + Random.nextFloat() * 120f + 100f
            )

            spawnTimer = Random.nextFloat() * 8f + 22f
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

            // All pigeons use exactly the same size.
            bird.size = PIGEON_SIZE

            bird.x = startX + offset
            bird.y = Constants.GROUND_Y

            bird.idlePose = Random.nextInt(3)
            bird.idleTimer = Random.nextFloat() * 1.5f + 0.5f

            bird.flightTimer = 0f
            bird.flightDelay = 0f

            offset += Random.nextFloat() * 24f + 24f
        }
    }

    companion object {

        private const val MAX_PIGEONS = 16

        private const val PIGEON_SIZE = 42f

        private const val TRIGGER_DISTANCE = 190f

        // Position within the intro building.
        private const val INTRO_FLOCK_LOCAL_X = 580f
    }
}
