package com.voxcom.rollerrush.animation

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Procedural roller-skating cycle (no sprite frames).
 *
 * One cycle = 1.0 and is split into five phases:
 *
 *   0.00 - 0.22  PHASE 1  left leg PUSHES backward   (right leg supports)
 *   0.22 - 0.42  PHASE 2  left leg RECOVERS under the body
 *   0.42 - 0.64  PHASE 3  right leg PUSHES backward  (left leg supports)
 *   0.64 - 0.84  PHASE 4  right leg RECOVERS under the body
 *   0.84 - 1.00  PHASE 5  short centred GLIDE (both legs under the body)
 *
 * The right leg runs the exact same curve as the left, just shifted by [RIGHT_OFFSET].
 * That is what makes it push -> recover -> push -> recover instead of a "running" step.
 * Every transition uses smoothstep / sin^2 so velocities are zero at phase borders
 * and the loop is seamless.
 */
class SkatingAnimation {

    /** Normalised cycle position 0..1. */
    var cycle = 0f
        private set

    private val leg = FloatArray(3) // [thigh, knee, absolute skate angle]

    fun reset() { cycle = 0f }

    /**
     * Advances the cycle by [dt] and writes the skating pose into [out].
     * [speedFactor] ~1 at base speed; faster world => slightly faster strokes.
     */
    fun update(dt: Float, speedFactor: Float, out: Pose) {
        cycle = (cycle + dt * speedFactor / CYCLE_SECONDS) % 1f

        legCurve(cycle, leg)
        val leftThigh = leg[0]
        out.leftThigh = leg[0]; out.leftKnee = leg[1]
        out.leftSkate = leg[2] - leg[0] - leg[1]     // convert absolute skate angle -> relative to shin

        legCurve((cycle - RIGHT_OFFSET + 1f) % 1f, leg)
        val rightThigh = leg[0]
        out.rightThigh = leg[0]; out.rightKnee = leg[1]
        out.rightSkate = leg[2] - leg[0] - leg[1]

        // Torso: constant forward lean, leaning a touch more into each push.
        out.torsoRotation = BASE_LEAN + 0.05f * max(leftThigh, rightThigh)
        // Head stabilisation: cancel most of the torso rotation so the head stays level.
        out.headRotation = -out.torsoRotation * 0.55f
        // Hips shift slightly toward the pushing side's opposite foot.
        out.hipOffsetX = 0.05f * (leftThigh - rightThigh)
        // Tiny vertical bob, twice per cycle (once per stroke). Most height change comes from knee flex.
        out.bodyOffsetY = -0.8f * sin(4f * PI.toFloat() * cycle)

        // Arms counter-swing with the skating stroke.
        // When a leg pushes backward, the arm on that side moves forward;
        // when that leg recovers, the arm sweeps backward. This keeps the
        // upper body alive while preserving the low speed-skater posture.
        val leftArmSwing = -30f * sin(2f * PI.toFloat() * cycle)
        val rightArmSwing = -30f * sin(2f * PI.toFloat() * (cycle - RIGHT_OFFSET))

        out.leftArm = 32f + leftArmSwing
        out.rightArm = 32f + rightArmSwing

        // Keep the elbows bent and tucked behind the torso. The forearms
        // follow the swing with a smaller amplitude so the hands do not flap.
        out.leftForearm = -24f - 7f * sin(2f * PI.toFloat() * cycle + 0.35f)
        out.rightForearm = -24f - 7f * sin(2f * PI.toFloat() * (cycle - RIGHT_OFFSET) + 0.35f)
    }

    companion object {
        const val CYCLE_SECONDS = 1.35f
        private const val RIGHT_OFFSET = 0.50f
        private const val PUSH_END = 0.22f
        private const val RECOVER_END = 0.42f

        // Key leg poses (degrees)
        private const val SUP_T = -18f   // supporting leg: thigh slightly forward/up
        private const val SUP_K = 52f    // supporting leg: deep knee bend for a low skating stance
        private const val PUSH_T = 58f   // pushing leg: strongly swept backward
        private const val PUSH_K = -4f   // pushing leg: nearly straight
        private const val BASE_LEAN = 50f

        private fun smooth(x: Float) = x * x * (3f - 2f * x)
        private fun bump(x: Float): Float { val s = sin(x * PI.toFloat()); return s * s }
        private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

        /**
         * Pose of ONE leg at its own cycle position [c] (0..1).
         * out = [thigh (world angle), knee (relative), skate (ABSOLUTE angle)].
         */
        private fun legCurve(c: Float, out: FloatArray) {
            when {
                c < PUSH_END -> {                         // PUSH: sweep back, straighten, heel lifts
                    val s = smooth(c / PUSH_END)
                    out[0] = lerp(SUP_T, PUSH_T, s)
                    out[1] = lerp(SUP_K, PUSH_K, s)
                    out[2] = lerp(0f, 5f, s)
                }
                c < RECOVER_END -> {                      // RECOVER: fold the leg and bring it forward
                    val u = (c - PUSH_END) / (RECOVER_END - PUSH_END)
                    val s = smooth(u)
                    val b = bump(u)                       // extra knee fold / lift mid-recovery
                    out[0] = lerp(PUSH_T, SUP_T, s) - 14f * b
                    out[1] = lerp(PUSH_K, SUP_K, s) + 38f * b
                    out[2] = lerp(5f, 0f, s) - 8f * b    // toe up slightly while the foot is airborne
                }
                else -> {                                 // SUPPORT / GLIDE: carry weight, sink during other leg's push
                    val u = (c - RECOVER_END) / (1f - RECOVER_END)
                    val w = bump(min(u / 0.38f, 1f))
                    out[0] = SUP_T - 2f * w
                    out[1] = SUP_K + 6f * w
                    out[2] = 0f
                }
            }
        }

        /** Jump / airborne pose: legs tucked (compressed), arms counterbalancing, body tilting with vertical speed. */
        fun airPose(vy: Float, out: Pose) {
            // Left leg forward, right leg trailing.
            out.leftThigh = -35f; out.leftKnee = 70f
            out.leftSkate = 8f - (-35f + 70f)          // keep skate roughly level (absolute +8)
            out.rightThigh = 8f; out.rightKnee = 55f
            out.rightSkate = 10f - (8f + 55f)
            // vy > 0 means falling: lean forward when falling, back when rising.
            out.torsoRotation = 6f + (vy * 0.015f).coerceIn(-9f, 9f)
            out.headRotation = -out.torsoRotation * 0.55f
            out.hipOffsetX = 0f
            out.bodyOffsetY = 0f
            // Right arm forward against the forward left leg (counterbalance).
            out.leftArm = -25f; out.leftForearm = -30f
            out.rightArm = -80f; out.rightForearm = -45f
        }

        /** Tumbling pose used after a collision. [t] = seconds since the crash. */
        fun crashPose(t: Float, out: Pose) {
            out.torsoRotation = -75f
            out.headRotation = 20f
            out.hipOffsetX = 0f
            out.bodyOffsetY = 0f
            out.leftThigh = 50f; out.leftKnee = 20f; out.leftSkate = -40f
            out.rightThigh = -40f; out.rightKnee = 50f; out.rightSkate = -20f
            val flail = sin(t * 18f) * 12f
            out.leftArm = -100f + flail; out.leftForearm = -20f
            out.rightArm = -70f - flail; out.rightForearm = -30f
        }

        /** Vertical distance from the hip joint down to the lowest point of the skate. Used for ground contact. */
        fun footDrop(thigh: Float, knee: Float, skateRel: Float, thighLen: Float, shinLen: Float,
                     skateDrop: Float, skateReach: Float): Float {
            val d = (PI / 180.0).toFloat()
            val shinAbs = thigh + knee
            val skateAbs = shinAbs + skateRel
            return thighLen * kotlin.math.cos(thigh * d) +
                    shinLen * kotlin.math.cos(shinAbs * d) +
                    skateDrop * kotlin.math.cos(skateAbs * d) +
                    skateReach * abs(sin(skateAbs * d))
        }
    }
}
