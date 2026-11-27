package com.voxcom.rollerrush.systems

import android.graphics.RectF
import com.voxcom.rollerrush.entities.ContainerPlatform
import com.voxcom.rollerrush.entities.Obstacle
import com.voxcom.rollerrush.player.Player
import com.voxcom.rollerrush.utils.Constants

object CollisionSystem {

    fun playerHitsObstacle(
        player: Player,
        obstacles: List<Obstacle>
    ): Boolean {

        for (obstacle in obstacles) {
            if (!obstacle.active || !obstacle.collisionEnabled) continue

            if (
                RectF.intersects(player.bodyHitbox, obstacle.hitbox) ||
                RectF.intersects(player.feetHitbox, obstacle.hitbox)
            ) {
                return true
            }
        }

        return false
    }

    fun playerHitsContainerSide(
        player: Player,
        platforms: List<ContainerPlatform>
    ): Boolean {

        for (platform in platforms) {
            if (!platform.active) continue

            if (
                RectF.intersects(player.bodyHitbox, platform.sideHitbox) ||
                RectF.intersects(player.feetHitbox, platform.sideHitbox)
            ) {
                return true
            }
        }

        return false
    }

    fun playerTouchesCircle(
        player: Player,
        cx: Float,
        cy: Float,
        r: Float
    ): Boolean {

        return circleHitsRect(cx, cy, r, player.bodyHitbox) ||
                circleHitsRect(cx, cy, r, player.feetHitbox)
    }

    fun circleHitsRect(
        cx: Float,
        cy: Float,
        r: Float,
        rect: RectF
    ): Boolean {

        val nx = cx.coerceIn(rect.left, rect.right)
        val ny = cy.coerceIn(rect.top, rect.bottom)

        val dx = cx - nx
        val dy = cy - ny

        return dx * dx + dy * dy <= r * r
    }

    /**
     * Springboard trigger:
     * Only the central portion of the board is valid.
     * Player must be grounded, not sliding, and at ground level.
     */
    fun playerTouchesSpringboard(
        player: Player,
        platform: ContainerPlatform
    ): Boolean {

        if (!platform.active || !player.grounded || player.sliding) {
            return false
        }

        if (player.bottomY < Constants.GROUND_Y - 5f) {
            return false
        }

        val board = platform.springboardHitbox

        // Restrict the trigger zone to the central 50% of the board.
        val centerZone = RectF(
            board.left + board.width() * 0.25f,
            board.top,
            board.right - board.width() * 0.25f,
            board.bottom
        )

        return RectF.intersects(
            player.feetHitbox,
            centerZone
        )
    }
}
