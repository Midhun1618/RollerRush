package com.voxcom.rollerrush.systems

import com.voxcom.rollerrush.entities.ContainerPlatform
import com.voxcom.rollerrush.game.GameWorld
import com.voxcom.rollerrush.utils.Constants
import kotlin.random.Random

class ContainerSystem {

    private val random = Random(System.nanoTime())
    private var spawnTimer = Constants.CONTAINER_FIRST_DELAY

    private var platforms: List<ContainerPlatform> = emptyList()

    fun reset() {
        spawnTimer = Constants.CONTAINER_FIRST_DELAY
        platforms = emptyList()
    }

    fun update(dt: Float, world: GameWorld) {

        platforms = world.containerPlatforms

        for (platform in platforms) {
            if (!platform.active) continue

            platform.update(dt, world.worldSpeed)

            if (platform.isOffScreenLeft(world.camera.visibleLeft)) {
                platform.active = false
            }
        }

        spawnTimer -= dt

        if (world.scoreSystem.distanceMeters < Constants.CONTAINER_UNLOCK_METERS) {
            return
        }

        if (spawnTimer > 0f) return
        if (platforms.any { it.active }) return

        val visibleRight = maxOf(
            world.camera.viewWidth,
            world.camera.visibleRight
        )

        val spawnX = visibleRight + Constants.SPAWN_MARGIN + 120f
        val sections = random.nextInt(1, 4)
        val newWidth = sections * Constants.CONTAINER_SEGMENT_WIDTH

        val blocked = world.obstacles.any { obstacle ->
            obstacle.active &&
                    obstacle.x < spawnX + newWidth &&
                    obstacle.x + obstacle.width > spawnX - 80f
        }

        if (blocked) {
            spawnTimer = 2f
            return
        }

        val platform = world.acquireContainerPlatform()

        if (platform != null) {
            platform.init(spawnX, sections)

            spawnTimer =
                Constants.CONTAINER_SPAWN_MIN_SECONDS +
                        random.nextFloat() *
                        (Constants.CONTAINER_SPAWN_MAX_SECONDS -
                                Constants.CONTAINER_SPAWN_MIN_SECONDS)
        }
    }

    fun findStuntTarget(playerX: Float): ContainerPlatform? {
        return platforms.firstOrNull { platform ->
            if (!platform.active) {
                false
            } else {
                val distance = platform.x - playerX
                distance in Constants.CONTAINER_STUNT_MIN_DISTANCE..
                        Constants.CONTAINER_STUNT_MAX_DISTANCE
            }
        }
    }

    fun platformUnderPlayer(playerX: Float): ContainerPlatform? {
        return platforms.firstOrNull { platform ->
            platform.active && platform.overlapsPlayer(playerX, 18f)
        }
    }

    fun isContainerNear(x: Float, margin: Float): Boolean {
        return platforms.any { platform ->
            platform.active &&
                    x >= platform.x - margin &&
                    x <= platform.right + margin
        }
    }
}
