package com.voxcom.rollerrush.entities

import android.graphics.Canvas
import android.graphics.Paint
import com.voxcom.rollerrush.utils.AssetManager
import com.voxcom.rollerrush.utils.Constants
import kotlin.math.abs
import kotlin.math.cos

class Coin : GameEntity() {
    private var spin = 0f
    val centerX: Float get() = x + width / 2f
    val centerY: Float get() = y + height / 2f

    fun init(cx: Float, cy: Float, spinOffset: Float) {
        width = Constants.COIN_RADIUS * 2f
        height = width
        x = cx - Constants.COIN_RADIUS
        y = cy - Constants.COIN_RADIUS
        spin = spinOffset
        active = true
        updateBounds()
    }

    override fun update(dt: Float, worldSpeed: Float) {
        super.update(dt, worldSpeed)
        spin += dt * 6f
    }

    override fun draw(canvas: Canvas, assets: AssetManager, paint: Paint) {
        // Fake 3D spin: squash the width with |cos|.
        val w = width * (0.25f + 0.75f * abs(cos(spin)))
        dst.set(centerX - w / 2f, y, centerX + w / 2f, y + height)
        canvas.drawBitmap(assets.coinBitmap, null, dst, paint)
    }
}
