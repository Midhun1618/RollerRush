package com.voxcom.rollerrush.systems

import com.voxcom.rollerrush.game.GameWorld
import com.voxcom.rollerrush.utils.Constants
import kotlin.random.Random

class ContainerSystem {

    private val random = Random(System.nanoTime())

    private var spawnTimer = Constants.CONTAINER_FIRST_DELAY

    fun reset() {
        spawnTimer = Constants.CONTAINER_FIRST_DELAY
    }

    fun update(dt: Float, world: GameWorld) {

        // Move existing containers
        for (platform in world.containerPlatforms) {
            if (!platform.active) continue

            platform.update(dt, world.worldSpeed)

            if (platform.right < world.camera.visibleLeft - 60f) {
                platform.active = false
            }
        }

        // Unlock containers after reaching the required distance
        if (world.scoreSystem.distanceMeters < Constants.CONTAINER_UNLOCK_METERS) {
            return
        }

        spawnTimer -= dt

        if (spawnTimer > 0f) return

        val spawnX =
            maxOf(world.camera.viewWidth, world.camera.visibleRight) +
                    Constants.SPAWN_MARGIN + 100f

        val sections = random.nextInt(1, 4)

        val containerWidth =
            sections * Constants.CONTAINER_SEGMENT_WIDTH

        // Avoid placing a container directly on an existing obstacle
        val obstacleConflict = world.obstacles.any { obstacle ->
            obstacle.active &&
                    obstacle.x < spawnX + containerWidth + 120f &&
                    obstacle.x + obstacle.width > spawnX - 120f
        }

        // Keep container sequences separated
        val existingContainer = world.containerPlatforms.any { platform ->
            platform.active &&
                    platform.right > world.camera.visibleLeft
        }

        if (obstacleConflict || existingContainer) {
            spawnTimer = 2.5f
            return
        }

        val platform = world.acquireContainerPlatform()

        if (platform != null) {
            platform.init(spawnX, sections)
        }

        spawnTimer =
            Constants.CONTAINER_SPAWN_MIN_SECONDS +
                    random.nextFloat() *
                    (Constants.CONTAINER_SPAWN_MAX_SECONDS -
                            Constants.CONTAINER_SPAWN_MIN_SECONDS)
    }
}
