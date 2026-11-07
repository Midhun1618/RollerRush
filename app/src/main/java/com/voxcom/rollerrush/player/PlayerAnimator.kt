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
    private val tuckPose = Pose()
    private val reachPose = Pose()
    private val landPose = Pose()
    private val heroPose = Pose()

    private val jumpAnticipationPose = Pose()
    private val out = Pose()

    private var airBlend = 0f
    private var landDip = 0f
    private var crashTime = 0f

    // Powerhouse booster animation
    private var boostTime = 0f
    private var boostBlend = 0f

    private var jumpAnticipationTime = -1f

    init {
        // Intro key poses (built once, never reallocated). Skate angles are given as
        // ABSOLUTE angles (0 = wheels flat) and converted to the rig's relative angle.
        setBody(tuckPose, 38f, -18f, 0f, -2f)
        setLegs(tuckPose, -75f, 105f, 10f, -62f, 98f, 5f)
        setArms(tuckPose, -60f, -75f, -72f, -60f)

        setBody(reachPose, 14f, -8f, -1f, -4f)
        setLegs(reachPose, -26f, 38f, 6f, -6f, 26f, 0f)
        setArms(reachPose, 40f, -12f, -85f, -10f)

        // IMPACT: deep lunge, front (near) leg folded, rear (far) leg driven back,
        // both wheels flat on the ground (foot drops match), arms thrown out.
        setBody(landPose, 74f, -30f, 2f, 6f)
        setLegs(landPose, 62f, 3f, 0f, -80f, 125f, 0f)
        setArms(landPose, 55f, -30f, -85f, -30f)

        // HERO BEAT: slightly higher, head up, fist forward, other arm swept back.
        setBody(heroPose, 72f, -52f, 3f, 2f)
        setLegs(heroPose, 57f, 2f, 0f, -70f, 115f, 0f)
        setArms(heroPose, 72f, -20f, -102f, -25f)
    }

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

    /**
     * Intro entrance. [time] = seconds since the run started. Phases (see Constants):
     * drop + half flip (tuck -> reach), impact squash, recovery, then push strokes that
     * blend into the live SKATING cycle. No allocation; all poses are preallocated.
     */
    fun updateIntro(
        time: Float,
        dt: Float
    ) {
        val t = time.coerceIn(0f, Constants.INTRO_DURATION)

        player.x = Constants.PLAYER_X
        player.crashed = false

        if (t < Constants.INTRO_IMPACT_TIME) {
            // ---------------- DROP + FLIP ----------------
            val fallT = t - Constants.INTRO_REVEAL
            val u = (fallT / Constants.INTRO_FALL_DURATION).coerceIn(0f, 1f)

            // Gravity-like ease-in, softened so the skater doesn't slam in.
            val e = u * u * 0.75f + u * 0.25f
            player.bottomY = lerp(Constants.INTRO_START_Y, Constants.GROUND_Y, e)
            player.grounded = false

            // Half flip: smooth in/out to upright by 85% of the rotation window,
            // then a small forward overshoot that settles to 0 at the end.
            val rx = (
                    (t - Constants.INTRO_ROTATION_START) /
                            Constants.INTRO_ROTATION_DURATION
                    ).coerceIn(0f, 1f)
            player.setIntroRotation(
                lerp(
                    Constants.INTRO_START_ROTATION,
                    Constants.INTRO_LANDING_ROTATION,
                    smooth(rx / 0.85f)
                ) + 7f * kotlin.math.sin(Math.PI.toFloat() * rx)
            )

            // Legs lead, torso follows, arms lag: staggered tuck -> reach.
            blendGroups(
                out, tuckPose, reachPose,
                smooth((u - 0.45f) / 0.40f),
                smooth((u - 0.40f) / 0.40f),
                smooth((u - 0.55f) / 0.40f)
            )
            apply(out)
            state = AnimationState.JUMPING
            return
        }

        // ---------------- GROUNDED ----------------
        player.bottomY = Constants.GROUND_Y
        player.grounded = true
        player.setIntroRotation(0f)

        // Skating cycle is frozen at phase 0 until the first push, then runs fast
        // and eases to the normal rate by the hand-off.
        val pushT = t - Constants.INTRO_SKATE_START
        if (pushT > 0f) {
            val span = Constants.INTRO_DURATION - Constants.INTRO_SKATE_START
            val rate = lerp(Constants.INTRO_STROKE_RATE, 1f, smooth(pushT / span))
            skating.update(dt, rate, skatePose)
        } else {
            skating.update(0f, 1f, skatePose)
        }

        val since = t - Constants.INTRO_IMPACT_TIME
        val rec = (
                (since - Constants.INTRO_LANDING_HOLD) /
                        Constants.INTRO_RECOVER_DURATION
                ).coerceIn(0f, 1f)

        if (rec <= 0f) {
            if (since < Constants.INTRO_COMPRESS_DURATION) {
                // Hard impact: snap into the deep lunge.
                out.lerp(
                    reachPose, landPose,
                    easeOut(since / Constants.INTRO_COMPRESS_DURATION)
                )
            } else {
                // Hero beat: settle, lift the head, hold the pose.
                val settle = (
                        (since - Constants.INTRO_COMPRESS_DURATION) /
                                (Constants.INTRO_LANDING_HOLD - Constants.INTRO_COMPRESS_DURATION)
                        ).coerceIn(0f, 1f)
                out.lerp(landPose, heroPose, smooth(settle))
            }
            state = AnimationState.LANDING
        } else {
            // Rise (slight overshoot) into the live skating pose.
            out.lerp(heroPose, skatePose, backOut(rec))
            state = if (rec < 1f) AnimationState.LANDING else AnimationState.SKATING
        }
        apply(out)
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

    private fun setBody(p: Pose, torso: Float, head: Float, hipX: Float, bodyY: Float) {
        p.torsoRotation = torso; p.headRotation = head
        p.hipOffsetX = hipX; p.bodyOffsetY = bodyY
    }

    private fun setLegs(
        p: Pose,
        lThigh: Float, lKnee: Float, lSkateAbs: Float,
        rThigh: Float, rKnee: Float, rSkateAbs: Float
    ) {
        p.leftThigh = lThigh; p.leftKnee = lKnee
        p.leftSkate = lSkateAbs - lThigh - lKnee
        p.rightThigh = rThigh; p.rightKnee = rKnee
        p.rightSkate = rSkateAbs - rThigh - rKnee
    }

    private fun setArms(p: Pose, la: Float, lf: Float, ra: Float, rf: Float) {
        p.leftArm = la; p.leftForearm = lf
        p.rightArm = ra; p.rightForearm = rf
    }

    /** Per-group blend a -> b so legs, torso and arms move on different timings. */
    private fun blendGroups(o: Pose, a: Pose, b: Pose, legs: Float, torso: Float, arms: Float) {
        o.leftThigh = lerp(a.leftThigh, b.leftThigh, legs)
        o.leftKnee = lerp(a.leftKnee, b.leftKnee, legs)
        o.leftSkate = lerp(a.leftSkate, b.leftSkate, legs)
        o.rightThigh = lerp(a.rightThigh, b.rightThigh, legs)
        o.rightKnee = lerp(a.rightKnee, b.rightKnee, legs)
        o.rightSkate = lerp(a.rightSkate, b.rightSkate, legs)
        o.torsoRotation = lerp(a.torsoRotation, b.torsoRotation, torso)
        o.headRotation = lerp(a.headRotation, b.headRotation, torso)
        o.hipOffsetX = lerp(a.hipOffsetX, b.hipOffsetX, torso)
        o.bodyOffsetY = lerp(a.bodyOffsetY, b.bodyOffsetY, torso)
        o.leftArm = lerp(a.leftArm, b.leftArm, arms)
        o.leftForearm = lerp(a.leftForearm, b.leftForearm, arms)
        o.rightArm = lerp(a.rightArm, b.rightArm, arms)
        o.rightForearm = lerp(a.rightForearm, b.rightForearm, arms)
    }

    private fun easeOut(x: Float): Float {
        val v = 1f - x.coerceIn(0f, 1f)
        return 1f - v * v
    }

    /** Ease-out with a small (~4%) overshoot; returns exactly 1 at x = 1. */
    private fun backOut(x: Float): Float {
        val v = x.coerceIn(0f, 1f) - 1f
        return 1f + 1.9f * v * v * v + 0.9f * v * v
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

        const val JUMP_CROUCH_DURATION = 0.10f
        const val JUMP_EXTENSION_DURATION = 0.18f

        const val JUMP_TOTAL_TRANSITION =
            JUMP_CROUCH_DURATION + JUMP_EXTENSION_DURATION

        const val LAND_RECOVER_RATE = 6f
    }
}
