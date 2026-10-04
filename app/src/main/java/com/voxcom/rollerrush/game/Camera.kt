package com.voxcom.rollerrush.game

import android.graphics.Canvas
import com.voxcom.rollerrush.utils.Constants
import kotlin.math.min

/**
 * COORDINATE CONVERSION (world <-> screen).
 *
 * Gameplay is written in WORLD UNITS. The world is always WORLD_HEIGHT units tall and
 * anchored to the bottom of the screen. We scale uniformly:
 *
 *      scale   = min(screenH / WORLD_HEIGHT, screenW / MIN_VIEW_WIDTH)
 *      screenX = worldX * scale
 *      screenY = worldY * scale + offsetY
 *
 *  - Wide phones (19.5:9, 21:9): height decides the scale; viewWidth grows (more world visible).
 *  - 4:3 tablets: width decides the scale; offsetY pushes the world down and sky fills the top.
 * Proportions of gameplay objects therefore never change between devices.
 */
class Camera {
    var screenWidth = 1f
        private set
    var screenHeight = 1f
        private set
    var scale = 1f
        private set
    var offsetY = 0f
        private set
    /** Visible world width in world units (>= MIN_VIEW_WIDTH). Spawn/cull use this. */
    @Volatile var viewWidth = Constants.MIN_VIEW_WIDTH
        private set

    fun onSizeChanged(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        screenWidth = w.toFloat()
        screenHeight = h.toFloat()
        scale = min(screenHeight / Constants.WORLD_HEIGHT, screenWidth / Constants.MIN_VIEW_WIDTH)
        offsetY = screenHeight - Constants.WORLD_HEIGHT * scale
        viewWidth = screenWidth / scale
    }

    /** After this call the canvas draws in world units. Caller wraps in save()/restore(). */
    fun applyTo(canvas: Canvas) {
        canvas.translate(0f, offsetY)
        canvas.scale(scale, scale)
    }

    fun worldToScreenX(wx: Float) = wx * scale
    fun worldToScreenY(wy: Float) = wy * scale + offsetY
    fun screenToWorldX(sx: Float) = sx / scale
    fun screenToWorldY(sy: Float) = (sy - offsetY) / scale
}
