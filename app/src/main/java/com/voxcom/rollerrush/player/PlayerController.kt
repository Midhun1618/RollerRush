
package com.voxcom.rollerrush.player

import com.voxcom.rollerrush.data.SkateStats
import com.voxcom.rollerrush.entities.ContainerPlatform
import com.voxcom.rollerrush.utils.Constants
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.sqrt

class PlayerController(
    private val player: Player,
    var stats: SkateStats
) {

    private val jumpRequested = AtomicBoolean(false)
    private val slideRequested = AtomicBoolean(false)

    private var jumpBuffer = 0f
    private var jumpAnticipationTimer = -1f
    private var jumpAnticipationStarted = false

    companion object {
        private const val JUMP_CROUCH_DELAY = 0.10f
        private const val PLATFORM_HALF_WIDTH = 28f
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

    fun update(
        dt: Float,
        platform: ContainerPlatform? = null
    ) {
        if (player.crashed) {
            updateCrash(dt)
            return
        }

        // SLIDE INPUT
        if (
            slideRequested.getAndSet(false) &&
            player.grounded &&
            !player.sliding
        ) {
            player.startSlide(Constants.SLIDE_DURATION)
        }

        // JUMP BUFFER
        if (jumpRequested.getAndSet(false)) {
            jumpBuffer = stats.jumpBufferTime
        }

        jumpBuffer = max(0f, jumpBuffer - dt)

        // JUMP ANTICIPATION
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
                    player.vy = -sqrt(
                        2f * Constants.GRAVITY * stats.jumpHeight
                    )

                    player.grounded = false
                    jumpAnticipationTimer = -1f
                }
            }
        }

        // PHYSICS
        integrate(dt, platform)

        player.updateSlide(dt)
    }

    private fun integrate(
        dt: Float,
        platform: ContainerPlatform?
    ) {
        val previousBottomY = player.bottomY

        player.vy += Constants.GRAVITY * dt
        player.bottomY += player.vy * dt

        val overlapsPlatform =
            platform != null &&
                    player.x + PLATFORM_HALF_WIDTH > platform.x &&
                    player.x - PLATFORM_HALF_WIDTH < platform.right

        // Land on the container roof while descending.
        if (
            platform != null &&
            overlapsPlatform &&
            player.vy >= 0f &&
            previousBottomY <= platform.topY &&
            player.bottomY >= platform.topY
        ) {
            if (!player.grounded) {
                player.markLanded()
            }

            player.bottomY = platform.topY
            player.vy = 0f
            player.grounded = true
            return
        }

        // Stay on the container while it moves underneath the player.
        val standingOnPlatform =
            platform != null &&
                    overlapsPlatform &&
                    player.grounded &&
                    previousBottomY <= platform.topY + 1f

        val groundY =
            if (standingOnPlatform) {
                platform!!.topY
            } else {
                Constants.GROUND_Y
            }

        // Normal road landing or container departure.
        if (player.bottomY >= groundY) {

            if (!player.grounded) {
                player.markLanded()
            }

            player.bottomY = groundY
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
