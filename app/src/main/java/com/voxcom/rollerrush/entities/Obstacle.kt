package com.voxcom.rollerrush.entities

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.voxcom.rollerrush.utils.AssetManager
import com.voxcom.rollerrush.utils.Constants
import kotlin.math.abs
import kotlin.math.sin

enum class ObstacleType { GROUND, TALL, MOVING }

class Obstacle : GameEntity() {
    var type = ObstacleType.GROUND
        private set
    /** Slightly smaller than the art so near-misses feel fair. */
    val hitbox = RectF()
    private var t = 0f

    fun init(type: ObstacleType, spawnX: Float) {
        this.type = type
        when (type) {
            ObstacleType.GROUND -> { width = 22f; height = 26f }
            ObstacleType.TALL -> { width = 20f; height = 58f }
            ObstacleType.MOVING -> { width = 24f; height = 24f }
        }
        x = spawnX
        y = Constants.GROUND_Y - height
        t = 0f
        active = true
        refreshBoxes()
    }

    override fun update(dt: Float, worldSpeed: Float) {
        if (type == ObstacleType.MOVING) {
            // Placeholder "moving" behaviour: approaches faster than the world and hops.
            x -= (worldSpeed + Constants.MOVING_EXTRA_SPEED) * dt
            t += dt
            y = Constants.GROUND_Y - height - abs(sin(t * 3.2f)) * Constants.MOVING_HOP_HEIGHT
        } else {
            x -= worldSpeed * dt
        }
        refreshBoxes()
    }

    private fun refreshBoxes() {
        updateBounds()
        hitbox.set(bounds)
        hitbox.inset(2f, 2f)
    }

    override fun draw(canvas: Canvas, assets: AssetManager, paint: Paint) {
        dst.set(x, y, x + width, y + height)
        canvas.drawBitmap(assets.obstacleBitmap(type), null, dst, paint)
    }
}
