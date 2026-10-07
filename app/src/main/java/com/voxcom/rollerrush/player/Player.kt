package com.voxcom.rollerrush.player

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.voxcom.rollerrush.utils.Constants

class PlayerSprites(
    val head: Bitmap, val hair: Bitmap, val torso: Bitmap,
    val upperArm: Bitmap, val forearm: Bitmap, val hand: Bitmap,
    val thigh: Bitmap, val shin: Bitmap, val skate: Bitmap,
    val upperArmFar: Bitmap, val forearmFar: Bitmap, val handFar: Bitmap,
    val thighFar: Bitmap, val shinFar: Bitmap, val skateFar: Bitmap
) {
    fun recycle() {
        listOf(head, hair, torso, upperArm, forearm, hand, thigh, shin, skate,
            upperArmFar, forearmFar, handFar, thighFar, shinFar, skateFar)
            .forEach { if (!it.isRecycled) it.recycle() }
    }
}

/**
 * The skater: physics state + the body-part rig.
 *
 * PLAYER HIERARCHY (built in init):
 *
 *  torso (root, pivot = hip joint; world position set by PlayerAnimator every frame)
 *   |- head --- hair
 *   |- leftUpperArm -- leftForearm -- leftHand           (far side, drawn behind torso)
 *   |- rightUpperArm -- rightForearm -- rightHand
 *   |- leftThigh -- leftShin -- leftSkate               (far side, drawn behind torso)
 *   '- rightThigh -- rightShin -- rightSkate
 *
 * Physics coordinates: x is the (fixed) horizontal position; bottomY is the y of the
 * lowest wheel point (where the skates touch the ground). y grows downward.
 */
class Player(private val sprites: PlayerSprites) {

    var x = Constants.PLAYER_X
    var bottomY = Constants.GROUND_Y
    var vy = 0f
    var grounded = true
    var crashed = false
    var crashVx = 0f
    var sliding = false
        private set

    private var slideTime = 0f
    private var landedEvent = false

    // Cinematic intro root rotation.
    // This is separate from torso.rotation so the intro flip
    // never interferes with normal skating poses.
    var introRotation = 0f
        private set


    // Gameplay hitboxes (world units). Updated by updateHitboxes().
    val bodyHitbox = RectF()   // torso/head
    val feetHitbox = RectF()   // skates + lower legs (ground detection of obstacles)

    // ---- Rig -----------------------------------------------------------
    val torso = BodyPart("torso", sprites.torso, Rig.TORSO_W, Rig.TORSO_H, Rig.TORSO_PIVOT_X, Rig.TORSO_PIVOT_Y)
    val head = BodyPart("head", sprites.head, Rig.HEAD_W, Rig.HEAD_H, Rig.HEAD_PIVOT_X, Rig.HEAD_PIVOT_Y)
    val hair = BodyPart("hair", sprites.hair, Rig.HAIR_W, Rig.HAIR_H, Rig.HAIR_PIVOT_X, Rig.HAIR_PIVOT_Y)

    val leftUpperArm = limb("leftUpperArm", sprites.upperArmFar, Rig.UPPER_ARM_LEN, Rig.UPPER_ARM_THICK)
    val leftForearm = limb("leftForearm", sprites.forearmFar, Rig.FOREARM_LEN, Rig.FOREARM_THICK)
    val leftHand = hand("leftHand", sprites.handFar)
    val rightUpperArm = limb("rightUpperArm", sprites.upperArm, Rig.UPPER_ARM_LEN, Rig.UPPER_ARM_THICK)
    val rightForearm = limb("rightForearm", sprites.forearm, Rig.FOREARM_LEN, Rig.FOREARM_THICK)
    val rightHand = hand("rightHand", sprites.hand)

    val leftThigh = limb("leftThigh", sprites.thighFar, Rig.THIGH_LEN, Rig.THIGH_THICK)
    val leftShin = limb("leftShin", sprites.shinFar, Rig.SHIN_LEN, Rig.SHIN_THICK)
    val leftSkate = skate("leftSkate", sprites.skateFar)
    val rightThigh = limb("rightThigh", sprites.thigh, Rig.THIGH_LEN, Rig.THIGH_THICK)
    val rightShin = limb("rightShin", sprites.shin, Rig.SHIN_LEN, Rig.SHIN_THICK)
    val rightSkate = skate("rightSkate", sprites.skate)

    init {
        torso.addChild(head, 0f, Rig.NECK_Y)
        head.addChild(hair, -1f, -10f)

        torso.addChild(leftUpperArm, 0f, Rig.SHOULDER_Y, behind = true)
        leftUpperArm.addChild(leftForearm, 0f, Rig.UPPER_ARM_LEN)
        leftForearm.addChild(leftHand, 0f, Rig.FOREARM_LEN)


        torso.addChild(leftThigh, 0f, 0f, behind = true)
        leftThigh.addChild(leftShin, 0f, Rig.THIGH_LEN)
        leftShin.addChild(leftSkate, 0f, Rig.SHIN_LEN)

        torso.addChild(rightThigh, 0f, 0f)
        rightThigh.addChild(rightShin, 0f, Rig.THIGH_LEN)
        rightShin.addChild(rightSkate, 0f, Rig.SHIN_LEN)

        torso.addChild(rightUpperArm, 0f, Rig.SHOULDER_Y)
        rightUpperArm.addChild(rightForearm, 0f, Rig.UPPER_ARM_LEN)
        rightForearm.addChild(rightHand, 0f, Rig.FOREARM_LEN)

        updateHitboxes()
    }

    fun setIntroRotation(rotation: Float) {
        introRotation = rotation
    }

    fun draw(canvas: Canvas, paint: Paint) {
        if (introRotation == 0f) {
            torso.draw(canvas, paint)
            return
        }

        canvas.save()

        // Rotate around the torso/root position.
        canvas.translate(torso.x, torso.y)
        canvas.rotate(introRotation)
        canvas.translate(-torso.x, -torso.y)

        torso.draw(canvas, paint)

        canvas.restore()
    }

    /**
     * Gameplay hitboxes. Intentionally smaller than the art:
     *  - body box: a narrow column through torso + head (forgiving to side contact)
     *  - feet box: the skates, used for obstacles that are low to the ground
     */
    fun updateHitboxes() {
        val bodyTop = if (sliding) Rig.SLIDE_BODY_BOX_TOP else Rig.BODY_BOX_TOP
        val bodyBottom = if (sliding) Rig.SLIDE_BODY_BOX_BOTTOM else Rig.BODY_BOX_BOTTOM
        bodyHitbox.set(x - Rig.BODY_BOX_HALF_W, bottomY - bodyTop,
            x + Rig.BODY_BOX_HALF_W, bottomY - bodyBottom)
        feetHitbox.set(x - Rig.FEET_BOX_LEFT, bottomY - Rig.FEET_BOX_TOP,
            x + Rig.FEET_BOX_RIGHT, bottomY - Rig.FEET_BOX_BOTTOM)
    }

    fun markLanded() { landedEvent = true }
    fun consumeLanded(): Boolean { val v = landedEvent; landedEvent = false; return v }
    fun hasLandedEvent(): Boolean = landedEvent

    fun startSlide(duration: Float) {
        if (!crashed && grounded) {
            sliding = true
            slideTime = duration
        }
    }

    fun updateSlide(dt: Float) {
        if (!sliding) return
        slideTime -= dt
        if (slideTime <= 0f) {
            slideTime = 0f
            sliding = false
        }
    }

    fun stopSlide() {
        sliding = false
        slideTime = 0f
    }

    fun crash() {
        if (crashed) return
        crashed = true
        vy = -260f
        crashVx = -90f
        grounded = false
    }

    fun reset() {
        x = Constants.PLAYER_X
        bottomY = Constants.GROUND_Y
        vy = 0f
        grounded = true
        crashed = false
        crashVx = 0f
        sliding = false
        slideTime = 0f
        landedEvent = false
        introRotation = 0f
        updateHitboxes()
    }

    private fun limb(name: String, bmp: Bitmap, len: Float, thick: Float) =
        BodyPart(name, bmp, thick, len + thick, thick / 2f, thick / 2f)

    private fun hand(name: String, bmp: Bitmap) =
        BodyPart(name, bmp, Rig.HAND_SIZE, Rig.HAND_SIZE, Rig.HAND_SIZE / 2f, Rig.HAND_SIZE / 2f)

    private fun skate(name: String, bmp: Bitmap) =
        BodyPart(name, bmp, Rig.SKATE_W, Rig.SKATE_H, Rig.SKATE_PIVOT_X, Rig.SKATE_PIVOT_Y)
}
