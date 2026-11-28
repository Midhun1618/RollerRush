package com.voxcom.rollerrush.player

import com.voxcom.rollerrush.entities.ContainerPlatform
import com.voxcom.rollerrush.utils.Constants

/**
 * Simple springboard-assisted container stunt.
 * No climbing, hanging, pull-up, flip, or scripted landing animation.
 * The player follows ballistic physics and may land only on a genuine
 * descending crossing of the container roof.
 */
class ContainerStuntController(
    private val player: Player
) {

    val cameraTarget = StuntCameraTarget()

    private enum class Phase { NONE, AIRBORNE, RIDING }

    private var phase = Phase.NONE
    private var target: ContainerPlatform? = null
    private var pendingImpact = 0f

    val active: Boolean
        get() = phase != Phase.NONE

    fun consumeCameraImpact(): Float {
        val value = pendingImpact
        pendingImpact = 0f
        return value
    }

    fun tryStart(platform: ContainerPlatform, springboardAuthorized: Boolean): Boolean {
        if (!springboardAuthorized || active || !player.grounded || player.sliding ||
            !platform.active || !platform.springboardLaunchConsumed
        ) return false

        target = platform
        phase = Phase.AIRBORNE
        player.stopSlide()
        player.grounded = false
        player.vy = -LAUNCH_VELOCITY
        pendingImpact = LAUNCH_IMPACT

        cameraTarget.zoom = 1.04f
        cameraTarget.focusDy = -12f
        cameraTarget.panX = -5f
        cameraTarget.tilt = 0f
        cameraTarget.response = 7f
        return true
    }

    fun update(dt: Float) {
        val platform = target ?: run {
            reset()
            return
        }

        if (!platform.active) {
            cancelAndFall()
            return
        }

        when (phase) {
            Phase.AIRBORNE -> updateFlight(dt, platform)
            Phase.RIDING -> updateRoofRide(platform)
            Phase.NONE -> Unit
        }
        updateCamera()
    }

    private fun updateFlight(dt: Float, platform: ContainerPlatform) {
        val previousBottom = player.bottomY
        val previousVelocity = player.vy

        // Integrate actual vertical motion. Do not animate or reposition the player.
        player.bottomY += player.vy * dt
        player.vy += Constants.GRAVITY * dt
        player.grounded = false

        val roofOverlap = platform.overlapsPlayer(player.x, LANDING_HALF_WIDTH)
        val crossedRoofWhileDescending = previousVelocity > 0f &&
                previousBottom <= platform.topY && player.bottomY >= platform.topY

        // Roof landing is allowed only after a real downward surface crossing.
        if (crossedRoofWhileDescending && roofOverlap) {
            landOnRoof(platform)
            return
        }

        // Once past the roof plane, never rescue/snap the player onto it.
        if (player.vy > 0f && player.bottomY > platform.topY + FAILED_LANDING_MARGIN) {
            cancelAndFall()
            return
        }

        // The moving container has passed the player.
        if (platform.right < player.x - PLAYER_HALF_WIDTH) {
            cancelAndFall()
        }
    }

    private fun landOnRoof(platform: ContainerPlatform) {
        // The only vertical snap: exact crossing of the roof while descending.
        player.bottomY = platform.topY
        player.vy = 0f
        player.grounded = true
        player.markLanded()
        phase = Phase.RIDING
        pendingImpact = LANDING_IMPACT
    }

    private fun updateRoofRide(platform: ContainerPlatform) {
        if (platform.overlapsPlayer(player.x, ROOF_SUPPORT_HALF_WIDTH)) {
            player.bottomY = platform.topY
            player.vy = 0f
            player.grounded = true
        } else {
            finishRide()
        }
    }

    private fun finishRide() {
        phase = Phase.NONE
        target = null
        player.grounded = false
        // Preserve velocity/position; normal physics resumes next frame.
        cameraTarget.neutral()
    }

    private fun cancelAndFall() {
        phase = Phase.NONE
        target = null
        player.grounded = false
        // Preserve x, bottomY and vy; do not teleport or manufacture velocity.
        cameraTarget.neutral()
    }

    /** PlayerAnimator owns all limb movement. */
    fun applyPose() = Unit

    private fun updateCamera() {
        when (phase) {
            Phase.AIRBORNE -> {
                cameraTarget.zoom = 1.04f
                cameraTarget.focusDy = -12f
                cameraTarget.panX = -5f
                cameraTarget.tilt = 0f
                cameraTarget.response = 7f
            }
            Phase.RIDING -> {
                cameraTarget.zoom = 1f
                cameraTarget.focusDy = 0f
                cameraTarget.panX = 0f
                cameraTarget.tilt = 0f
                cameraTarget.response = 5f
            }
            Phase.NONE -> cameraTarget.neutral()
        }
    }

    fun reset() {
        phase = Phase.NONE
        target = null
        pendingImpact = 0f
        player.setIntroRotation(0f)
        cameraTarget.neutral()
    }

    private companion object {
        const val LAUNCH_VELOCITY = 650f
        const val PLAYER_HALF_WIDTH = 18f
        const val LANDING_HALF_WIDTH = 8f
        const val ROOF_SUPPORT_HALF_WIDTH = 10f
        const val FAILED_LANDING_MARGIN = 12f
        const val LAUNCH_IMPACT = 4.5f
        const val LANDING_IMPACT = 5.5f
    }
}

/** Camera goals consumed by GameWorld and Camera. */
class StuntCameraTarget {
    var zoom = 1f
    var focusDy = 0f
    var panX = 0f
    var tilt = 0f
    var response = 4.5f

    fun neutral() {
        zoom = 1f
        focusDy = 0f
        panX = 0f
        tilt = 0f
        response = 4.5f
    }
}
