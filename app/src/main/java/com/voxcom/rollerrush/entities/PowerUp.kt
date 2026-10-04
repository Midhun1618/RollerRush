package com.voxcom.rollerrush.entities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.voxcom.rollerrush.utils.AssetManager

enum class PowerUpType { MAGNET, SHIELD }

/**
 * Foundation only: pooled + scrolled + drawn, but SpawnSystem does not spawn it yet and
 * no effect is applied on pickup. Extend here (and in CoinSystem/GameWorld) later.
 */
class PowerUp : GameEntity() {
    var type = PowerUpType.MAGNET
        private set

    fun init(type: PowerUpType, cx: Float, cy: Float) {
        this.type = type
        width = 18f; height = 18f
        x = cx - 9f; y = cy - 9f
        active = true
        updateBounds()
    }

    override fun draw(canvas: Canvas, assets: AssetManager, paint: Paint) {
        val old = paint.color
        paint.color = if (type == PowerUpType.MAGNET) Color.CYAN else Color.GREEN
        canvas.drawCircle(x + 9f, y + 9f, 9f, paint)
        paint.color = old
    }
}
