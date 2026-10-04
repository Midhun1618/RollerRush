package com.voxcom.rollerrush.systems

import android.graphics.RectF
import com.voxcom.rollerrush.entities.Obstacle
import com.voxcom.rollerrush.player.Player

/**
 * Simple rectangle / circle collision. Stateless, allocation-free.
 *
 * The player has TWO gameplay boxes (see Player.updateHitboxes):
 *  - body box (torso + head column)
 *  - feet box (skates), so low obstacles are hit by the skates and a well-timed jump clears them.
 * Neither equals the sprite bounds.
 */
object CollisionSystem {

    fun playerHitsObstacle(player: Player, obstacles: List<Obstacle>): Boolean {
        for (i in 0 until obstacles.size) {
            val o = obstacles[i]
            if (!o.active) continue
            if (RectF.intersects(player.bodyHitbox, o.hitbox) ||
                RectF.intersects(player.feetHitbox, o.hitbox)) return true
        }
        return false
    }

    /** Circle (coin pickup radius) vs. player boxes. */
    fun playerTouchesCircle(player: Player, cx: Float, cy: Float, r: Float): Boolean =
        circleHitsRect(cx, cy, r, player.bodyHitbox) || circleHitsRect(cx, cy, r, player.feetHitbox)

    fun circleHitsRect(cx: Float, cy: Float, r: Float, rect: RectF): Boolean {
        val nx = cx.coerceIn(rect.left, rect.right)   // nearest point on rect to circle centre
        val ny = cy.coerceIn(rect.top, rect.bottom)
        val dx = cx - nx
        val dy = cy - ny
        return dx * dx + dy * dy <= r * r
    }
}
