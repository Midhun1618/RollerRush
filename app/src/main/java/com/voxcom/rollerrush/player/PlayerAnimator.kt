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

    private val out = Pose()

    private var airBlend = 0f
    private var landDip = 0f
    private var crashTime = 0f

    fun reset() {
        skating.reset()

        airBlend = 0f
        landDip = 0f
        crashTime = 0f

        player.setIntroRotation(0f)

        state = AnimationState.SKATING
    }

    /**
     * Cinematic entrance.
     *
     * The player starts above the world at -180 degrees and smoothly
     * rotates to exactly 0 degrees while falling. There is no 360-degree
     * root flip, so the landing cannot snap back to zero.
     */
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

                // Keep the player away from the very beginning of the
                // background. Horizontal movement toward normal gameplay
                // position happens only after the landing.
                player.x = Constants.INTRO_START_X

                player.bottomY = lerp(
                    Constants.INTRO_START_Y,
                    Constants.GROUND_Y - 18f,
                    fall
                )

                player.grounded = false
                player.crashed = false

                // -180 -> 0 smoothly. Never use 360 here.
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

                // Tucked legs.
                introPose.leftThigh = -52f
                introPose.leftKnee = 82f
                introPose.leftSkate = 8f - (-52f + 82f)

                introPose.rightThigh = 8f
                introPose.rightKnee = 52f
                introPose.rightSkate = 48f - (8f + 52f)

                // Small arm movement during the fall.
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

                player.x = Constants.INTRO_START_X
                player.bottomY = Constants.GROUND_Y
                player.grounded = true
                player.crashed = false

                // Rotation is already complete.
                player.setIntroRotation(Constants.INTRO_LANDING_ROTATION)

                introPose.lerp(skatePose, skatePose, 0f)

                // Strong compression, then recovery.
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

                // Smoothly move from the cinematic position to the normal
                // gameplay position. No horizontal teleport at the end.
                player.x = lerp(
                    Constants.INTRO_START_X,
                    Constants.PLAYER_X,
                    skateBlend
                )

                player.setIntroRotation(0f)

                // Start the normal skating cycle underneath the cinematic
                // landing pose, then blend into it. This prevents the pose
                // from snapping to the first skating frame after the flip.
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
     * Normal gameplay animation.
     */
    fun update(
        dt: Float,
        worldSpeed: Float,
        speedBoostActive: Boolean = false
    ) {

        skating.update(
            dt,
            (worldSpeed / Constants.BASE_SPEED)
                .coerceIn(0.8f, 1.6f),
            skatePose
        )

        if (player.consumeLanded()) {
            landDip = 1f
        }

        landDip =
            max(
                0f,
                landDip - dt * LAND_RECOVER_RATE
            )

        state = when {
            player.crashed ->
                AnimationState.CRASHED

            player.sliding ->
                AnimationState.SLIDING

            !player.grounded -> {
                if (player.vy < 0f)
                    AnimationState.JUMPING
                else
                    AnimationState.FALLING
            }

            landDip > 0.02f ->
                AnimationState.LANDING

            else ->
                AnimationState.SKATING
        }

        val target =
            if (!player.grounded && !player.crashed)
                1f
            else
                0f

        airBlend =
            if (airBlend < target) {
                min(
                    target,
                    airBlend + dt * AIR_BLEND_RATE
                )
            } else {
                max(
                    target,
                    airBlend - dt * AIR_BLEND_RATE
                )
            }

        SkatingAnimation.airPose(
            player.vy,
            airPose
        )

        out.lerp(
            skatePose,
            airPose,
            smooth(airBlend)
        )

        // -------------------------------------------------------------
        // AGGRESSIVE SLIDE
        // -------------------------------------------------------------

        if (
            player.sliding &&
            player.grounded &&
            !player.crashed
        ) {

            slidePose.lerp(
                skatePose,
                skatePose,
                0f
            )

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

            out.lerp(
                out,
                slidePose,
                0.95f
            )
        }

        // -------------------------------------------------------------
        // SPEED BOOST
        // -------------------------------------------------------------

        if (
            speedBoostActive &&
            player.grounded &&
            !player.sliding &&
            !player.crashed
        ) {

            boostPose.lerp(
                skatePose,
                skatePose,
                0f
            )

            boostPose.torsoRotation = 75f
            boostPose.headRotation = -35f

            boostPose.hipOffsetX = 7f
            boostPose.bodyOffsetY = 5f

            boostPose.leftThigh = -60f
            boostPose.leftKnee = 65f
            boostPose.leftSkate = 5f

            boostPose.rightThigh = 48f
            boostPose.rightKnee = 10f
            boostPose.rightSkate = 20f

            boostPose.leftArm = -48f
            boostPose.leftForearm = -24f

            boostPose.rightArm = 20f
            boostPose.rightForearm = -42f

            out.lerp(
                out,
                boostPose,
                0.9f
            )
        }

        // -------------------------------------------------------------
        // LANDING
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

        // -------------------------------------------------------------
        // CRASH
        // -------------------------------------------------------------

        if (player.crashed) {

            crashTime += dt

            SkatingAnimation.crashPose(
                crashTime,
                crashPose
            )

            out.lerp(
                out,
                crashPose,
                min(
                    1f,
                    crashTime * 5f
                )
            )
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

        val dropL =
            SkatingAnimation.footDrop(
                p.leftThigh,
                p.leftKnee,
                p.leftSkate,
                Rig.THIGH_LEN,
                Rig.SHIN_LEN,
                Rig.SKATE_DROP,
                Rig.SKATE_REACH
            )

        val dropR =
            SkatingAnimation.footDrop(
                p.rightThigh,
                p.rightKnee,
                p.rightSkate,
                Rig.THIGH_LEN,
                Rig.SHIN_LEN,
                Rig.SKATE_DROP,
                Rig.SKATE_REACH
            )

        p0.torso.x =
            p0.x + p.hipOffsetX

        p0.torso.y =
            p0.bottomY -
                    max(dropL, dropR) +
                    p.bodyOffsetY
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

        const val AIR_BLEND_RATE = 9f
        const val LAND_RECOVER_RATE = 6f
    }
}
