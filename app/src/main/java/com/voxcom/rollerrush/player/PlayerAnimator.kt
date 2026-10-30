package com.voxcom.rollerrush.player

import com.voxcom.rollerrush.animation.AnimationState
import com.voxcom.rollerrush.animation.Pose
import com.voxcom.rollerrush.animation.SkatingAnimation
import com.voxcom.rollerrush.utils.Constants
import kotlin.math.max
import kotlin.math.min

class PlayerAnimator(private val player: Player) {

    var state = AnimationState.SKATING
        private set

    private val skating = SkatingAnimation()

    private val skatePose = Pose()
    private val airPose = Pose()
    private val crashPose = Pose()
    private val slidePose = Pose()
    private val boostPose = Pose()
    private val introPose = Pose()
    private val jumpAnticipationPose = Pose()
    private val out = Pose()

    private var airBlend = 0f
    private var landDip = 0f
    private var crashTime = 0f

    // Powerhouse booster animation
    private var boostTime = 0f
    private var boostBlend = 0f

    private var jumpAnticipationTime = -1f

    fun reset() {
        skating.reset()

        airBlend = 0f
        landDip = 0f
        crashTime = 0f

        boostTime = 0f
        boostBlend = 0f

        jumpAnticipationTime = -1f

        player.setIntroRotation(0f)

        state = AnimationState.SKATING
    }

    fun requestJumpAnticipation() {
        jumpAnticipationTime = 0f
        airBlend = 0f
    }

    fun updateIntro(
        time: Float,
        dt: Float
    ) {
        val t = time.coerceIn(0f, Constants.INTRO_DURATION)

        when {
            // =========================================================
            // FALL
            // =========================================================
            t < Constants.INTRO_FALL_DURATION -> {
                val raw =
                    (t / Constants.INTRO_FALL_DURATION)
                        .coerceIn(0f, 1f)

                val fall = smooth(raw)

                player.x = Constants.PLAYER_X

                player.bottomY = lerp(
                    Constants.INTRO_START_Y,
                    Constants.GROUND_Y - 18f,
                    fall
                )

                player.grounded = false
                player.crashed = false

                player.setIntroRotation(
                    lerp(
                        Constants.INTRO_START_ROTATION,
                        Constants.INTRO_LANDING_ROTATION,
                        fall
                    )
                )

                introPose.lerp(skatePose, skatePose, 0f)

                introPose.torsoRotation = 38f
                introPose.headRotation = -12f
                introPose.hipOffsetX = 0f
                introPose.bodyOffsetY = 0f

                introPose.leftThigh = -52f
                introPose.leftKnee = 82f
                introPose.leftSkate = 8f - (-52f + 82f)

                introPose.rightThigh = 8f
                introPose.rightKnee = 52f
                introPose.rightSkate = 48f - (8f + 52f)

                val armSwing =
                    kotlin.math.sin(raw * Math.PI.toFloat() * 1.5f) * 12f

                introPose.leftArm = -28f - armSwing
                introPose.leftForearm = -25f
                introPose.rightArm = -62f + armSwing
                introPose.rightForearm = -25f

                apply(introPose)
                state = AnimationState.JUMPING
            }

            // =========================================================
            // LANDING
            // =========================================================
            t < Constants.INTRO_SKATE_START -> {
                val landingTime =
                    (t - Constants.INTRO_FALL_DURATION)
                        .coerceAtLeast(0f)

                val raw =
                    (landingTime / Constants.INTRO_LANDING_DURATION)
                        .coerceIn(0f, 1f)

                player.x = Constants.PLAYER_X
                player.bottomY = Constants.GROUND_Y
                player.grounded = true
                player.crashed = false

                player.setIntroRotation(Constants.INTRO_LANDING_ROTATION)

                introPose.lerp(skatePose, skatePose, 0f)

                val compression =
                    if (raw < 0.35f) {
                        smooth(raw / 0.35f)
                    } else {
                        1f - smooth((raw - 0.35f) / 0.65f)
                    }

                introPose.torsoRotation = 38f + 12f * compression
                introPose.headRotation = -18f + 9f * compression
                introPose.bodyOffsetY = 9f * compression
                introPose.hipOffsetX = -2f * compression

                introPose.leftThigh = -16f - 18f * compression
                introPose.leftKnee = 50f + 36f * compression
                introPose.leftSkate = -7f * compression

                introPose.rightThigh = -16f - 14f * compression
                introPose.rightKnee = 50f + 32f * compression
                introPose.rightSkate = 7f * compression

                introPose.leftArm = 22f - 28f * compression
                introPose.leftForearm = -18f - 22f * compression
                introPose.rightArm = 38f + 22f * compression
                introPose.rightForearm = -18f + 28f * compression

                apply(introPose)

                state = if (raw < 0.65f) {
                    AnimationState.LANDING
                } else {
                    AnimationState.SKATING
                }
            }

            // =========================================================
            // FIRST SKATING PUSH / SMOOTH EXIT
            // =========================================================
            else -> {
                player.bottomY = Constants.GROUND_Y
                player.grounded = true
                player.crashed = false

                val skateRaw =
                    (
                            (t - Constants.INTRO_SKATE_START) /
                                    (Constants.INTRO_DURATION - Constants.INTRO_SKATE_START)
                            ).coerceIn(0f, 1f)

                val skateBlend = smooth(skateRaw)

                player.x = Constants.PLAYER_X
                player.setIntroRotation(0f)

                skating.update(dt, 1f, skatePose)

                out.lerp(
                    introPose,
                    skatePose,
                    skateBlend
                )

                apply(out)
                state = AnimationState.SKATING
            }
        }
    }

    /**
     * Normal gameplay animation with a dedicated crash/ragdoll-style state.
     */

    fun update(
        dt: Float,
        worldSpeed: Float,
        speedBoostActive: Boolean = false
    ) {
        // -------------------------------------------------------------
        // CRASH
        // -------------------------------------------------------------
        if (player.crashed) {
            crashTime += dt

            val t = crashTime
            val damping = kotlin.math.exp(-2.8f * t)

            crashPose.torsoRotation =
                70f +
                        28f * (1f - kotlin.math.exp(-5f * t)) +
                        kotlin.math.sin(t * 10f) * 12f * damping

            crashPose.headRotation =
                -20f +
                        kotlin.math.sin(t * 13f) * 24f * damping

            crashPose.hipOffsetX =
                kotlin.math.sin(t * 8f) * 6f * damping

            crashPose.bodyOffsetY =
                12f +
                        18f * (1f - kotlin.math.exp(-4f * t)) +
                        kotlin.math.sin(t * 9f) * 5f * damping

            crashPose.leftThigh =
                -55f +
                        kotlin.math.sin(t * 10f + 0.4f) * 48f * damping

            crashPose.leftKnee =
                65f +
                        kotlin.math.sin(t * 14f) * 58f * damping

            crashPose.leftSkate =
                20f +
                        kotlin.math.sin(t * 16f + 1f) * 65f * damping

            crashPose.rightThigh =
                35f +
                        kotlin.math.sin(t * 9f + 1.8f) * 55f * damping

            crashPose.rightKnee =
                50f +
                        kotlin.math.sin(t * 12f + 0.8f) * 65f * damping

            crashPose.rightSkate =
                -20f +
                        kotlin.math.sin(t * 17f + 2f) * 70f * damping

            crashPose.leftArm =
                -100f +
                        kotlin.math.sin(t * 12f) * 75f * damping

            crashPose.leftForearm =
                -45f +
                        kotlin.math.sin(t * 17f + 1f) * 65f * damping

            crashPose.rightArm =
                95f +
                        kotlin.math.sin(t * 10f + 2f) * 70f * damping

            crashPose.rightForearm =
                35f +
                        kotlin.math.sin(t * 15f + 0.5f) * 75f * damping

            apply(crashPose)
            state = AnimationState.CRASHED
            return
        }

        // -------------------------------------------------------------
        // SKATING SPEED
        // -------------------------------------------------------------
        val normalSpeedFactor =
            (worldSpeed / Constants.BASE_SPEED)
                .coerceIn(0.8f, 1.6f)

        val boostTarget =
            speedBoostActive &&
                    player.grounded &&
                    !player.sliding

        val skatingSpeedFactor =
            if (boostTarget) {
                (normalSpeedFactor * 1.65f)
                    .coerceIn(1.5f, 2.6f)
            } else {
                normalSpeedFactor
            }

        skating.update(
            dt,
            skatingSpeedFactor,
            skatePose
        )

        // -------------------------------------------------------------
        // LANDING EVENT
        // -------------------------------------------------------------
        if (player.consumeLanded()) {
            landDip = 1f
        }

        landDip = max(
            0f,
            landDip - dt * LAND_RECOVER_RATE
        )

        // -------------------------------------------------------------
        // ANIMATION STATE
        // -------------------------------------------------------------
        state = when {
            player.sliding ->
                AnimationState.SLIDING

            !player.grounded -> {
                if (player.vy < 0f) {
                    AnimationState.JUMPING
                } else {
                    AnimationState.FALLING
                }
            }

            landDip > 0.02f ->
                AnimationState.LANDING

            else ->
                AnimationState.SKATING
        }

        // -------------------------------------------------------------
        // AIR POSE
        // -------------------------------------------------------------
        SkatingAnimation.airPose(
            player.vy,
            airPose
        )

        // -------------------------------------------------------------
        // JUMP ANTICIPATION + AIR TRANSITION
        // -------------------------------------------------------------
        if (jumpAnticipationTime >= 0f) {
            jumpAnticipationTime += dt

            val t = jumpAnticipationTime

            jumpAnticipationPose.lerp(
                skatePose,
                skatePose,
                0f
            )

            // Body compression
            jumpAnticipationPose.torsoRotation =
                skatePose.torsoRotation + 8f

            jumpAnticipationPose.headRotation =
                skatePose.headRotation - 5f

            jumpAnticipationPose.hipOffsetX =
                skatePose.hipOffsetX

            jumpAnticipationPose.bodyOffsetY =
                skatePose.bodyOffsetY + 12f

            // Legs: crouch
            jumpAnticipationPose.leftThigh = -8f
            jumpAnticipationPose.leftKnee = 82f
            jumpAnticipationPose.leftSkate = -74f

            jumpAnticipationPose.rightThigh = -8f
            jumpAnticipationPose.rightKnee = 82f
            jumpAnticipationPose.rightSkate = -74f

            // Arms: swing backward
            jumpAnticipationPose.leftArm = 65f
            jumpAnticipationPose.leftForearm = -65f

            jumpAnticipationPose.rightArm = 65f
            jumpAnticipationPose.rightForearm = -65f

            // Phase 1: crouch
            if (t < JUMP_CROUCH_DURATION) {

                val crouchProgress = smooth(
                    (t / JUMP_CROUCH_DURATION)
                        .coerceIn(0f, 1f)
                )

                out.lerp(
                    skatePose,
                    jumpAnticipationPose,
                    crouchProgress
                )
            }

            // Phase 2: extend into the jump
            else if (t < JUMP_TOTAL_TRANSITION) {

                val extensionProgress = smooth(
                    (
                            (t - JUMP_CROUCH_DURATION) /
                                    JUMP_EXTENSION_DURATION
                            ).coerceIn(0f, 1f)
                )

                out.lerp(
                    jumpAnticipationPose,
                    airPose,
                    extensionProgress
                )
            }

            // Phase 3: finish transition
            else {
                jumpAnticipationTime = -1f
                airBlend = 1f

                out.lerp(
                    airPose,
                    airPose,
                    0f
                )
            }

        } else {

            if (player.grounded) {
                jumpAnticipationTime = -1f
            }

            val airTarget =
                if (!player.grounded) 1f else 0f

            if (airBlend < airTarget) {
                airBlend = min(
                    airTarget,
                    airBlend + dt * AIR_TAKEOFF_BLEND_RATE
                )
            } else {
                airBlend = max(
                    airTarget,
                    airBlend - dt * AIR_LANDING_BLEND_RATE
                )
            }

            out.lerp(
                skatePose,
                airPose,
                smooth(airBlend)
            )
        }

        // -------------------------------------------------------------
        // SLIDE
        // -------------------------------------------------------------
        if (player.sliding && player.grounded) {
            slidePose.lerp(skatePose, skatePose, 0f)

            slidePose.torsoRotation = 82f
            slidePose.headRotation = -24f
            slidePose.hipOffsetX = 3f
            slidePose.bodyOffsetY = 8f

            slidePose.leftThigh = -78f
            slidePose.leftKnee = 22f
            slidePose.leftSkate = -8f

            slidePose.rightThigh = 58f
            slidePose.rightKnee = 34f
            slidePose.rightSkate = 8f

            slidePose.leftArm = -38f
            slidePose.leftForearm = -12f
            slidePose.rightArm = 26f
            slidePose.rightForearm = -34f

            out.lerp(out, slidePose, 0.95f)
        }

        // -------------------------------------------------------------
        // POWERHOUSE SPEED BOOST
        // -------------------------------------------------------------
        boostBlend = if (boostTarget) {
            min(1f, boostBlend + dt * 5.5f)
        } else {
            max(0f, boostBlend - dt * 4.5f)
        }

        if (boostTarget) {
            boostTime += dt
        } else if (boostBlend <= 0f) {
            boostTime = 0f
        }

        if (boostBlend > 0f) {
            val phase =
                boostTime * 2f * Math.PI.toFloat() * 2.15f

            boostPose.lerp(
                skatePose,
                skatePose,
                0f
            )

            boostPose.torsoRotation =
                84f + kotlin.math.sin(phase * 2f) * 2.5f

            boostPose.headRotation =
                -43f - kotlin.math.sin(phase * 2f) * 3f

            boostPose.hipOffsetX =
                9f + kotlin.math.sin(phase) * 1.8f

            boostPose.bodyOffsetY =
                5f + kotlin.math.sin(phase * 2f) * 2.5f

            out.lerp(
                out,
                boostPose,
                boostBlend
            )
        }

        // -------------------------------------------------------------
        // LANDING RECOVERY
        // -------------------------------------------------------------
        if (landDip > 0f) {
            val dKnee = 28f * landDip
            val dThigh = -8f * landDip

            out.leftKnee += dKnee
            out.leftThigh += dThigh
            out.leftSkate -= dKnee + dThigh

            out.rightKnee += dKnee
            out.rightThigh += dThigh
            out.rightSkate -= dKnee + dThigh

            out.torsoRotation += 6f * landDip
            out.headRotation -= 5f * landDip
        }

        apply(out)
    }


    private fun apply(p: Pose) {

        val p0 = player

        p0.torso.rotation =
            p.torsoRotation

        p0.head.rotation =
            p.headRotation

        p0.leftThigh.rotation =
            p.leftThigh - p.torsoRotation

        p0.leftShin.rotation =
            p.leftKnee

        p0.leftSkate.rotation =
            p.leftSkate

        p0.rightThigh.rotation =
            p.rightThigh - p.torsoRotation

        p0.rightShin.rotation =
            p.rightKnee

        p0.rightSkate.rotation =
            p.rightSkate

        p0.leftUpperArm.rotation =
            p.leftArm

        p0.leftForearm.rotation =
            p.leftForearm

        p0.rightUpperArm.rotation =
            p.rightArm

        p0.rightForearm.rotation =
            p.rightForearm

        val dropL = SkatingAnimation.footDrop(
            p.leftThigh,
            p.leftKnee,
            p.leftSkate,
            Rig.THIGH_LEN,
            Rig.SHIN_LEN,
            Rig.SKATE_DROP,
            Rig.SKATE_REACH
        )

        val dropR = SkatingAnimation.footDrop(
            p.rightThigh,
            p.rightKnee,
            p.rightSkate,
            Rig.THIGH_LEN,
            Rig.SHIN_LEN,
            Rig.SKATE_DROP,
            Rig.SKATE_REACH
        )

        p0.torso.y =
            p0.bottomY - max(dropL, dropR) + p.bodyOffsetY

        p0.torso.x =
            p0.x + p.hipOffsetX
    }

    private fun smooth(x: Float): Float {
        val v = x.coerceIn(0f, 1f)
        return v * v * (3f - 2f * v)
    }

    private fun lerp(
        a: Float,
        b: Float,
        t: Float
    ): Float {
        return a + (b - a) * t
    }

    private companion object {
        const val AIR_TAKEOFF_BLEND_RATE = 4.5f
        const val AIR_LANDING_BLEND_RATE = 7.5f

        const val JUMP_CROUCH_DURATION = 0.12f
        const val JUMP_EXTENSION_DURATION = 0.18f

        const val JUMP_TOTAL_TRANSITION =
            JUMP_CROUCH_DURATION + JUMP_EXTENSION_DURATION

        const val LAND_RECOVER_RATE = 6f
    }
}
