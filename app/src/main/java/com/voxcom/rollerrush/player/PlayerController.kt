package com.voxcom.rollerrush.player

import com.voxcom.rollerrush.data.SkateStats
import com.voxcom.rollerrush.utils.Constants
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.sqrt

class PlayerController(private val player: Player, var stats: SkateStats) {

    // Touch events arrive on the UI thread, the game loop runs on its own thread.
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

    fun update(dt: Float) {
        if (player.crashed) {
            updateCrash(dt)
            return
        }

        // -------------------------------------------------------------
        // SLIDE INPUT
        // -------------------------------------------------------------
        if (
            slideRequested.getAndSet(false) &&
            player.grounded &&
            !player.sliding
        ) {
            player.startSlide(Constants.SLIDE_DURATION)
        }

        // -------------------------------------------------------------
        // JUMP BUFFER
        // -------------------------------------------------------------
        if (jumpRequested.getAndSet(false)) {
            jumpBuffer = stats.jumpBufferTime
        }

        jumpBuffer = max(0f, jumpBuffer - dt)

        // -------------------------------------------------------------
        // JUMP ANTICIPATION / CROUCH
        // -------------------------------------------------------------

        // Start crouching when a valid jump is requested.
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

        // Wait for the crouch to finish before applying jump physics.
        if (jumpAnticipationTimer >= 0f) {

            if (!player.grounded || player.sliding) {
                // Cancel anticipation if the player can no longer jump.
                jumpAnticipationTimer = -1f
            } else {
                jumpAnticipationTimer -= dt

                if (jumpAnticipationTimer <= 0f) {

                    // ACTUAL TAKEOFF
                    player.vy = -sqrt(
                        2f * Constants.GRAVITY * stats.jumpHeight
                    )

                    player.grounded = false

                    jumpAnticipationTimer = -1f
                }
            }
        }

        // -------------------------------------------------------------
        // PHYSICS
        // -------------------------------------------------------------
        integrate(dt)
        player.updateSlide(dt)
    }

    private fun integrate(dt: Float) {
        player.vy += Constants.GRAVITY * dt
        player.bottomY += player.vy * dt
        if (player.bottomY >= Constants.GROUND_Y) {          // ground detection
            if (!player.grounded) player.markLanded()        // animator reacts to this
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
