
package com.voxcom.rollerrush.player

import com.voxcom.rollerrush.animation.SkatingAnimation
import com.voxcom.rollerrush.entities.ContainerPlatform
import com.voxcom.rollerrush.utils.Constants
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

class ContainerStuntController(
    private val player: Player
) {

    private enum class Phase {
        NONE,
        REACH,
        GRAB,
        SWING,
        RELEASE,
        HERO
    }

    private var phase = Phase.NONE
    private var timer = 0f

    private var target: ContainerPlatform? = null

    private var startX = 0f
    private var startBottomY = 0f
    private var releaseStartY = 0f

    val active: Boolean
        get() = phase != Phase.NONE

    fun reset() {
        phase = Phase.NONE
        timer = 0f
        target = null
    }

    fun tryStart(platform: ContainerPlatform): Boolean {

        if (active || !player.grounded || player.sliding || !platform.active) {
            return false
        }

        val distance = platform.x - player.x

        if (distance !in Constants.CONTAINER_STUNT_MIN_DISTANCE..
            Constants.CONTAINER_STUNT_MAX_DISTANCE) {
            return false
        }

        target = platform
        phase = Phase.REACH
        timer = 0f

        startX = player.x
        startBottomY = player.bottomY

        player.stopSlide()

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

        timer += dt

        when (phase) {

            Phase.REACH -> {
                val t = smooth(timer / REACH_DURATION)

                player.x = startX + 5f * t
                player.bottomY = lerp(
                    startBottomY,
                    platform.topY + 44f,
                    t
                )

                player.vy = -360f
                player.grounded = false

                if (timer >= REACH_DURATION) {
                    phase = Phase.GRAB
                    timer = 0f
                }
            }

            Phase.GRAB -> {
                // Hang from the top-left edge.
                player.bottomY = platform.topY + 44f
                player.vy = 0f
                player.grounded = false

                // Wait until the container edge reaches the hands.
                if (platform.x <= player.x + 42f) {
                    phase = Phase.SWING
                    timer = 0f
                } else if (platform.x < player.x - 30f) {
                    cancelAndFall()
                }
            }

            Phase.SWING -> {
                val t = smooth(timer / SWING_DURATION)

                // Pull the body up and swing the legs forward.
                player.x = startX + 5f + 20f * t
                player.bottomY = lerp(
                    platform.topY + 44f,
                    platform.topY - 12f,
                    t
                )

                player.vy = -240f
                player.grounded = false

                if (timer >= SWING_DURATION) {
                    releaseStartY = player.bottomY
                    phase = Phase.RELEASE
                    timer = 0f
                }
            }

            Phase.RELEASE -> {
                val t = (timer / RELEASE_DURATION).coerceIn(0f, 1f)

                // Swing momentum becomes a strong upward-and-forward arc.
                val arc = sin(PI.toFloat() * t) * RELEASE_ARC_HEIGHT

                player.x = startX + 25f + 12f * smooth(t)
                player.bottomY = lerp(
                    releaseStartY,
                    platform.topY,
                    t
                ) - arc

                player.vy = if (t < 0.5f) {
                    -520f * (1f - t * 2f)
                } else {
                    520f * ((t - 0.5f) * 2f)
                }

                player.grounded = false

                if (timer >= RELEASE_DURATION) {
                    if (platform.overlapsPlayer(player.x, 22f)) {
                        player.bottomY = platform.topY
                        player.vy = 0f
                        player.grounded = true
                        player.markLanded()

                        phase = Phase.HERO
                        timer = 0f
                    } else {
                        cancelAndFall()
                    }
                }
            }

            Phase.HERO -> {
                // Hold the heroic landing pose on the container roof.
                player.bottomY = platform.topY
                player.vy = 0f
                player.grounded = true

                if (timer >= HERO_HOLD_DURATION) {
                    phase = Phase.NONE
                    timer = 0f
                    target = null
                }
            }

            Phase.NONE -> Unit
        }
    }

    /**
     * Call this AFTER PlayerAnimator.update().
     * It overrides the normal pose only while the stunt is active.
     */
    fun applyPose() {
        if (!active) return

        when (phase) {

            Phase.REACH -> {
                applyRigPose(
                    torso = 20f,
                    head = -12f,
                    bodyY = -2f,
                    lThigh = -55f, lKnee = 70f, lSkate = 10f,
                    rThigh = 10f, rKnee = 65f, rSkate = 0f,
                    lArm = -125f, lForearm = -35f,
                    rArm = -135f, rForearm = -30f
                )
            }

            Phase.GRAB -> {
                applyRigPose(
                    torso = 34f,
                    head = -14f,
                    bodyY = 2f,
                    lThigh = -70f, lKnee = 95f, lSkate = 10f,
                    rThigh = 25f, rKnee = 80f, rSkate = 0f,
                    lArm = -145f, lForearm = -15f,
                    rArm = -150f, rForearm = -12f
                )
            }

            Phase.SWING -> {
                val t = smooth(timer / SWING_DURATION)

                applyRigPose(
                    torso = lerp(34f, -18f, t),
                    head = lerp(-14f, 8f, t),
                    bodyY = -4f,
                    lThigh = lerp(-70f, 65f, t),
                    lKnee = lerp(95f, 5f, t),
                    lSkate = 15f,
                    rThigh = lerp(25f, -55f, t),
                    rKnee = lerp(80f, 95f, t),
                    rSkate = 5f,
                    lArm = -150f, lForearm = -35f,
                    rArm = -155f, rForearm = -25f
                )
            }

            Phase.RELEASE -> {
                val t = smooth(timer / RELEASE_DURATION)

                applyRigPose(
                    torso = lerp(-18f, 8f, t),
                    head = lerp(8f, -8f, t),
                    bodyY = -5f,
                    lThigh = lerp(65f, -55f, t),
                    lKnee = lerp(5f, 85f, t),
                    lSkate = 8f,
                    rThigh = lerp(-55f, 8f, t),
                    rKnee = lerp(95f, 55f, t),
                    rSkate = 50f,
                    lArm = lerp(-150f, -25f, t),
                    lForearm = -30f,
                    rArm = lerp(-155f, -80f, t),
                    rForearm = -45f
                )
            }

            Phase.HERO -> {
                applyRigPose(
                    torso = 72f,
                    head = -52f,
                    bodyY = 2f,
                    lThigh = 57f, lKnee = 2f, lSkate = 0f,
                    rThigh = -70f, rKnee = 115f, rSkate = 0f,
                    lArm = 72f, lForearm = -20f,
                    rArm = -102f, rForearm = -25f
                )
            }

            Phase.NONE -> Unit
        }
    }

    private fun applyRigPose(
        torso: Float,
        head: Float,
        bodyY: Float,
        lThigh: Float,
        lKnee: Float,
        lSkate: Float,
        rThigh: Float,
        rKnee: Float,
        rSkate: Float,
        lArm: Float,
        lForearm: Float,
        rArm: Float,
        rForearm: Float
    ) {
        player.torso.rotation = torso
        player.head.rotation = head

        player.leftThigh.rotation = lThigh - torso
        player.leftShin.rotation = lKnee
        player.leftSkate.rotation = lSkate - lThigh - lKnee

        player.rightThigh.rotation = rThigh - torso
        player.rightShin.rotation = rKnee
        player.rightSkate.rotation = rSkate - rThigh - rKnee

        player.leftUpperArm.rotation = lArm
        player.leftForearm.rotation = lForearm

        player.rightUpperArm.rotation = rArm
        player.rightForearm.rotation = rForearm

        val leftDrop = SkatingAnimation.footDrop(
            lThigh,
            lKnee,
            lSkate - lThigh - lKnee,
            Rig.THIGH_LEN,
            Rig.SHIN_LEN,
            Rig.SKATE_DROP,
            Rig.SKATE_REACH
        )

        val rightDrop = SkatingAnimation.footDrop(
            rThigh,
            rKnee,
            rSkate - rThigh - rKnee,
            Rig.THIGH_LEN,
            Rig.SHIN_LEN,
            Rig.SKATE_DROP,
            Rig.SKATE_REACH
        )

        player.torso.x = player.x
        player.torso.y = player.bottomY - max(leftDrop, rightDrop) + bodyY
    }

    private fun cancelAndFall() {
        phase = Phase.NONE
        timer = 0f
        target = null
        player.grounded = false
        player.vy = 180f
    }

    private fun smooth(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t
    }

    private companion object {
        const val REACH_DURATION = 0.20f
        const val SWING_DURATION = 0.22f
        const val RELEASE_DURATION = 0.42f
        const val HERO_HOLD_DURATION = 0.38f
        const val RELEASE_ARC_HEIGHT = 78f
    }
}
