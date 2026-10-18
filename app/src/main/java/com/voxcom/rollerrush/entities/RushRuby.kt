package com.voxcom.rollerrush.entities

import android.graphics.Canvas
import android.graphics.Paint
import com.voxcom.rollerrush.utils.AssetManager
import com.voxcom.rollerrush.utils.Constants
import kotlin.math.abs
import kotlin.math.cos

class RushRuby : GameEntity() {

    private var spin = 0f

    val centerX: Float
        get() = x + width / 2f

    val centerY: Float
        get() = y + height / 2f

    fun init(cx: Float, cy: Float) {
        width = Constants.COIN_RADIUS * 2.6f
        height = width

        x = cx - width / 2f
        y = cy - height / 2f

        spin = 0f
        active = true
        updateBounds()
    }

    override fun update(dt: Float, worldSpeed: Float) {
        super.update(dt, worldSpeed)
        spin += dt * 4f
    }

    override fun draw(
        canvas: Canvas,
        assets: AssetManager,
        paint: Paint
    ) {
        // Slight horizontal squash gives the ruby a rotating 3D effect.
        val drawnWidth =
            width * (0.55f + 0.45f * abs(cos(spin)))

        dst.set(
            centerX - drawnWidth / 2f,
            y,
            centerX + drawnWidth / 2f,
            y + height
        )

        canvas.drawBitmap(
            assets.rushRubyBitmap,
            null,
            dst,
            paint
        )
    }
}