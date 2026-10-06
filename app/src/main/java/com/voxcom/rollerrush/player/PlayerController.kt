package com.voxcom.rollerrush.player

import com.voxcom.rollerrush.data.SkateStats
import com.voxcom.rollerrush.utils.Constants
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Player movement: gravity, jumping, ground detection, landing.
 * Reads every tunable from [SkateStats], so upgrades never require changes here.
 * All motion is deltaTime based.
 */
class PlayerController(private val player: Player, var stats: SkateStats) {

    // Touch events arrive on the UI thread, the game loop runs on its own thread.
    private val jumpRequested = AtomicBoolean(false)
    private val slideRequested = AtomicBoolean(false)
    private var jumpBuffer = 0f

    fun requestJump() = jumpRequested.set(true)
    fun requestSlide() = slideRequested.set(true)

    fun reset() {
        jumpRequested.set(false)
        slideRequested.set(false)
        jumpBuffer = 0f
    }

    fun update(dt: Float) {
        if (player.crashed) { updateCrash(dt); return }

        if (slideRequested.getAndSet(false) && player.grounded && !player.sliding) {
            player.startSlide(Constants.SLIDE_DURATION)
        }

        // A tap is remembered for a short time, so tapping slightly BEFORE landing still jumps.
        if (jumpRequested.getAndSet(false)) jumpBuffer = stats.jumpBufferTime
        jumpBuffer = max(0f, jumpBuffer - dt)

        // Only grounded players can jump -> no infinite air jumping.
        // A slide has priority over a buffered jump.
        if (jumpBuffer > 0f && player.grounded && !player.sliding) {
            player.vy = -sqrt(2f * Constants.GRAVITY * stats.jumpHeight)
            player.grounded = false
            jumpBuffer = 0f
        }

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
}
