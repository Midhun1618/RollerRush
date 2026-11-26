package com.voxcom.rollerrush.entities

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Bitmap
import android.graphics.Color
import com.voxcom.rollerrush.utils.Constants
import kotlin.random.Random as KotlinRandom

class ContainerPlatform {

    var active = false
    var x = 0f
    var width = 0f
    var sectionCount = 1

    // Container artwork
    private val sectionTextureIndices = mutableListOf<Int>()

    val right: Float
        get() = x + width

    val topY: Float
        get() = Constants.GROUND_Y - Constants.CONTAINER_HEIGHT

    val sideHitbox = RectF()

    // =========================================================
    // SPRINGBOARD
    // =========================================================

    private val springboardWidth = 64f
    private val springboardHeight = 14f
    private val springboardGap = 12f

    // Positioned in front of the container (left side).
    val springboardX: Float
        get() = x - springboardGap - springboardWidth

    val springboardHitbox = RectF()

    var springboardCompression = 0f
        private set

    private var compressionTimer = 0f

    val springboardActivated: Boolean
        get() = compressionTimer > 0f

    private val springboardBasePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val springboardTopPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val springPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val springboardRect = RectF()

    var springboardLaunchConsumed = false
        private set

    // =========================================================
    // CONTAINER DRAWING
    // =========================================================

    private val bitmapPaint = Paint(
        Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG
    )

    fun textureForSection(index: Int): Int {
        return sectionTextureIndices[index]
    }

    fun init(spawnX: Float, sections: Int) {

        x = spawnX
        sectionCount = sections.coerceIn(1, 3)
        width = sectionCount * Constants.CONTAINER_SEGMENT_WIDTH

        sectionTextureIndices.clear()

        repeat(sectionCount) {
            sectionTextureIndices.add(KotlinRandom.nextInt(0, 6))
        }

        springboardCompression = 0f
        compressionTimer = 0f
        springboardLaunchConsumed = false

        active = true

        refreshHitboxes()
    }

    fun update(dt: Float, worldSpeed: Float) {

        if (!active) return

        x -= worldSpeed * dt

        // Springboard compression animation recovery.
        if (compressionTimer > 0f) {

            compressionTimer -= dt

            if (compressionTimer <= 0f) {
                compressionTimer = 0f
                springboardCompression = 0f
            } else {
                springboardCompression =
                    (compressionTimer / 0.18f).coerceIn(0f, 1f)
            }
        }

        refreshHitboxes()
    }

    // Trigger the compression visual.
    // Actual player launch physics will be added in the next step.
    fun compressSpringboard(): Boolean {

        if (springboardLaunchConsumed || !active) {
            return false
        }

        springboardLaunchConsumed = true
        compressionTimer = 0.18f
        springboardCompression = 1f

        return true
    }

    fun overlapsPlayer(playerX: Float, halfWidth: Float = 18f): Boolean {
        return playerX + halfWidth > x &&
                playerX - halfWidth < right
    }

    fun isOffScreenLeft(visibleLeft: Float): Boolean {
        return right < visibleLeft - 80f
    }

    private fun refreshHitboxes() {

        sideHitbox.set(
            x,
            topY + 3f,
            right,
            Constants.GROUND_Y
        )

        springboardHitbox.set(
            springboardX,
            Constants.GROUND_Y - springboardHeight,
            springboardX + springboardWidth,
            Constants.GROUND_Y
        )
    }

    // =========================================================
    // DRAW CONTAINER
    // =========================================================

    fun draw(canvas: Canvas, bitmaps: Array<Bitmap>) {

        if (!active) return

        val segmentWidth = Constants.CONTAINER_SEGMENT_WIDTH

        for (section in 0 until sectionCount) {

            val sectionX = x + section * segmentWidth

            val destination = RectF(
                sectionX,
                topY,
                sectionX + segmentWidth,
                Constants.GROUND_Y
            )

            val bitmap = bitmaps[textureForSection(section)]

            if (!bitmap.isRecycled) {
                canvas.drawBitmap(
                    bitmap,
                    null,
                    destination,
                    bitmapPaint
                )
            }
        }

        drawSpringboard(canvas)
    }

    // =========================================================
    // PROCEDURAL SPRINGBOARD
    // =========================================================

    private fun drawSpringboard(canvas: Canvas) {

        val compression = springboardCompression

        val baseY = Constants.GROUND_Y
        val compressedHeight = springboardHeight * compression

        // Board base
        springboardBasePaint.color = Color.rgb(55, 55, 65)

        canvas.drawRoundRect(
            springboardX,
            baseY - 5f,
            springboardX + springboardWidth,
            baseY,
            4f,
            4f,
            springboardBasePaint
        )

        // Spring coils
        springPaint.color = Color.rgb(190, 195, 205)

        val coilBottom = baseY - 5f
        val coilTop = baseY - springboardHeight + compressedHeight

        val coilHeight = (coilBottom - coilTop).coerceAtLeast(2f)

        val coilCount = 4
        val coilStep = springboardWidth / (coilCount + 1)

        for (i in 1..coilCount) {

            val coilX = springboardX + coilStep * i

            canvas.drawLine(
                coilX,
                coilTop,
                coilX,
                coilBottom,
                springPaint
            )
        }

        // Top platform
        springboardTopPaint.color = Color.rgb(255, 185, 40)

        springboardRect.set(
            springboardX,
            baseY - springboardHeight + compressedHeight,
            springboardX + springboardWidth,
            baseY - 5f + compressedHeight
        )

        canvas.drawRoundRect(
            springboardRect,
            5f,
            5f,
            springboardTopPaint
        )

        // Highlight stripe
        springboardTopPaint.color = Color.rgb(255, 230, 120)

        canvas.drawRoundRect(
            springboardX + 5f,
            springboardRect.top + 2f,
            springboardX + springboardWidth - 5f,
            springboardRect.top + 5f,
            2f,
            2f,
            springboardTopPaint
        )
    }
}
