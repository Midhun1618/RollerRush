package com.voxcom.rollerrush.player

import com.voxcom.rollerrush.entities.ContainerPlatform
import com.voxcom.rollerrush.utils.Constants

/**
 * Controls the springboard launch and container-roof landing.
 *
 * Flight uses normal vertical physics.
 * Roof landing is permitted only during a valid downward crossing.
 */
class ContainerStuntController(
    private val player: Player
) {

    val cameraTarget = StuntCameraTarget()

    private enum class Phase {
        NONE,
        AIRBORNE,
        RIDING
    }

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

    fun tryStart(
        platform: ContainerPlatform,
        springboardAuthorized: Boolean
    ): Boolean {

        if (
            !springboardAuthorized ||
            active ||
            !player.grounded ||
            player.sliding ||
            !platform.active ||
            !platform.springboardLaunchConsumed
        ) {
            return false
        }

        target = platform
        phase = Phase.AIRBORNE

        player.stopSlide()
        player.grounded = false

        // Launch using normal ballistic physics.
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

    private fun updateFlight(
        dt: Float,
        platform: ContainerPlatform
    ) {

        val previousBottom = player.bottomY
        val previousVelocity = player.vy

        // Actual vertical movement.
        player.bottomY += player.vy * dt
        player.vy += Constants.GRAVITY * dt

        player.grounded = false

        // A narrow footprint is required for a valid roof landing.
        val validRoofOverlap =
            platform.overlapsPlayer(
                player.x,
                LANDING_HALF_WIDTH
            )

        // The player must cross the roof surface while descending.
        val crossedRoofFromAbove =
            previousVelocity > 0f &&
                    previousBottom <= platform.topY &&
                    player.bottomY >= platform.topY

        if (crossedRoofFromAbove && validRoofOverlap) {
            landOnRoof(platform)
            return
        }

        // Once the player has passed the roof height, no rescue snap.
        // Return control to normal player physics.
        if (
            player.vy > 0f &&
            player.bottomY > platform.topY + FAILED_LANDING_MARGIN
        ) {
            cancelAndFall()
            return
        }

        // The container has passed the player horizontally.
        if (platform.right < player.x - PLAYER_HALF_WIDTH) {
            cancelAndFall()
        }
    }

    private fun landOnRoof(platform: ContainerPlatform) {

        // This snap is allowed ONLY after a valid downward crossing.
        player.bottomY = platform.topY
        player.vy = 0f
        player.grounded = true

        player.markLanded()

        phase = Phase.RIDING
        pendingImpact = LANDING_IMPACT
    }

    private fun updateRoofRide(platform: ContainerPlatform) {

        // Narrow support check prevents edge/side overlap
        // from falsely maintaining roof support.
        if (
            platform.overlapsPlayer(
                player.x,
                ROOF_SUPPORT_HALF_WIDTH
            )
        ) {

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
        player.vy = 0f

        cameraTarget.neutral()
    }

    private fun cancelAndFall() {

        phase = Phase.NONE
        target = null

        player.grounded = false

        // Do not artificially reposition the player.
        // Normal physics takes over from the current velocity.

        cameraTarget.neutral()
    }

    /**
     * Normal PlayerAnimator handles the limbs.
     */
    fun applyPose() {
        // Intentionally empty.
    }

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

        // Landing requires a smaller overlap than ordinary collision.
        const val LANDING_HALF_WIDTH = 8f

        // Roof support should not extend too far beyond the player's center.
        const val ROOF_SUPPORT_HALF_WIDTH = 10f

        const val FAILED_LANDING_MARGIN = 12f

        const val LAUNCH_IMPACT = 4.5f
        const val LANDING_IMPACT = 5.5f
    }
}

/**
 * Camera goals consumed by GameWorld and Camera.
 */
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
