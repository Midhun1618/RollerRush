package com.voxcom.rollerrush.entities

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import com.voxcom.rollerrush.utils.AssetManager
import com.voxcom.rollerrush.utils.Constants
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class RushRuby : GameEntity() {

    private var spin = 0f



    val centerX: Float
        get() = x + width / 2f

    val centerY: Float
        get() = y + height / 2f

    private var glowRotation = 0f
    private var pulseTime = 0f

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
    }

    fun init(cx: Float, cy: Float) {
        width = Constants.COIN_RADIUS * 2.6f
        height = width


        x = cx - width / 2f
        y = cy - height / 2f

        spin = 0f
        active = true

        glowRotation = 0f
        pulseTime = 0f

        updateBounds()
    }

    override fun update(dt: Float, worldSpeed: Float) {
        super.update(dt, worldSpeed)

        // Only the lighting rotates; the ruby itself remains still.
        glowRotation = (glowRotation + dt * 45f) % 360f
        pulseTime += dt * 2.5f
    }

    override fun draw(
        canvas: Canvas,
        assets: AssetManager,
        paint: Paint
    ) {
        val cx = centerX
        val cy = centerY


        // Pulsating light: scale and opacity change smoothly.
        val pulse = (sin(pulseTime) + 1f) * 0.5f
        val glowScale = 0.88f + pulse * 0.32f
        glowPaint.alpha = (100 + pulse * 155f).toInt().coerceIn(0, 255)

        // Draw the lighting image BEHIND the ruby.
        val glowSize = width * 2.5f * glowScale

        canvas.save()
        canvas.rotate(glowRotation, cx, cy)
        dst.set(
            cx - glowSize / 2f,
            cy - glowSize / 2f,
            cx + glowSize / 2f,
            cy + glowSize / 2f
        )
        canvas.drawBitmap(assets.rushRubyGlowBitmap, null, dst, glowPaint)
        canvas.restore()

        // Draw the ruby normally: no rotation, no swiveling, no squashing.
        dst.set(x, y, x + width, y + height)
        canvas.drawBitmap(assets.rushRubyBitmap, null, dst, paint)
    }
}