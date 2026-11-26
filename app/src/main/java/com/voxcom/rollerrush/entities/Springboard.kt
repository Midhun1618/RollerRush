
package com.voxcom.rollerrush.entities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.voxcom.rollerrush.utils.Constants

/**
 * A springboard placed in front of a container.
 *
 * It is a separate gameplay object. It only launches the player when the
 * player is grounded and physically overlaps the activation area.
 */
class Springboard {

    var active = false
        private set

    var x = 0f
        private set

    var launchAuthorized = false
        private set

    private var compression = 0f
    private var animationTimer = 0f
    private var launched = false

    val width = 54f
    val height = 22f

    val right: Float
        get() = x + width

    val activationRect = RectF()

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(50, 55, 70)
    }

    private val springPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 180, 40)
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val topPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 80, 80)
    }

    fun init(spawnX: Float) {
        x = spawnX
        active = true
        launchAuthorized = false
        compression = 0f
        animationTimer = 0f
        launched = false
        refreshHitbox()
    }

    fun update(dt: Float, worldSpeed: Float) {
        if (!active) return

        x -= worldSpeed * dt

        if (compression > 0f) {
            animationTimer += dt
            compression = (1f - animationTimer / 0.22f).coerceIn(0f, 1f)
        }

        refreshHitbox()
    }

    /**
     * Activates only when the player is grounded and touches the board.
     * Returns true exactly once for a successful launch.
     */
    fun tryLaunch(
        playerX: Float,
        playerBottomY: Float,
        grounded: Boolean
    ): Boolean {
        if (!active || launched || !grounded) return false

        val overlapsX =
            playerX + 18f > x &&
                    playerX - 18f < right

        val touchesGround =
            playerBottomY >= Constants.GROUND_Y - 3f

        if (!overlapsX || !touchesGround) return false

        launched = true
        launchAuthorized = true
        compression = 1f
        animationTimer = 0f

        return true
    }

    fun consumeLaunchAuthorization(): Boolean {
        if (!launchAuthorized) return false

        launchAuthorized = false
        return true
    }

    fun deactivate() {
        active = false
        launchAuthorized = false
    }

    fun isOffScreenLeft(visibleLeft: Float): Boolean {
        return right < visibleLeft - 80f
    }

    private fun refreshHitbox() {
        activationRect.set(
            x,
            Constants.GROUND_Y - height,
            right,
            Constants.GROUND_Y
        )
    }

    fun draw(canvas: Canvas) {
        if (!active) return

        val compressedHeight = height * compression
        val top = Constants.GROUND_Y - height + compressedHeight

        // Base
        canvas.drawRoundRect(
            x,
            Constants.GROUND_Y - 8f,
            right,
            Constants.GROUND_Y,
            5f,
            5f,
            basePaint
        )

        // Coil
        val coilTop = top + 3f
        val coilBottom = Constants.GROUND_Y - 8f

        if (coilBottom > coilTop) {
            val segments = 5
            val coilWidth = width - 12f
            val step = coilWidth / segments

            var px = x + 6f
            var py = coilTop

            for (i in 0..segments) {
                val nextX = x + 6f + step * i
                val nextY = if (i % 2 == 0) coilTop else coilBottom

                canvas.drawLine(
                    px,
                    py,
                    nextX,
                    nextY,
                    springPaint
                )

                px = nextX
                py = nextY
            }
        }

        // Top plate
        canvas.drawRoundRect(
            x - 3f,
            top,
            right + 3f,
            top + 6f,
            4f,
            4f,
            topPaint
        )
    }
}
