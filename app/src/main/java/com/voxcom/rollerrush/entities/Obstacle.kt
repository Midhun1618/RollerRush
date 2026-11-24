package com.voxcom.rollerrush.entities

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.voxcom.rollerrush.utils.AssetManager
import com.voxcom.rollerrush.utils.Constants
import kotlin.math.abs
import kotlin.math.sin

enum class ObstacleType {
    GROUND,
    TALL,
    MOVING,
    OVERHEAD,
    SHOPPING_CART,
    BARRICADE
}

class Obstacle : GameEntity() {

    var type = ObstacleType.GROUND
        private set

    val hitbox = RectF()

    private var t = 0f

    // Separate collision state from visibility/activity.
    var collisionEnabled = true

    // Used by the rolling tyre and shopping cart artwork.
    var rotationDegrees = 0f
        private set

    private val tyreRotationSpeed = -240f

    fun init(type: ObstacleType, spawnX: Float) {
        this.type = type

        when (type) {
            ObstacleType.GROUND -> {
                width = 22f
                height = 26f
            }

            ObstacleType.TALL -> {
                width = 20f
                height = 58f
            }

            ObstacleType.MOVING -> {
                width = 32f
                height = 32f
            }

            ObstacleType.OVERHEAD -> {
                width = 38f
                height = 24f
            }

            ObstacleType.SHOPPING_CART -> {
                width = Constants.SHOPPING_CART_WIDTH
                height = Constants.SHOPPING_CART_HEIGHT
            }

            ObstacleType.BARRICADE -> {
                width = Constants.BARRICADE_WIDTH
                height = Constants.BARRICADE_HEIGHT
            }
        }

        x = spawnX
        y = Constants.GROUND_Y - height

        t = 0f
        rotationDegrees = 0f
        active = true
        collisionEnabled = true

        refreshBoxes()
    }

    override fun update(dt: Float, worldSpeed: Float) {
        when (type) {
            ObstacleType.MOVING -> {
                x -= (worldSpeed + Constants.MOVING_EXTRA_SPEED) * dt
                t += dt

                y = Constants.GROUND_Y - height -
                        abs(sin(t * 3.2f)) * Constants.MOVING_HOP_HEIGHT

                rotationDegrees =
                    (rotationDegrees + tyreRotationSpeed * dt) % 360f
            }

            ObstacleType.SHOPPING_CART -> {
                x -= (worldSpeed + Constants.CART_EXTRA_SPEED) * dt
                t += dt

                y = Constants.GROUND_Y - height +
                        sin(t * 8f) * Constants.CART_BOB_AMOUNT

                rotationDegrees =
                    sin(t * 5.5f) * Constants.CART_TILT_AMOUNT
            }

            else -> {
                x -= worldSpeed * dt
                y = Constants.GROUND_Y - height
            }
        }

        refreshBoxes()
    }

    private fun refreshBoxes() {
        updateBounds()

        when (type) {
            ObstacleType.BARRICADE -> {
                hitbox.set(
                    x + width * 0.12f,
                    y + height * 0.025f,
                    x + width * 0.88f,
                    y + height * 0.235f
                )
            }

            ObstacleType.SHOPPING_CART -> {
                hitbox.set(
                    x + width * 0.08f,
                    y + height * 0.12f,
                    x + width * 0.96f,
                    y + height * 0.98f
                )
            }

            else -> {
                hitbox.set(bounds)
                hitbox.inset(2f, 2f)
            }
        }
    }

    override fun draw(
        canvas: Canvas,
        assets: AssetManager,
        paint: Paint
    ) {
        dst.set(x, y, x + width, y + height)

        when (type) {
            ObstacleType.MOVING,
            ObstacleType.SHOPPING_CART -> {
                canvas.save()

                canvas.rotate(
                    rotationDegrees,
                    dst.centerX(),
                    dst.centerY()
                )

                canvas.drawBitmap(
                    assets.obstacleBitmap(type),
                    null,
                    dst,
                    paint
                )

                canvas.restore()
            }

            else -> {
                canvas.drawBitmap(
                    assets.obstacleBitmap(type),
                    null,
                    dst,
                    paint
                )
            }
        }
    }
}