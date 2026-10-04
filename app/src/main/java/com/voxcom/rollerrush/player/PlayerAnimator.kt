package com.voxcom.rollerrush.player

import com.voxcom.rollerrush.animation.AnimationState
import com.voxcom.rollerrush.animation.Pose
import com.voxcom.rollerrush.animation.SkatingAnimation
import com.voxcom.rollerrush.utils.Constants
import kotlin.math.max
import kotlin.math.min

/**
 * Turns the player's physics state into a [Pose] and applies that pose to the BodyPart rig.
 *
 * The skating cycle always keeps running; the airborne pose is BLENDED in/out so
 * take-off and landing are smooth, never a snap.
 */
class PlayerAnimator(private val player: Player) {

    var state = AnimationState.SKATING
        private set

    private val skating = SkatingAnimation()
    private val skatePose = Pose()
    private val airPose = Pose()
    private val crashPose = Pose()
    private val out = Pose()

    private var airBlend = 0f     // 0 = skating pose, 1 = airborne pose
    private var landDip = 0f      // 1 right after landing, decays to 0: knee-absorb squash
    private var crashTime = 0f

    fun reset() {
        skating.reset()
        airBlend = 0f; landDip = 0f; crashTime = 0f
        state = AnimationState.SKATING
    }

    fun update(dt: Float, worldSpeed: Float) {
        skating.update(dt, (worldSpeed / Constants.BASE_SPEED).coerceIn(0.8f, 1.6f), skatePose)

        if (player.consumeLanded()) landDip = 1f
        landDip = max(0f, landDip - dt * LAND_RECOVER_RATE)

        state = when {
            player.crashed -> AnimationState.CRASHED
            !player.grounded -> if (player.vy < 0f) AnimationState.JUMPING else AnimationState.FALLING
            landDip > 0.02f -> AnimationState.LANDING
            else -> AnimationState.SKATING
        }

        // Blend skating <-> airborne.
        val target = if (!player.grounded && !player.crashed) 1f else 0f
        airBlend = if (airBlend < target) min(target, airBlend + dt * AIR_BLEND_RATE)
        else max(target, airBlend - dt * AIR_BLEND_RATE)
        SkatingAnimation.airPose(player.vy, airPose)
        out.lerp(skatePose, airPose, smooth(airBlend))

        // Landing: both knees absorb the impact, torso dips forward, skates stay flat.
        if (landDip > 0f) {
            val dKnee = 28f * landDip
            val dThigh = -8f * landDip
            out.leftKnee += dKnee; out.leftThigh += dThigh; out.leftSkate -= (dKnee + dThigh)
            out.rightKnee += dKnee; out.rightThigh += dThigh; out.rightSkate -= (dKnee + dThigh)
            out.torsoRotation += 6f * landDip
            out.headRotation -= 5f * landDip
        }

        if (player.crashed) {
            crashTime += dt
            SkatingAnimation.crashPose(crashTime, crashPose)
            out.lerp(out, crashPose, min(1f, crashTime * 5f))
        }

        apply(out)
    }

    /**
     * Writes [pose] into the rig and places the torso (root) so the lowest skate touches
     * the player's ground-contact point.
     *
     * Forward kinematics: for each leg the vertical hip->wheel distance ("drop") is computed
     * from its joint angles. The leg with the larger drop is the planted one; the hip is put
     * exactly that far above bottomY. As the legs swap roles the hip rises and falls naturally,
     * so the torso never bobs more than the legs dictate.
     */
    private fun apply(p: Pose) {
        val p0 = player

        // Thigh angles in the pose are WORLD angles; the thigh is a child of the rotated torso,
        // so subtract the torso rotation to get the local value.
        p0.torso.rotation = p.torsoRotation
        p0.head.rotation = p.headRotation

        p0.leftThigh.rotation = p.leftThigh - p.torsoRotation
        p0.leftShin.rotation = p.leftKnee
        p0.leftSkate.rotation = p.leftSkate
        p0.rightThigh.rotation = p.rightThigh - p.torsoRotation
        p0.rightShin.rotation = p.rightKnee
        p0.rightSkate.rotation = p.rightSkate

        p0.leftUpperArm.rotation = p.leftArm
        p0.leftForearm.rotation = p.leftForearm
        p0.rightUpperArm.rotation = p.rightArm
        p0.rightForearm.rotation = p.rightForearm

        val dropL = SkatingAnimation.footDrop(p.leftThigh, p.leftKnee, p.leftSkate,
            Rig.THIGH_LEN, Rig.SHIN_LEN, Rig.SKATE_DROP, Rig.SKATE_REACH)
        val dropR = SkatingAnimation.footDrop(p.rightThigh, p.rightKnee, p.rightSkate,
            Rig.THIGH_LEN, Rig.SHIN_LEN, Rig.SKATE_DROP, Rig.SKATE_REACH)

        p0.torso.x = p0.x + p.hipOffsetX
        p0.torso.y = p0.bottomY - max(dropL, dropR) + p.bodyOffsetY
    }

    private fun smooth(x: Float) = x * x * (3f - 2f * x)

    private companion object {
        const val AIR_BLEND_RATE = 9f      // ~0.11 s to blend
        const val LAND_RECOVER_RATE = 6f   // ~0.17 s to stand back up
    }
}
