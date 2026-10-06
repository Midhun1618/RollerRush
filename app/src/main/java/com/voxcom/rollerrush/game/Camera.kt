package com.voxcom.rollerrush.game

import android.graphics.Canvas
import com.voxcom.rollerrush.utils.Constants
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * World -> screen conversion plus cinematic camera effects.
 *
 * Camera effects:
 * - Jump kick
 * - Landing shake / zoom
 * - Crash shake / zoom
 * - Speed boost chase camera
 * - Slide cinematic camera
 */
class Camera {

    var screenWidth = 1f
        private set

    var screenHeight = 1f
        private set

    var scale = 1f
        private set

    var offsetY = 0f
        private set

    @Volatile
    var viewWidth = Constants.MIN_VIEW_WIDTH
        private set

    // ------------------------------------------------------------
    // GENERAL CAMERA SHAKE
    // ------------------------------------------------------------

    private var shakeTime = 0f
    private var shakeDuration = 0f
    private var shakeStrength = 0f
    private var shakePhase = 0f

    // ------------------------------------------------------------
    // GENERAL ZOOM
    // ------------------------------------------------------------

    private var zoom = 1f
    private var zoomStart = 1f
    private var zoomTarget = 1f
    private var zoomTime = 0f
    private var zoomDuration = 0f
    private var zoomReturning = false

    // ------------------------------------------------------------
    // SPEED BOOST CAMERA
    // ------------------------------------------------------------

    private var boostActive = false
    private var boostBlend = 0f
    private var panX = 0f

    // ------------------------------------------------------------
    // SLIDE CAMERA
    // ------------------------------------------------------------

    private var slideActive = false
    private var slideAmount = 0f
    private var slideTime = 0f

    private var slideShakeX = 0f
    private var slideShakeY = 0f

    private var slideZoom = 1f
    private var slideOffsetY = 0f
    private var slideTilt = 0f

    private var slideSeed = 0f

    // ------------------------------------------------------------
    // SIZE
    // ------------------------------------------------------------

    fun onSizeChanged(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return

        screenWidth = w.toFloat()
        screenHeight = h.toFloat()

        scale = min(
            screenHeight / Constants.WORLD_HEIGHT,
            screenWidth / Constants.MIN_VIEW_WIDTH
        )

        offsetY = screenHeight - Constants.WORLD_HEIGHT * scale

        viewWidth = screenWidth / scale
    }

    // ------------------------------------------------------------
    // UPDATE
    // ------------------------------------------------------------

    fun update(dt: Float) {

        // --------------------------------------------------------
        // GENERAL SHAKE
        // --------------------------------------------------------

        if (shakeTime > 0f) {
            shakeTime = max(0f, shakeTime - dt)

            shakePhase += dt * 42f

            if (shakeTime <= 0f) {
                shakeStrength = 0f
            }
        }

        // --------------------------------------------------------
        // SPEED BOOST
        // --------------------------------------------------------

        val boostTarget =
            if (boostActive) 1f else 0f

        boostBlend +=
            (boostTarget - boostBlend) *
                    (1f - exp(-dt * 8f))

        panX =
            Constants.SPEED_BOOST_CAMERA_PAN * boostBlend

        // --------------------------------------------------------
        // GENERAL ZOOM
        // --------------------------------------------------------

        if (boostActive) {

            zoom =
                1f +
                        (Constants.SPEED_BOOST_CAMERA_ZOOM - 1f) *
                        boostBlend

        } else if (zoomTime < zoomDuration) {

            zoomTime += dt

            val t =
                (zoomTime / zoomDuration)
                    .coerceIn(0f, 1f)

            val eased =
                t * t * (3f - 2f * t)

            zoom =
                zoomStart +
                        (zoomTarget - zoomStart) * eased

        } else if (!zoomReturning && zoomTarget != 1f) {

            zoomStart = zoom
            zoomTarget = 1f
            zoomTime = 0f
            zoomDuration = 0.24f
            zoomReturning = true

        } else {

            zoom = zoomTarget
        }

        // --------------------------------------------------------
        // SLIDE CINEMATIC CAMERA
        // --------------------------------------------------------

        val slideTarget =
            if (slideActive) 1f else 0f

        // Smoothly enter and exit slide camera.
        slideAmount +=
            (slideTarget - slideAmount) *
                    min(1f, dt * 9f)

        slideTime += dt

        if (slideAmount > 0.001f) {

            val motionTime =
                slideTime * 7f + slideSeed

            // Very subtle organic camera movement.
            slideShakeX =
                sin(motionTime * 1.7f) *
                        1.8f *
                        slideAmount

            slideShakeY =
                cos(motionTime * 2.1f) *
                        1.2f *
                        slideAmount

            // Slight close-up.
            slideZoom =
                1f + 0.075f * slideAmount

            // Move camera downward.
            slideOffsetY =
                18f * slideAmount

            // Small cinematic tilt.
            slideTilt =
                -1.75f * slideAmount

        } else {

            slideShakeX = 0f
            slideShakeY = 0f
            slideZoom = 1f
            slideOffsetY = 0f
            slideTilt = 0f
        }
    }

    // ------------------------------------------------------------
    // RESET
    // ------------------------------------------------------------

    fun resetEffects() {

        shakeTime = 0f
        shakeDuration = 0f
        shakeStrength = 0f
        shakePhase = 0f

        zoom = 1f
        zoomStart = 1f
        zoomTarget = 1f
        zoomTime = 0f
        zoomDuration = 0f
        zoomReturning = false

        boostActive = false
        boostBlend = 0f
        panX = 0f

        slideActive = false
        slideAmount = 0f
        slideTime = 0f

        slideShakeX = 0f
        slideShakeY = 0f

        slideZoom = 1f
        slideOffsetY = 0f
        slideTilt = 0f

        slideSeed = 0f
    }

    // ------------------------------------------------------------
    // SPEED BOOST
    // ------------------------------------------------------------

    fun setSpeedBoost(active: Boolean) {

        if (boostActive == active) return

        boostActive = active

        if (active) {

            addShake(
                strength = 2.5f,
                duration = 0.14f
            )

            zoomStart = zoom
            zoomTarget =
                Constants.SPEED_BOOST_CAMERA_ZOOM

            zoomTime = 0f
            zoomDuration = 0.18f
            zoomReturning = false

        } else {

            zoomStart = zoom
            zoomTarget = 1f
            zoomTime = 0f
            zoomDuration = 0.28f
            zoomReturning = true
        }
    }

    // ------------------------------------------------------------
    // JUMP
    // ------------------------------------------------------------

    fun triggerJump() {

        addShake(
            strength = 2.0f,
            duration = 0.10f
        )

        kickZoom(
            target = 1.035f,
            duration = 0.14f
        )
    }

    // ------------------------------------------------------------
    // LANDING
    // ------------------------------------------------------------

    fun triggerLanding(impact: Float = 1f) {

        val strength =
            (3.5f + impact * 5.0f)
                .coerceAtMost(8.5f)

        addShake(
            strength = strength,
            duration = 0.16f
        )

        kickZoom(
            target =
                (1.035f + impact * 0.025f)
                    .coerceAtMost(1.07f),
            duration = 0.20f
        )
    }

    // ------------------------------------------------------------
    // SLIDE
    // ------------------------------------------------------------

    fun triggerSlide() {

        slideActive = true
        slideTime = 0f
        slideSeed += 17.37f

        // Small initial camera movement.
        addShake(
            strength = 1.5f,
            duration = 0.08f
        )
    }

    fun endSlide() {

        slideActive = false
    }

    // ------------------------------------------------------------
    // CRASH
    // ------------------------------------------------------------

    fun triggerCrash() {

        addShake(
            strength = 12f,
            duration = 0.42f
        )

        kickZoom(
            target = 1.12f,
            duration = 0.30f
        )
    }

    // ------------------------------------------------------------
    // SHAKE
    // ------------------------------------------------------------

    private fun addShake(
        strength: Float,
        duration: Float
    ) {

        shakeStrength =
            max(shakeStrength, strength)

        shakeDuration =
            max(shakeDuration, duration)

        shakeTime =
            max(shakeTime, duration)
    }

    // ------------------------------------------------------------
    // ZOOM KICK
    // ------------------------------------------------------------

    private fun kickZoom(
        target: Float,
        duration: Float
    ) {

        if (boostActive) return

        zoomStart = zoom
        zoomTarget = target
        zoomTime = 0f
        zoomDuration = duration
        zoomReturning = false
    }

    // ------------------------------------------------------------
    // APPLY CAMERA
    // ------------------------------------------------------------

    fun applyTo(
        canvas: Canvas,
        focusX: Float = Constants.PLAYER_X,
        focusY: Float = Constants.GROUND_Y - 65f
    ) {

        // General camera shake.
        val progress =
            if (shakeDuration <= 0f) {
                0f
            } else {
                shakeTime / shakeDuration
            }

        val envelope =
            progress * progress

        val shakeX =
            sin(shakePhase) *
                    shakeStrength *
                    envelope

        val shakeY =
            sin(shakePhase * 1.37f) *
                    shakeStrength *
                    0.72f *
                    envelope

        // --------------------------------------------------------
        // COMBINE GENERAL ZOOM + SLIDE ZOOM
        // --------------------------------------------------------

        val finalZoom =
            zoom * slideZoom

        val zScale =
            scale * finalZoom

        // --------------------------------------------------------
        // SCREEN POSITION
        // --------------------------------------------------------

        val screenFocusX =
            focusX * scale

        val screenFocusY =
            offsetY +
                    focusY * scale

        // Slide movement needs to be scaled with the world.
        val finalX =
            screenFocusX +
                    panX * scale +
                    slideShakeX +
                    shakeX

        val finalY =
            screenFocusY +
                    slideOffsetY * scale +
                    slideShakeY +
                    shakeY

        // --------------------------------------------------------
        // TRANSFORM
        // --------------------------------------------------------

        canvas.save()

        canvas.translate(
            finalX,
            finalY
        )

        // Slide camera tilt.
        canvas.rotate(slideTilt)

        canvas.scale(
            zScale,
            zScale
        )

        canvas.translate(
            -focusX,
            -focusY
        )
    }

    // ------------------------------------------------------------
    // WORLD / SCREEN CONVERSION
    // ------------------------------------------------------------

    fun worldToScreenX(wx: Float): Float =
        wx * scale

    fun worldToScreenY(wy: Float): Float =
        wy * scale + offsetY

    fun screenToWorldX(sx: Float): Float =
        sx / scale

    fun screenToWorldY(sy: Float): Float =
        (sy - offsetY) / scale
}