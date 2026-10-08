package com.voxcom.rollerrush.entities

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.voxcom.rollerrush.utils.AssetManager
import kotlin.math.cos
import kotlin.math.sin

enum class PowerUpType {
    MAGNET,
    SHIELD,
    SPEED
}

class PowerUp : GameEntity() {

    var type = PowerUpType.SPEED
        private set

    private var t = 0f

    // ------------------------------------------------------------
    // PICKUP EFFECT
    // ------------------------------------------------------------

    private var collected = false
    private var effectTime = 0f

    private var effectX = 0f
    private var effectY = 0f

    companion object {

        // Three-frame booster animation.
        private const val FRAME_DURATION = 0.085f

        // How long the pickup particle effect lasts.
        private const val PICKUP_EFFECT_DURATION = 0.55f

        // Visual size of the square booster.
        // Change these if you want the booster larger/smaller.
        private const val VISUAL_WIDTH = 35f
        private const val VISUAL_HEIGHT = 35f

        // Number of custom PNG particles.
        private const val PARTICLE_COUNT = 18
    }

    // ------------------------------------------------------------
    // PARTICLE DATA
    // ------------------------------------------------------------

    private data class Particle(
        var angle: Float = 0f,
        var speed: Float = 0f,
        var distance: Float = 0f,
        var rotation: Float = 0f,
        var rotationSpeed: Float = 0f,
        var scale: Float = 1f,
        var alpha: Float = 1f
    )

    private val particles =
        Array(PARTICLE_COUNT) {
            Particle()
        }

    // ------------------------------------------------------------
    // INIT
    // ------------------------------------------------------------

    fun init(
        type: PowerUpType,
        cx: Float,
        cy: Float
    ) {
        this.type = type

        // Collision box.
        width = 20f
        height = 20f

        x = cx - width / 2f
        y = cy - height / 2f

        t = 0f

        collected = false
        effectTime = 0f

        effectX = cx
        effectY = cy

        active = true

        updateBounds()
    }

    // ------------------------------------------------------------
    // COLLECT
    // ------------------------------------------------------------

    fun collect() {
        if (!active || collected) return

        collected = true
        effectTime = 0f

        effectX = x + width / 2f
        effectY = y + height / 2f

        setupParticles()
    }

    // ------------------------------------------------------------
    // PARTICLE SETUP
    // ------------------------------------------------------------

    private fun setupParticles() {

        for (i in particles.indices) {

            val p = particles[i]

            // Spread evenly around the pickup,
            // with a small offset so it doesn't look too perfect.
            p.angle =
                (i.toFloat() / PARTICLE_COUNT) *
                        Math.PI.toFloat() *
                        2f +
                        (i % 3 - 1) * 0.12f

            // Different particles travel at different speeds.
            p.speed =
                38f +
                        (i % 5) * 10f

            p.distance = 0f

            // Random-looking rotation without using Random every frame.
            p.rotation =
                (i * 37 % 360).toFloat()

            p.rotationSpeed =
                if (i % 2 == 0) {
                    180f + i * 7f
                } else {
                    -180f - i * 6f
                }

            // Slightly different particle sizes.
            p.scale =
                when (i % 4) {
                    0 -> 0.75f
                    1 -> 1.00f
                    2 -> 1.20f
                    else -> 0.90f
                }

            p.alpha = 1f
        }
    }

    // ------------------------------------------------------------
    // UPDATE
    // ------------------------------------------------------------

    override fun update(
        dt: Float,
        worldSpeed: Float
    ) {

        if (collected) {

            effectTime += dt

            updateParticles(dt)

            if (effectTime >= PICKUP_EFFECT_DURATION) {
                active = false
            }

            return
        }

        super.update(dt, worldSpeed)

        t += dt
    }

    // ------------------------------------------------------------
    // PARTICLE UPDATE
    // ------------------------------------------------------------

    private fun updateParticles(dt: Float) {

        val progress =
            (effectTime / PICKUP_EFFECT_DURATION)
                .coerceIn(0f, 1f)

        /*
         * Fast initial expansion followed by slowing down.
         */
        val expansion =
            1f -
                    (1f - progress) *
                    (1f - progress)

        for (p in particles) {

            p.distance =
                p.speed *
                        expansion

            p.rotation +=
                p.rotationSpeed *
                        dt

            /*
             * Fade mainly during the second half.
             */
            p.alpha =
                if (progress < 0.45f) {
                    1f
                } else {
                    1f -
                            (
                                    (progress - 0.45f) /
                                            0.55f
                                    )
                }
        }
    }

    // ------------------------------------------------------------
    // DRAW
    // ------------------------------------------------------------

    override fun draw(
        canvas: Canvas,
        assets: AssetManager,
        paint: Paint
    ) {

        if (collected) {
            drawPickupParticles(
                canvas,
                assets,
                paint
            )
            return
        }

        drawBooster(
            canvas,
            assets,
            paint
        )
    }

    // ------------------------------------------------------------
    // BOOSTER
    // ------------------------------------------------------------

    private fun drawBooster(
        canvas: Canvas,
        assets: AssetManager,
        paint: Paint
    ) {

        if (type != PowerUpType.SPEED) {
            return
        }

        /*
         * 3-frame animation:
         *
         * volt_powerup1
         * volt_powerup2
         * volt_powerup3
         */
        val frame =
            (
                    t / FRAME_DURATION
                    ).toInt() % 3

        val bitmap =
            assets.speedBoostFrames[frame]

        val cx =
            x + width / 2f

        /*
         * Floating movement.
         */
        val cy =
            y +
                    height / 2f +
                    sin(t * 6f) * 2f

        val left =
            cx -
                    VISUAL_WIDTH / 2f

        val top =
            cy -
                    VISUAL_HEIGHT / 2f

        val dst =
            RectF(
                left,
                top,
                left + VISUAL_WIDTH,
                top + VISUAL_HEIGHT
            )

        paint.alpha = 255

        canvas.drawBitmap(
            bitmap,
            null,
            dst,
            paint
        )
    }

    // ------------------------------------------------------------
    // CUSTOM PNG PARTICLES
    // ------------------------------------------------------------

    private fun drawPickupParticles(
        canvas: Canvas,
        assets: AssetManager,
        paint: Paint
    ) {

        val particleBitmap =
            assets.speedBoostParticle

        for (p in particles) {

            val angle = p.angle

            val px =
                effectX +
                        cos(angle) *
                        p.distance

            val py =
                effectY +
                        sin(angle) *
                        p.distance

            /*
             * Particle gets slightly smaller as it travels.
             */
            val progress =
                (
                        effectTime /
                                PICKUP_EFFECT_DURATION
                        )
                    .coerceIn(0f, 1f)

            val size =
                7f *
                        p.scale *
                        (1f - progress * 0.35f)

            val half =
                size / 2f

            val rect =
                RectF(
                    px - half,
                    py - half,
                    px + half,
                    py + half
                )

            paint.alpha =
                (255f * p.alpha)
                    .toInt()
                    .coerceIn(0, 255)

            canvas.save()

            canvas.rotate(
                p.rotation,
                px,
                py
            )

            canvas.drawBitmap(
                particleBitmap,
                null,
                rect,
                paint
            )

            canvas.restore()
        }

        // Restore paint state.
        paint.alpha = 255
    }
}