package com.voxcom.rollerrush.player

import com.voxcom.rollerrush.data.SkateStats
import com.voxcom.rollerrush.entities.ContainerPlatform
import com.voxcom.rollerrush.utils.Constants
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.sqrt

class PlayerController(private val player: Player, var stats: SkateStats) {

    private val jumpRequested = AtomicBoolean(false)
    private val slideRequested = AtomicBoolean(false)

    private var jumpBuffer = 0f
    private var jumpAnticipationTimer = -1f
    private var jumpAnticipationStarted = false

    companion object {
        private const val JUMP_CROUCH_DELAY = 0.10f
    }

    fun requestJump() = jumpRequested.set(true)
    fun requestSlide() = slideRequested.set(true)

    fun reset() {
        jumpRequested.set(false)
        slideRequested.set(false)
        jumpBuffer = 0f
        jumpAnticipationTimer = -1f
        jumpAnticipationStarted = false
    }

    fun update(dt: Float, platform: ContainerPlatform? = null) {
        if (player.crashed) {
            updateCrash(dt)
            return
        }

        if (slideRequested.getAndSet(false) && player.grounded && !player.sliding) {
            player.startSlide(Constants.SLIDE_DURATION)
        }

        if (jumpRequested.getAndSet(false)) {
            jumpBuffer = stats.jumpBufferTime
        }

        jumpBuffer = max(0f, jumpBuffer - dt)

        if (
            jumpAnticipationTimer < 0f &&
            jumpBuffer > 0f &&
            player.grounded &&
            !player.sliding
        ) {
            jumpAnticipationTimer = JUMP_CROUCH_DELAY
            jumpBuffer = 0f
            jumpAnticipationStarted = true
        }

        if (jumpAnticipationTimer >= 0f) {
            if (!player.grounded || player.sliding) {
                jumpAnticipationTimer = -1f
            } else {
                jumpAnticipationTimer -= dt

                if (jumpAnticipationTimer <= 0f) {
                    player.vy = -sqrt(2f * Constants.GRAVITY * stats.jumpHeight)
                    player.grounded = false
                    jumpAnticipationTimer = -1f
                }
            }
        }

        integrate(dt, platform)
        player.updateSlide(dt)
    }

    /**
     * Roof collision is a landing-only surface:
     * the player must approach it from above while descending.
     * Horizontal overlap alone must never snap a ground-level player onto the roof.
     */
    private fun integrate(dt: Float, platform: ContainerPlatform?) {
        val previousBottomY = player.bottomY
        val previousVy = player.vy

        player.vy += Constants.GRAVITY * dt
        player.bottomY += player.vy * dt

        val validPlatform = platform?.takeIf { it.active }

        val roofY = validPlatform?.topY
        val crossedRoofFromAbove =
            roofY != null &&
                    previousVy >= 0f &&
                    previousBottomY <= roofY &&
                    player.bottomY >= roofY &&
                    validPlatform.overlapsPlayer(player.x, 18f)

        if (crossedRoofFromAbove) {
            if (!player.grounded) {
                player.markLanded()
            }
            player.bottomY = roofY!!
            player.vy = 0f
            player.grounded = true
            return
        }

        // The street remains the fallback surface. The container roof is not
        // considered if the player is below it, beside it, or moving upward.
        if (player.vy >= 0f && player.bottomY >= Constants.GROUND_Y) {
            if (!player.grounded) {
                player.markLanded()
            }
            player.bottomY = Constants.GROUND_Y
            player.vy = 0f
            player.grounded = true
        } else {
            player.grounded = false
        }
    }

    private fun updateCrash(dt: Float) {
        player.crashVx *= (1f - 2f * dt).coerceAtLeast(0f)
        player.x += player.crashVx * dt

        player.vy += Constants.GRAVITY * dt
        player.bottomY += player.vy * dt

        if (player.bottomY >= Constants.GROUND_Y) {
            player.bottomY = Constants.GROUND_Y
            player.vy = 0f
        }
    }

    fun consumeJumpAnticipationStarted(): Boolean {
        val started = jumpAnticipationStarted
        jumpAnticipationStarted = false
        return started
    }
}
