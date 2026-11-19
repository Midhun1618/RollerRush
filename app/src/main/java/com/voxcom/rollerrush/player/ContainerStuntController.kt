package com.voxcom.rollerrush.player

import com.voxcom.rollerrush.animation.SkatingAnimation
import com.voxcom.rollerrush.entities.ContainerPlatform
import com.voxcom.rollerrush.utils.Constants
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Camera goals published by the container stunt every frame.
 * Camera chases these values smoothly (exponential easing), so when the stunt
 * ends and everything goes back to neutral the camera glides back to the
 * normal skating framing on its own.
 */
class StuntCameraTarget {
    var zoom = 1f          // 1 = normal framing
    var focusDy = 0f       // world units, negative = look higher
    var panX = 0f          // world units, negative = more space in front of the skater
    var tilt = 0f          // degrees, positive = clockwise (follows a forward flip)
    var response = 4.5f    // chase speed (1/seconds); higher = snappier

    fun neutral() {
        zoom = 1f
        focusDy = 0f
        panX = 0f
        tilt = 0f
        response = 4.5f
    }
}

/**
 * Container stunt:
 *
 *  LEAP     skater hops toward the container with arms reaching for the top-left edge
 *  HANG     hands lock onto the edge (hands are pinned by forward kinematics), a quick
 *           catch-dip, then a pull-up with the legs loading backwards
 *  FLIP     push off the edge, tuck, full front flip, open up for landing
 *  LAND     impact into the heroic one-knee pose
 *  HERO     hold the hero pose
 *  RECOVER  blend rig back into the normal skating cycle (and x back to PLAYER_X)
 *
 * GameWorld order per frame: update() -> PlayerAnimator.update() -> applyPose().
 */
class ContainerStuntController(
    private val player: Player
) {

    /** Read by GameWorld and handed to Camera. */
    val cameraTarget = StuntCameraTarget()

    private enum class Phase { NONE, LEAP, HANG, FLIP, LAND, HERO, RECOVER }

    private class Pose(
        var torso: Float = 0f,
        var head: Float = 0f,       // absolute
        var lThigh: Float = 0f,     // absolute
        var lKnee: Float = 0f,      // relative to thigh
        var lSkate: Float = 0f,     // absolute
        var rThigh: Float = 0f,
        var rKnee: Float = 0f,
        var rSkate: Float = 0f,
        var lArm: Float = 0f,       // absolute upper-arm angle
        var lFore: Float = 0f,      // relative to upper arm
        var rArm: Float = 0f,
        var rFore: Float = 0f
    ) {
        fun mix(a: Pose, b: Pose, t: Float): Pose {
            torso = a.torso + (b.torso - a.torso) * t
            head = a.head + (b.head - a.head) * t
            lThigh = a.lThigh + (b.lThigh - a.lThigh) * t
            lKnee = a.lKnee + (b.lKnee - a.lKnee) * t
            lSkate = a.lSkate + (b.lSkate - a.lSkate) * t
            rThigh = a.rThigh + (b.rThigh - a.rThigh) * t
            rKnee = a.rKnee + (b.rKnee - a.rKnee) * t
            rSkate = a.rSkate + (b.rSkate - a.rSkate) * t
            lArm = a.lArm + (b.lArm - a.lArm) * t
            lFore = a.lFore + (b.lFore - a.lFore) * t
            rArm = a.rArm + (b.rArm - a.rArm) * t
            rFore = a.rFore + (b.rFore - a.rFore) * t
            return this
        }
    }

    private var phase = Phase.NONE
    private var timer = 0f
    private var target: ContainerPlatform? = null

    private var startHipX = 0f
    private var startHipY = 0f
    private var hipX = 0f
    private var hipY = 0f
    private var flipX0 = 0f
    private var flipY0 = 0f
    private var landX = 0f
    private var recoverX0 = 0f

    private var pendingImpact = 0f

    private val snap = FloatArray(13)   // animator pose snapshot (12 rotations + torso.y)
    private val work = Pose()
    private val hand = FloatArray(2)
    private val preLandDrop = plantedDrop(PRELAND)

    val active: Boolean
        get() = phase != Phase.NONE

    /** Strength of a one-shot camera punch (grab / landing). Returns 0 when nothing is pending. */
    fun consumeCameraImpact(): Float {
        val v = pendingImpact
        pendingImpact = 0f
        return v
    }

    fun reset() {
        phase = Phase.NONE
        timer = 0f
        target = null
        pendingImpact = 0f
        player.setIntroRotation(0f)
        cameraTarget.neutral()
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
        phase = Phase.LEAP
        timer = 0f

        // The animator's last pose is the starting point of the leap.
        startHipX = player.torso.x
        startHipY = player.torso.y
        hipX = startHipX
        hipY = startHipY

        player.stopSlide()

        return true
    }

    // ------------------------------------------------------------------
    // STATE MACHINE (timing, grounded state, camera goals)
    // ------------------------------------------------------------------

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

            Phase.LEAP -> {
                player.vy = -260f
                player.grounded = false

                if (timer >= LEAP_DURATION) {
                    next(Phase.HANG, LEAP_DURATION)
                    pendingImpact = GRAB_IMPACT
                }
            }

            Phase.HANG -> {
                player.vy = 0f
                player.grounded = false

                if (timer >= HANG_DURATION) {
                    next(Phase.FLIP, HANG_DURATION)
                    flipX0 = hipX
                    flipY0 = hipY
                }
            }

            Phase.FLIP -> {
                player.vy = if (timer < FLIP_DURATION * APEX_AT) -380f else 380f
                player.grounded = false

                if (timer >= FLIP_DURATION) {
                    val x = max(platform.x + LAND_INSET, Constants.PLAYER_X)

                    if (platform.overlapsPlayer(x, 16f)) {
                        touchDown(platform, x)
                    } else {
                        cancelAndFall()
                        return
                    }
                }
            }

            Phase.LAND, Phase.HERO, Phase.RECOVER -> {
                player.bottomY = platform.topY
                player.vy = 0f
                player.grounded = true

                if (phase == Phase.RECOVER) {
                    val r = (timer / RECOVER_DURATION).coerceIn(0f, 1f)
                    player.x = lerp(recoverX0, Constants.PLAYER_X, smooth(r))
                } else {
                    player.x = landX
                }

                // The roof ran out under the skater: hand control back to normal physics.
                if (platform.right < player.x + 14f) {
                    finish()
                    return
                }

                when (phase) {
                    Phase.LAND -> {
                        if (timer >= LAND_DURATION) next(Phase.HERO, LAND_DURATION)
                    }

                    Phase.HERO -> {
                        val roofLeft = platform.right - player.x
                        if (timer >= HERO_HOLD_DURATION || roofLeft < 110f) {
                            recoverX0 = player.x
                            next(Phase.RECOVER, HERO_HOLD_DURATION)
                        }
                    }

                    Phase.RECOVER -> {
                        if (timer >= RECOVER_DURATION) {
                            finish()
                            return
                        }
                    }

                    else -> Unit
                }
            }

            Phase.NONE -> Unit
        }

        updateCamera()
    }

    // ------------------------------------------------------------------
    // POSE + POSITION (call AFTER PlayerAnimator.update())
    // ------------------------------------------------------------------

    fun applyPose() {
        if (!active) return

        val platform = target ?: return

        snapshotAnimatorPose()

        when (phase) {

            Phase.LEAP -> {
                val t = (timer / LEAP_DURATION).coerceIn(0f, 1f)
                val s = smooth(t)

                writeRig(work.mix(LEAP_A, HANG_A, s), smooth(t / 0.30f))

                // Aim at where the hands will be pinned on the (moving) edge.
                handOffset(HANG_A)
                val tx = platform.x + HOLD_INSET - hand[0]
                val ty = platform.topY + HOLD_DROP - hand[1]

                placeAirborne(
                    lerp(startHipX, tx, s),
                    lerp(startHipY, ty, s) - HOP_HEIGHT * sin(PI.toFloat() * t)
                )
            }

            Phase.HANG -> {
                val t = (timer / HANG_DURATION).coerceIn(0f, 1f)
                val pull = smooth((t - 0.25f) / 0.75f)

                val p = work.mix(HANG_A, HANG_B, pull)
                writeRig(p, 1f)

                // Hands are glued to the edge; the body is solved from them.
                handOffset(p)
                val dip = CATCH_DIP * sin(PI.toFloat() * (t / 0.35f).coerceIn(0f, 1f))

                placeAirborne(
                    platform.x + HOLD_INSET - hand[0],
                    platform.topY + HOLD_DROP - hand[1] + dip
                )
            }

            Phase.FLIP -> {
                val u = (timer / FLIP_DURATION).coerceIn(0f, 1f)

                writeRig(flipPose(u), 1f)

                // Full forward flip, starts after the push-off and is done before landing.
                player.setIntroRotation(360f * smooth((u - 0.12f) / 0.78f))

                val xLand = max(platform.x + LAND_INSET, Constants.PLAYER_X)
                val yLand = platform.topY - preLandDrop
                val apexY = min2(flipY0, yLand) - APEX_HEIGHT

                val y = if (u < APEX_AT) {
                    val k = u / APEX_AT
                    flipY0 + (apexY - flipY0) * (1f - (1f - k) * (1f - k))
                } else {
                    val k = (u - APEX_AT) / (1f - APEX_AT)
                    apexY + (yLand - apexY) * (k * k)
                }

                placeAirborne(lerp(flipX0, xLand, smooth(u)), y)
            }

            Phase.LAND -> {
                val t = (timer / LAND_DURATION).coerceIn(0f, 1f)
                val fast = 1f - (1f - t) * (1f - t)

                writeRig(work.mix(PRELAND, HERO, fast), 1f)
                plant(LAND_SQUASH * sin(PI.toFloat() * t) + 2f * t, 1f)
            }

            Phase.HERO -> {
                writeRig(HERO, 1f)
                plant(2f, 1f)
            }

            Phase.RECOVER -> {
                val r = (timer / RECOVER_DURATION).coerceIn(0f, 1f)
                val w = 1f - smooth(r)

                // Blend from the hero pose back into the live skating cycle.
                writeRig(HERO, w)
                plant(2f * w, w)
            }

            Phase.NONE -> Unit
        }
    }

    // ------------------------------------------------------------------
    // CAMERA GOALS
    // ------------------------------------------------------------------

    private fun updateCamera() {
        val c = cameraTarget

        when (phase) {

            Phase.NONE -> c.neutral()

            Phase.LEAP -> {
                val t = smooth(timer / LEAP_DURATION)
                c.zoom = lerp(1f, 1.06f, t)
                c.focusDy = -6f * t
                c.panX = -8f * t
                c.tilt = 0f
                c.response = 8f
            }

            Phase.HANG -> {
                // Close in on the grab.
                c.zoom = 1.10f
                c.focusDy = -10f
                c.panX = -10f
                c.tilt = -1.2f
                c.response = 12f
            }

            Phase.FLIP -> {
                val u = (timer / FLIP_DURATION).coerceIn(0f, 1f)
                c.zoom = lerp(1.10f, 1.0f, smooth(u * 1.6f))
                c.focusDy = -6f - 18f * sin(PI.toFloat() * u)
                c.panX = -22f * smooth(u * 1.5f)
                c.tilt = 3.2f * sin(PI.toFloat() * u)
                c.response = 7f
            }

            Phase.LAND -> {
                // Punch in on the impact.
                c.zoom = 1.15f
                c.focusDy = -4f
                c.panX = -18f
                c.tilt = -2.2f
                c.response = 20f
            }

            Phase.HERO -> {
                c.zoom = 1.11f
                c.focusDy = -8f
                c.panX = -16f
                c.tilt = 0f
                c.response = 4f
            }

            Phase.RECOVER -> {
                val r = smooth(timer / RECOVER_DURATION)
                c.zoom = lerp(1.08f, 1f, r)
                c.focusDy = lerp(-6f, 0f, r)
                c.panX = lerp(-14f, 0f, r)
                c.tilt = 0f
                c.response = 5f
            }
        }
    }

    // ------------------------------------------------------------------
    // RIG HELPERS
    // ------------------------------------------------------------------

    private fun snapshotAnimatorPose() {
        snap[0] = player.torso.rotation
        snap[1] = player.head.rotation
        snap[2] = player.leftThigh.rotation
        snap[3] = player.leftShin.rotation
        snap[4] = player.leftSkate.rotation
        snap[5] = player.rightThigh.rotation
        snap[6] = player.rightShin.rotation
        snap[7] = player.rightSkate.rotation
        snap[8] = player.leftUpperArm.rotation
        snap[9] = player.leftForearm.rotation
        snap[10] = player.rightUpperArm.rotation
        snap[11] = player.rightForearm.rotation
        snap[12] = player.torso.y
    }

    /** Writes [p] onto the rig, blended with the animator's pose by [w] (1 = fully the stunt pose). */
    private fun writeRig(p: Pose, w: Float) {
        player.torso.rotation = lerp(snap[0], p.torso, w)
        player.head.rotation = lerp(snap[1], p.head - p.torso, w)

        player.leftThigh.rotation = lerp(snap[2], p.lThigh - p.torso, w)
        player.leftShin.rotation = lerp(snap[3], p.lKnee, w)
        player.leftSkate.rotation = lerp(snap[4], p.lSkate - p.lThigh - p.lKnee, w)

        player.rightThigh.rotation = lerp(snap[5], p.rThigh - p.torso, w)
        player.rightShin.rotation = lerp(snap[6], p.rKnee, w)
        player.rightSkate.rotation = lerp(snap[7], p.rSkate - p.rThigh - p.rKnee, w)

        player.leftUpperArm.rotation = lerp(snap[8], p.lArm - p.torso, w)
        player.leftForearm.rotation = lerp(snap[9], p.lFore, w)
        player.rightUpperArm.rotation = lerp(snap[10], p.rArm - p.torso, w)
        player.rightForearm.rotation = lerp(snap[11], p.rFore, w)
    }

    /** Forward kinematics: near-hand tip relative to the hip, in world units. */
    private fun handOffset(p: Pose) {
        val d = (PI / 180.0).toFloat()
        val t = p.torso * d

        var x = -Rig.SHOULDER_Y * sin(t)
        var y = Rig.SHOULDER_Y * cos(t)

        val a = p.rArm * d
        x += -Rig.UPPER_ARM_LEN * sin(a)
        y += Rig.UPPER_ARM_LEN * cos(a)

        val b = (p.rArm + p.rFore) * d
        val reach = Rig.FOREARM_LEN + Rig.HAND_SIZE * 0.5f
        x += -reach * sin(b)
        y += reach * cos(b)

        hand[0] = x
        hand[1] = y
    }

    private fun currentDrop(): Float {
        val left = SkatingAnimation.footDrop(
            player.leftThigh.rotation + player.torso.rotation,
            player.leftShin.rotation,
            player.leftSkate.rotation,
            Rig.THIGH_LEN, Rig.SHIN_LEN, Rig.SKATE_DROP, Rig.SKATE_REACH
        )
        val right = SkatingAnimation.footDrop(
            player.rightThigh.rotation + player.torso.rotation,
            player.rightShin.rotation,
            player.rightSkate.rotation,
            Rig.THIGH_LEN, Rig.SHIN_LEN, Rig.SKATE_DROP, Rig.SKATE_REACH
        )
        return max(left, right)
    }

    /** Hip position is driven directly (airborne phases). */
    private fun placeAirborne(x: Float, y: Float) {
        hipX = x
        hipY = y

        player.x = x
        player.torso.x = x
        player.torso.y = y
        player.bottomY = y + currentDrop()
        player.grounded = false
    }

    /** Feet planted on the roof; hip follows from the pose (blended with the animator by [w]). */
    private fun plant(bodyY: Float, w: Float) {
        val planted = player.bottomY - currentDrop() + bodyY

        player.torso.x = player.x
        player.torso.y = lerp(snap[12], planted, w)
    }

    private fun flipPose(u: Float): Pose {
        return when {
            u < 0.14f -> work.mix(HANG_B, PUSH, smooth(u / 0.14f))
            u < 0.34f -> work.mix(PUSH, TUCK, smooth((u - 0.14f) / 0.20f))
            u < 0.68f -> work.mix(TUCK, TUCK, 0f)
            u < 0.95f -> work.mix(TUCK, PRELAND, smooth((u - 0.68f) / 0.27f))
            else -> work.mix(PRELAND, PRELAND, 0f)
        }
    }

    // ------------------------------------------------------------------
    // TRANSITIONS
    // ------------------------------------------------------------------

    private fun next(p: Phase, finishedDuration: Float) {
        phase = p
        timer = max(0f, timer - finishedDuration)
    }

    private fun touchDown(platform: ContainerPlatform, x: Float) {
        player.setIntroRotation(0f)
        player.x = x
        landX = x
        player.bottomY = platform.topY
        player.vy = 0f
        player.grounded = true
        player.markLanded()

        pendingImpact = LAND_IMPACT
        next(Phase.LAND, FLIP_DURATION)
    }

    /** Stunt finished: continue with normal physics. */
    private fun finish() {
        val platform = target

        player.setIntroRotation(0f)
        phase = Phase.NONE
        timer = 0f
        target = null
        cameraTarget.neutral()

        if (platform != null && platform.active && platform.overlapsPlayer(player.x, 14f)) {
            player.bottomY = platform.topY
            player.vy = 0f
            player.grounded = true
        } else {
            player.vy = 0f
            player.grounded = false
        }
    }

    private fun cancelAndFall() {
        player.setIntroRotation(0f)
        phase = Phase.NONE
        timer = 0f
        target = null
        cameraTarget.neutral()
        player.x = Constants.PLAYER_X
        player.grounded = false
        player.vy = 180f
    }

    private fun smooth(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun min2(a: Float, b: Float): Float = if (a < b) a else b

    private companion object {

        // ---- timing (seconds) ----
        const val LEAP_DURATION = 0.30f
        const val HANG_DURATION = 0.20f
        const val FLIP_DURATION = 0.62f
        const val LAND_DURATION = 0.14f
        const val HERO_HOLD_DURATION = 0.28f
        const val RECOVER_DURATION = 0.45f

        // ---- geometry (world units) ----
        const val HOLD_INSET = 3f        // hand sits this far past the container edge
        const val HOLD_DROP = 2f         // hand sits this far below the roof surface line
        const val HOP_HEIGHT = 22f       // extra hop during the leap
        const val CATCH_DIP = 5f         // body stretch when the hands catch the edge
        const val APEX_HEIGHT = 46f      // flip apex above the landing hip height
        const val APEX_AT = 0.55f        // fraction of the flip at which the apex is reached
        const val LAND_INSET = 46f       // landing spot, measured from the container's left edge
        const val LAND_SQUASH = 7f       // extra crouch on impact

        // ---- camera punches ----
        const val GRAB_IMPACT = 4.5f
        const val LAND_IMPACT = 9.5f

        // ---- poses (angles in degrees; thighs/arms/head/skates are ABSOLUTE) ----
        // Convention: 0 = limb hanging straight down, negative = swings forward, positive = backward.

        // Launch: stretched, arms reaching forward-up for the edge.
        private val LEAP_A = Pose(
            torso = 12f, head = 5f,
            lThigh = -35f, lKnee = 45f, lSkate = 5f,
            rThigh = 10f, rKnee = 25f, rSkate = 0f,
            lArm = -120f, lFore = -20f,
            rArm = -125f, rFore = -18f
        )

        // Grab: legs tucked up, arms forward-up on the ledge.
        private val HANG_A = Pose(
            torso = 16f, head = 0f,
            lThigh = -85f, lKnee = 95f, lSkate = 0f,
            rThigh = -68f, rKnee = 90f, rSkate = 0f,
            lArm = -125f, lFore = -20f,
            rArm = -128f, rFore = -18f
        )

        // Pull-up: elbows folded, chest at the edge, legs loaded backwards.
        private val HANG_B = Pose(
            torso = 8f, head = -6f,
            lThigh = 28f, lKnee = 55f, lSkate = 0f,
            rThigh = 40f, rKnee = 60f, rSkate = 0f,
            lArm = 28f, lFore = -168f,
            rArm = 30f, rFore = -170f
        )

        // Push off the edge: legs kick forward, arms drive down.
        private val PUSH = Pose(
            torso = -4f, head = -10f,
            lThigh = -50f, lKnee = 55f, lSkate = 10f,
            rThigh = -35f, rKnee = 60f, rSkate = 5f,
            lArm = 40f, lFore = -10f,
            rArm = 45f, rFore = -10f
        )

        // Tight tuck for the flip.
        private val TUCK = Pose(
            torso = 52f, head = 25f,
            lThigh = -105f, lKnee = 128f, lSkate = 0f,
            rThigh = -98f, rKnee = 120f, rSkate = 0f,
            lArm = -55f, lFore = -75f,
            rArm = -60f, rFore = -70f
        )

        // Open up, knees soft, arms out for balance.
        private val PRELAND = Pose(
            torso = 26f, head = 4f,
            lThigh = -22f, lKnee = 48f, lSkate = 0f,
            rThigh = -35f, rKnee = 55f, rSkate = 0f,
            lArm = -75f, lFore = -15f,
            rArm = -95f, rFore = -15f
        )

        // Superhero landing: front foot planted, back leg trailing, fist down, other arm back.
        private val HERO = Pose(
            torso = 68f, head = 12f,
            lThigh = 52f, lKnee = 4f, lSkate = 0f,
            rThigh = -68f, rKnee = 112f, rSkate = 0f,
            lArm = 105f, lFore = -10f,
            rArm = -12f, rFore = 0f
        )

        private fun plantedDrop(p: Pose): Float {
            val left = SkatingAnimation.footDrop(
                p.lThigh, p.lKnee, p.lSkate - p.lThigh - p.lKnee,
                Rig.THIGH_LEN, Rig.SHIN_LEN, Rig.SKATE_DROP, Rig.SKATE_REACH
            )
            val right = SkatingAnimation.footDrop(
                p.rThigh, p.rKnee, p.rSkate - p.rThigh - p.rKnee,
                Rig.THIGH_LEN, Rig.SHIN_LEN, Rig.SKATE_DROP, Rig.SKATE_REACH
            )
            return max(left, right)
        }
    }
}