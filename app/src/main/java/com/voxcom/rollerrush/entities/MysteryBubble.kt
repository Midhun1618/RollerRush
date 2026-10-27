package com.voxcom.rollerrush.entities

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.PI
import kotlin.math.sin
import com.voxcom.rollerrush.utils.AssetManager

class MysteryBubble : GameEntity() {

    private var elapsed = 0f
    private var baseY = 0f
    private var rotation = 0f

    val centerX: Float
        get() = x + width / 2f

    val centerY: Float
        get() = y + height / 2f

    val rotationDegrees: Float
        get() = rotation

    fun init(cx: Float, cy: Float, size: Float = 32f) {
        width = size
        height = size
        x = cx - size / 2f
        baseY = cy - size / 2f
        y = baseY
        elapsed = 0f
        rotation = 0f
        active = true
        updateBounds()
    }

    override fun update(dt: Float, worldSpeed: Float) {
        x -= worldSpeed * dt
        elapsed += dt

        // Gentle floating motion: one complete cycle every ~2.8 seconds.
        y = baseY + sin(elapsed * (2.0 * PI / 4.8).toFloat()) * 9f

        // Gentle rotation in both directions.
        rotation = sin(elapsed * (2.0 * PI / 10.6).toFloat()) * 7f

        updateBounds()
    }

    override fun draw(
        canvas: Canvas,
        assets: AssetManager,
        paint: Paint
    ) {
        if (!active) return

        val cx = centerX
        val cy = centerY

        canvas.save()
        canvas.rotate(rotation, cx, cy)

        dst.set(x, y, x + width, y + height)
        canvas.drawBitmap(
            assets.mysteryBubbleBitmap,
            null,
            dst,
            paint
        )

        canvas.restore()
    }


    fun drawBubble(
        canvas: Canvas,
        bitmap: Bitmap,
        paint: Paint
    ) {
        if (!active || bitmap.isRecycled) return

        val cx = centerX
        val cy = centerY

        canvas.save()
        canvas.rotate(rotation, cx, cy)
        dst.set(x, y, x + width, y + height)
        canvas.drawBitmap(bitmap, null, dst, paint)
        canvas.restore()
    }
}
