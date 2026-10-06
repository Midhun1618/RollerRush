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
    private val out = Pose()

    private var airBlend = 0f
    private var landDip = 0f
    private var crashTime = 0f

    fun reset() {
        skating.reset()
        airBlend = 0f
        landDip = 0f
        crashTime = 0f
        state = AnimationState.SKATING
    }

    /**
     * Updates the player's animation.
     *
     * [speedBoostActive] is passed explicitly so the boost pose does not depend
     * on world speed. This keeps the animation correct even as normal game
     * difficulty increases.
     */
    fun update(
        dt: Float,
        worldSpeed: Float,
        speedBoostActive: Boolean = false
    ) {
        skating.update(
            dt,
            (worldSpeed / Constants.BASE_SPEED).coerceIn(0.8f, 1.6f),
            skatePose
        )

        if (player.consumeLanded()) landDip = 1f
        landDip = max(0f, landDip - dt * LAND_RECOVER_RATE)

        state = when {
            player.crashed -> AnimationState.CRASHED
            player.sliding -> AnimationState.SLIDING
            !player.grounded -> {
                if (player.vy < 0f) AnimationState.JUMPING
                else AnimationState.FALLING
            }
            landDip > 0.02f -> AnimationState.LANDING
            else -> AnimationState.SKATING
        }

        // Blend skating <-> airborne.
        val target = if (!player.grounded && !player.crashed) 1f else 0f

        airBlend =
            if (airBlend < target) {
                min(target, airBlend + dt * AIR_BLEND_RATE)
            } else {
                max(target, airBlend - dt * AIR_BLEND_RATE)
            }

        SkatingAnimation.airPose(player.vy, airPose)

        out.lerp(
            skatePose,
            airPose,
            smooth(airBlend)
        )

        // -------------------------------------------------------------
        // AGGRESSIVE SLIDE
        // -------------------------------------------------------------
        if (player.sliding && player.grounded && !player.crashed) {

            slidePose.lerp(skatePose, skatePose, 0f)

            // Full-body aggressive slide.
            // Torso is almost horizontal with the legs thrown apart.
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
        // SPEED BOOST POSE
        // -------------------------------------------------------------
        //
        // During boost the skater becomes much more aerodynamic:
        //
        //       normal
        //          O
        //         /|
        //        / |
        //
        //       BOOST
        //             O
        //          __/|
        //       __/  |
        //
        // Lower torso angle = stronger forward lean.
        //
        if (
            speedBoostActive &&
            player.grounded &&
            !player.sliding &&
            !player.crashed
        ) {
            boostPose.lerp(skatePose, skatePose, 0f)

            // More aggressive forward lean.
            // 82 -> 68 makes the torso visibly flatter/more aerodynamic.
            boostPose.torsoRotation = 75f

            // Head follows the forward lean.
            boostPose.headRotation = -35f

            // Shift the body slightly forward.
            boostPose.hipOffsetX = 7f
            boostPose.bodyOffsetY = 5f

            // Front leg reaches farther forward.
            boostPose.leftThigh = -60f
            boostPose.leftKnee = 65f
            boostPose.leftSkate = 5f

            // Rear leg extends backward for a stronger speed silhouette.
            boostPose.rightThigh = 48f
            boostPose.rightKnee = 10f
            boostPose.rightSkate = 20f

            // Arms sweep backward to sell the acceleration.
            boostPose.leftArm = -48f
            boostPose.leftForearm = -24f

            boostPose.rightArm = 20f
            boostPose.rightForearm = -42f

            // Blend instead of snapping into the boost pose.
            out.lerp(out, boostPose, 0.9f)
        }

        // -------------------------------------------------------------
        // LANDING
        // -------------------------------------------------------------
        if (landDip > 0f) {
            val dKnee = 28f * landDip
            val dThigh = -8f * landDip

            out.leftKnee += dKnee
            out.leftThigh += dThigh
            out.leftSkate -= (dKnee + dThigh)

            out.rightKnee += dKnee
            out.rightThigh += dThigh
            out.rightSkate -= (dKnee + dThigh)

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
                min(1f, crashTime * 5f)
            )
        }

        apply(out)
    }

    /**
     * Writes [pose] into the rig and places the torso (root) so the lowest skate
     * touches the player's ground-contact point.
     */
    private fun apply(p: Pose) {
        val p0 = player

        p0.torso.rotation = p.torsoRotation
        p0.head.rotation = p.headRotation

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

        p0.torso.x =
            p0.x + p.hipOffsetX

        p0.torso.y =
            p0.bottomY -
                    max(dropL, dropR) +
                    p.bodyOffsetY
    }

    private fun smooth(x: Float): Float =
        x * x * (3f - 2f * x)

    private companion object {
        const val AIR_BLEND_RATE = 9f
        const val LAND_RECOVER_RATE = 6f
    }
}