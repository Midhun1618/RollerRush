package com.voxcom.rollerrush.animation

enum class AnimationState { SKATING, JUMPING, FALLING, LANDING, SLIDING, CRASHED }

/**
 * One complete pose of the rig. Angles are in degrees, positive = clockwise on screen.
 * The skater faces RIGHT, so for a leg/arm "positive" means swung BACKWARD and "negative" FORWARD.
 *
 * Thigh / knee / skate semantics (important for the FK maths in PlayerAnimator):
 *  - thigh      : WORLD angle of the thigh measured from "straight down" (torso lean is compensated).
 *  - knee       : angle of the lower leg RELATIVE to the thigh (>= 0 bends the knee).
 *  - skate      : angle of the skate RELATIVE to the lower leg.
 *  - arms       : relative to the torso.
 */
class Pose {
    var leftThigh = 0f; var leftKnee = 0f; var leftSkate = 0f
    var rightThigh = 0f; var rightKnee = 0f; var rightSkate = 0f
    var torsoRotation = 0f
    var headRotation = 0f      // relative to torso
    var hipOffsetX = 0f
    var bodyOffsetY = 0f
    var leftArm = 0f; var leftForearm = 0f
    var rightArm = 0f; var rightForearm = 0f

    /** this = a + (b - a) * t  (a or b may be `this`). No allocation. */
    fun lerp(a: Pose, b: Pose, t: Float) {
        leftThigh = a.leftThigh + (b.leftThigh - a.leftThigh) * t
        leftKnee = a.leftKnee + (b.leftKnee - a.leftKnee) * t
        leftSkate = a.leftSkate + (b.leftSkate - a.leftSkate) * t
        rightThigh = a.rightThigh + (b.rightThigh - a.rightThigh) * t
        rightKnee = a.rightKnee + (b.rightKnee - a.rightKnee) * t
        rightSkate = a.rightSkate + (b.rightSkate - a.rightSkate) * t
        torsoRotation = a.torsoRotation + (b.torsoRotation - a.torsoRotation) * t
        headRotation = a.headRotation + (b.headRotation - a.headRotation) * t
        hipOffsetX = a.hipOffsetX + (b.hipOffsetX - a.hipOffsetX) * t
        bodyOffsetY = a.bodyOffsetY + (b.bodyOffsetY - a.bodyOffsetY) * t
        leftArm = a.leftArm + (b.leftArm - a.leftArm) * t
        leftForearm = a.leftForearm + (b.leftForearm - a.leftForearm) * t
        rightArm = a.rightArm + (b.rightArm - a.rightArm) * t
        rightForearm = a.rightForearm + (b.rightForearm - a.rightForearm) * t
    }
}
