package com.voxcom.rollerrush.game

import android.graphics.Canvas
import com.voxcom.rollerrush.player.StuntCameraTarget
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
 * - Lively jump camera (crouch dip, takeoff snap, height follow, landing thump)
 * - Container stunt camera (zoom / look-up / pan / tilt, chased smoothly)
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

    @Volatile
    var visibleLeft = 0f
        private set

    @Volatile
    var visibleRight = Constants.MIN_VIEW_WIDTH
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
    // JUMP CAMERA
    // ------------------------------------------------------------

    // Two small damped springs react to impulses: vertical bob (units) and tilt (deg).
    private var kickY = 0f
    private var kickYVel = 0f
    private var kickTilt = 0f
    private var kickTiltVel = 0f

    // Camera rises with the skater (lagging behind), then settles back after landing.
    private var airHeight = 0f
    private var followDy = 0f

    // Gentle zoom that breathes with the jump (>= 1 so no extra world is revealed).
    private var jumpZoom = 1f

    /** Called every frame by GameWorld: skater height above the surface he took off from (>= 0). */
    fun setAirHeight(height: Float) {
        airHeight = height.coerceAtLeast(0f)
    }

    /** Crouch before takeoff: camera dips with the body. */
    fun triggerJumpAnticipation() {
        kickYVel += 46f
    }

    // ------------------------------------------------------------
    // CONTAINER STUNT CAMERA
    // ------------------------------------------------------------

    // Goals come from ContainerStuntController.cameraTarget; the camera chases them
    // with exponential easing, so entering AND leaving the stunt framing is smooth.
    private var stuntTarget: StuntCameraTarget? = null

    private var stuntZoom = 1f
    private var stuntDy = 0f
    private var stuntPanX = 0f
    private var stuntTilt = 0f

    fun setStuntTarget(target: StuntCameraTarget?) {
        stuntTarget = target
    }

    /** One-shot camera punch (ledge grab / heroic landing). */
    fun triggerStuntImpact(strength: Float) {
        addShake(
            strength = strength,
            duration = 0.24f
        )
    }

    // ------------------------------------------------------------
    // INTRO CAMERA
    // ------------------------------------------------------------

    // 0 = normal gameplay framing, 1 = full intro framing. Driven (and eased)
    // by GameWorld from the intro timeline, so the camera is already back to
    // normal when gameplay starts.
    private var introBlend = 0f
    // ------------------------------------------------------------
    // CRASH CAMERA
    // ------------------------------------------------------------

    private var crashTiltTime = 1f
    private var crashTilt = 0f

    fun setIntroBlend(blend: Float) {
        introBlend = blend.coerceIn(0f, 1f)
    }

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
        // JUMP CAMERA
        // --------------------------------------------------------

        val sdt = min(dt, 0.033f)

        // Damped springs (semi-implicit Euler, ~2 Hz, a little under-damped = bouncy).
        kickYVel += (-190f * kickY - 17f * kickYVel) * sdt
        kickY += kickYVel * sdt

        kickTiltVel += (-170f * kickTilt - 15f * kickTiltVel) * sdt
        kickTilt += kickTiltVel * sdt

        // Follow the jump upward, quicker on the way up than on the way down.
        val followTarget = -min(airHeight, 120f) * 0.17f
        val followRate = if (followTarget < followDy) 7f else 3.6f
        followDy += (followTarget - followDy) * (1f - exp(-sdt * followRate))

        // Slight push-in while rising, easing out near the apex and on the way down.
        val zoomTarget2 = 1f + 0.03f * min(airHeight / 95f, 1f)
        jumpZoom += (zoomTarget2 - jumpZoom) * (1f - exp(-sdt * 6f))

        // --------------------------------------------------------
        // CONTAINER STUNT CAMERA
        // --------------------------------------------------------

        val st = stuntTarget
        val stuntK = 1f - exp(-dt * (st?.response ?: 4.5f))

        stuntZoom += ((st?.zoom ?: 1f) - stuntZoom) * stuntK
        stuntDy += ((st?.focusDy ?: 0f) - stuntDy) * stuntK
        stuntPanX += ((st?.panX ?: 0f) - stuntPanX) * stuntK
        stuntTilt += ((st?.tilt ?: 0f) - stuntTilt) * stuntK

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

        // ------------------------------------------------------------
// CRASH TILT: sharp reaction, then controlled settle
// ------------------------------------------------------------

        if (crashTiltTime < 0.65f) {
            crashTiltTime += dt

            crashTilt =
                sin(crashTiltTime * 25f) *
                        7f *
                        exp(-crashTiltTime * 5f)
        } else {
            crashTilt = 0f
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

        introBlend = 0f

        crashTiltTime = 1f
        crashTilt = 0f

        stuntZoom = 1f
        stuntDy = 0f
        stuntPanX = 0f
        stuntTilt = 0f

        kickY = 0f
        kickYVel = 0f
        kickTilt = 0f
        kickTiltVel = 0f
        airHeight = 0f
        followDy = 0f
        jumpZoom = 1f
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

        // Takeoff snap: camera springs up and leans in a touch.
        kickYVel -= 130f
        kickTiltVel -= 16f

        addShake(
            strength = 1.2f,
            duration = 0.08f
        )

        kickZoom(
            target = 1.03f,
            duration = 0.12f
        )
    }

    // ------------------------------------------------------------
    // LANDING
    // ------------------------------------------------------------

    fun triggerLanding(impact: Float = 1f) {

        // Landing thump: camera drops hard, then bounces back via the spring.
        kickYVel += 150f * impact.coerceIn(0.4f, 1.5f) * (1f - introBlend)
        kickTiltVel += 12f

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
        // Sharp impact shake.
        addShake(
            strength = 14f,
            duration = 0.42f
        )

        // Start the camera tilt reaction.
        crashTiltTime = 0f
        crashTilt = 0f

        // Quick push-in, followed by the existing automatic zoom return.
        kickZoom(
            target = 1.10f,
            duration = 0.18f
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
        val progress =
            if (shakeDuration <= 0f) 0f else shakeTime / shakeDuration

        val envelope = progress * progress

        val shakeX =
            sin(shakePhase) * shakeStrength * envelope

        val shakeY =
            sin(shakePhase * 1.37f) *
                    shakeStrength * 0.72f * envelope

        // Ease the intro -> gameplay camera hand-off (smoothstep).
        val introBlend = this.introBlend

        // Intro zoom is layered on top of normal cinematic zoom.
        val introZoom =
            1f +
                    (Constants.INTRO_CAMERA_ZOOM - 1f) * introBlend

        val finalZoom =
            zoom * slideZoom * introZoom * stuntZoom * jumpZoom

        val zScale =
            scale * finalZoom

        val screenFocusX =
            focusX * scale

        val screenFocusY =
            offsetY + focusY * scale

        val normalX =
            screenFocusX +
                    (panX + stuntPanX) * scale +
                    slideShakeX +
                    shakeX

        val normalY =
            screenFocusY +
                    slideOffsetY * scale +
                    slideShakeY +
                    shakeY

        // During the intro the player is framed in the centre.
        // Because focusX is world.player.x, the framing follows the player
        // without changing the world itself.
        val introX =
            screenWidth * 0.5f +
                    Constants.INTRO_CAMERA_PAN_X * scale +
                    shakeX

        val introY =
            screenHeight * 0.50f + shakeY

        val finalX =
            normalX + (introX - normalX) * introBlend

        val finalY =
            normalY + (introY - normalY) * introBlend

        canvas.translate(finalX, finalY)
        // World-space range that is actually visible after zoom / pan / tilt.
        val tiltDeg = kotlin.math.abs((slideTilt + crashTilt + stuntTilt + kickTilt) * (1f - introBlend))
        val tiltExtra = kotlin.math.abs(
            sin(Math.toRadians(tiltDeg.toDouble())).toFloat()
        ) * screenHeight / zScale

        visibleLeft  = focusX - finalX / zScale - tiltExtra - 2f
        visibleRight = focusX + (screenWidth - finalX) / zScale + tiltExtra + 2f

        canvas.rotate(
            (slideTilt + crashTilt + stuntTilt + kickTilt) * (1f - introBlend)
        )

        canvas.scale(zScale, zScale)
        // Container stunt: look higher (stuntDy < 0) so the flip stays framed.
        canvas.translate(-focusX, -(focusY + stuntDy + followDy + kickY))
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