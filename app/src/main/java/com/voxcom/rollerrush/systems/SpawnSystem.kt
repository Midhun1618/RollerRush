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
 * Procedural obstacle and reward spawning.
 *
 * Obstacles are selected from the types unlocked at the current distance.
 * Entities are acquired from GameWorld's fixed pools.
 */
class SpawnSystem {
    private val rng = Random(System.nanoTime())

    private val earlyTypes = arrayOf(
        ObstacleType.GROUND
    )

    private val midTypes = arrayOf(
        ObstacleType.GROUND,
        ObstacleType.TALL
    )

    private val lateTypes = arrayOf(
        ObstacleType.GROUND,
        ObstacleType.TALL,
        ObstacleType.MOVING
    )

    private var timer = Constants.FIRST_SPAWN_DELAY
    private var powerUpTimer = Constants.SPEED_BOOST_SPAWN_MIN
    private var nextRushRubyDistance = 0

    private var mysteryBubbleSpawned = false
    private var nextMysteryBubbleDistance = 0

    fun reset() {
        timer = Constants.FIRST_SPAWN_DELAY
        powerUpTimer = Constants.SPEED_BOOST_SPAWN_MIN
        nextRushRubyDistance = rng.nextInt(1000, 2001)
        mysteryBubbleSpawned = false
        nextMysteryBubbleDistance = rng.nextInt(202, 303)
    }

    fun update(dt: Float, world: GameWorld) {
        timer -= dt
        powerUpTimer -= dt

        // Use the camera's visible edge so spawning remains correct during zoom/pan.
        val right = maxOf(world.camera.viewWidth, world.camera.visibleRight)

        // Rush Ruby spawning.
        if (world.scoreSystem.distanceMeters >= nextRushRubyDistance) {
            val ruby = world.acquireRushRuby()

            if (ruby != null) {
                ruby.init(
                    right + Constants.SPAWN_MARGIN + 100f,
                    Constants.GROUND_Y - 72f
                )

                nextRushRubyDistance =
                    world.scoreSystem.distanceMeters + rng.nextInt(1000, 2001)
            }
        }

        // Speed booster spawning.
        if (powerUpTimer <= 0f) {
            if (world.scoreSystem.distanceMeters >= Constants.SPEED_BOOST_UNLOCK_METERS) {
                val powerUp = world.acquirePowerUp()

                if (powerUp != null) {
                    powerUp.init(
                        com.voxcom.rollerrush.entities.PowerUpType.SPEED,
                        right + Constants.SPAWN_MARGIN + 80f,
                        Constants.GROUND_Y - 72f
                    )
                }
            }

            powerUpTimer = Constants.SPEED_BOOST_SPAWN_MIN +
                    rng.nextFloat() *
                    (Constants.SPEED_BOOST_SPAWN_MAX - Constants.SPEED_BOOST_SPAWN_MIN)
        }

        if (timer > 0f) return

        val interval = world.difficulty.nextSpawnInterval(rng)
        timer += interval

        val spawnX = right + Constants.SPAWN_MARGIN

        // Keep ordinary obstacles away from the container stunt area.
        if (world.isContainerNear(spawnX, 180f)) {
            return
        }

        val meters = world.scoreSystem.distanceMeters

        val baseTypes = when {
            meters >= Constants.MOVING_UNLOCK_METERS -> lateTypes
            meters >= Constants.TALL_UNLOCK_METERS -> midTypes
            else -> earlyTypes
        }

        val type = chooseObstacleType(meters, baseTypes)

        val obstacle = world.acquireObstacle() ?: return
        obstacle.init(type, spawnX)

        // Reward placement matches the action needed for the obstacle:
        // jumpable obstacles get an arc; slide/rolling hazards get a low line.
        when (type) {
            ObstacleType.GROUND,
            ObstacleType.TALL,
            ObstacleType.SHOPPING_CART -> {
                if (rng.nextFloat() < 0.65f) {
                    spawnCoinArc(world, obstacle)
                } else {
                    spawnCoinLine(
                        world,
                        spawnX + 0.5f * world.worldSpeed * interval
                    )
                }
            }

            ObstacleType.MOVING -> {
                spawnCoinLine(
                    world,
                    spawnX + 0.5f * world.worldSpeed * interval
                )
            }

            ObstacleType.OVERHEAD,
            ObstacleType.BARRICADE -> {
                spawnCoinLine(
                    world,
                    spawnX + obstacle.width * 0.5f
                )
            }
        }

        // Mystery bubble appears once per run.
        if (
            !mysteryBubbleSpawned &&
            world.scoreSystem.distanceMeters >= nextMysteryBubbleDistance
        ) {
            world.mysteryBubble.init(
                right + Constants.SPAWN_MARGIN + 80f,
                Constants.GROUND_Y - 65f
            )
            mysteryBubbleSpawned = true
        }
    }

    private fun chooseObstacleType(
        meters: Int,
        baseTypes: Array<ObstacleType>
    ): ObstacleType {
        // New obstacle unlocks are intentionally weighted so they add variety
        // without flooding the player with unfamiliar hazards.
        if (
            meters >= Constants.BARRICADE_UNLOCK_METERS &&
            rng.nextFloat() < Constants.BARRICADE_SPAWN_CHANCE
        ) {
            return ObstacleType.BARRICADE
        }

        if (
            meters >= Constants.CART_UNLOCK_METERS &&
            rng.nextFloat() < Constants.CART_SPAWN_CHANCE
        ) {
            return ObstacleType.SHOPPING_CART
        }

        if (
            meters >= Constants.OVERHEAD_UNLOCK_METERS &&
            rng.nextFloat() < 0.12f
        ) {
            return ObstacleType.OVERHEAD
        }

        return baseTypes[rng.nextInt(baseTypes.size)]
    }

    /** Coins following a jump arc above an obstacle. */
    private fun spawnCoinArc(world: GameWorld, obstacle: Obstacle) {
        val count = 5
        val spacing = 22f
        val centerX = obstacle.x + obstacle.width / 2f
        val peak = min(obstacle.height + 70f, 130f)
        val base = 28f

        for (i in 0 until count) {
            val coin = world.acquireCoin() ?: return
            val fraction = i / (count - 1).toFloat()
            val height = base + (peak - base) *
                    sin(fraction * PI.toFloat())

            coin.init(
                centerX + (i - count / 2) * spacing,
                Constants.GROUND_Y - height,
                i * 0.5f
            )
        }
    }

    /** Low coin line for skating/sliding through a hazard. */
    private fun spawnCoinLine(world: GameWorld, centerX: Float) {
        val count = 5

        for (i in 0 until count) {
            val coin = world.acquireCoin() ?: return
            coin.init(
                centerX + (i - count / 2) * 24f,
                Constants.GROUND_Y - 26f,
                i * 0.5f
            )
        }
    }
}
