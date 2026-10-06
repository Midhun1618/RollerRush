package com.voxcom.rollerrush.systems

import android.graphics.RectF
import com.voxcom.rollerrush.entities.Obstacle
import com.voxcom.rollerrush.player.Player

object CollisionSystem {
    fun playerHitsObstacle(player: Player, obstacles: List<Obstacle>): Boolean {
        for (i in 0 until obstacles.size) {
            val o = obstacles[i]
            if (!o.active) continue
            if (RectF.intersects(player.bodyHitbox, o.hitbox) || RectF.intersects(player.feetHitbox, o.hitbox)) return true
        }
        return false
    }

    fun playerTouchesCircle(player: Player, cx: Float, cy: Float, r: Float): Boolean =
        circleHitsRect(cx, cy, r, player.bodyHitbox) || circleHitsRect(cx, cy, r, player.feetHitbox)

    fun circleHitsRect(cx: Float, cy: Float, r: Float, rect: RectF): Boolean {
        val nx = cx.coerceIn(rect.left, rect.right); val ny = cy.coerceIn(rect.top, rect.bottom)
        val dx = cx - nx; val dy = cy - ny
        return dx * dx + dy * dy <= r * r
    }
}
