package com.voxcom.rollerrush.entities

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.voxcom.rollerrush.utils.AssetManager

/**
 * Base for everything that scrolls toward the player. Entities are POOLED:
 * they are created once and re-used by flipping [active], so nothing is allocated mid-run.
 * (x, y) = top-left corner in world units.
 */
abstract class GameEntity {
    var x = 0f
    var y = 0f
    var width = 0f
    var height = 0f
    var active = false
    val bounds = RectF()
    protected val dst = RectF()

    open fun update(dt: Float, worldSpeed: Float) {
        x -= worldSpeed * dt            // world moves toward the player
        updateBounds()
    }

    protected fun updateBounds() = bounds.set(x, y, x + width, y + height)

    /** True once fully off the left edge. */
    fun isOffScreenLeft(): Boolean = x + width < -40f

    abstract fun draw(canvas: Canvas, assets: AssetManager, paint: Paint)
}
