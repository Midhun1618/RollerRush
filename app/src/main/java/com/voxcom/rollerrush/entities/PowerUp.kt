package com.voxcom.rollerrush.entities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.voxcom.rollerrush.utils.AssetManager
import kotlin.math.sin

enum class PowerUpType { MAGNET, SHIELD, SPEED }

class PowerUp : GameEntity() {
    var type = PowerUpType.SPEED
        private set
    private var t = 0f

    fun init(type: PowerUpType, cx: Float, cy: Float) {
        this.type = type; width = 20f; height = 20f
        x = cx - 10f; y = cy - 10f; t = 0f; active = true; updateBounds()
    }

    override fun update(dt: Float, worldSpeed: Float) {
        super.update(dt, worldSpeed)
        t += dt
    }

    override fun draw(canvas: Canvas, assets: AssetManager, paint: Paint) {
        val cx = x + 10f; val cy = y + 10f + sin(t * 6f) * 2f
        val old = paint.color; val oldStyle = paint.style
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(255, 170, 25)
        canvas.drawCircle(cx, cy, 10f, paint)
        paint.color = Color.WHITE
        val path = Path()
        path.moveTo(cx + 2f, cy - 7f); path.lineTo(cx - 4f, cy + 1f); path.lineTo(cx, cy + 1f)
        path.lineTo(cx - 2f, cy + 7f); path.lineTo(cx + 5f, cy - 2f); path.lineTo(cx + 1f, cy - 2f); path.close()
        canvas.drawPath(path, paint)
        paint.color = old; paint.style = oldStyle
    }
}
