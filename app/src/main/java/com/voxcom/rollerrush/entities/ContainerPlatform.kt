package com.voxcom.rollerrush.entities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.voxcom.rollerrush.utils.Constants

class ContainerPlatform {

    var x = 0f
        private set

    var width = 0f
        private set

    var height = Constants.CONTAINER_HEIGHT
        private set

    var sectionCount = 1
        private set

    var active = false

    val right: Float
        get() = x + width

    val topY: Float
        get() = Constants.GROUND_Y - height

    val sideHitbox = RectF()

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(55, 91, 120)
    }

    private val topPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(91, 133, 160)
    }

    private val ribPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(37, 67, 91)
        strokeWidth = 2.5f
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(145, 177, 194)
        strokeWidth = 2f
    }

    private val seamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(28, 53, 74)
        strokeWidth = 5f
    }

    private val doorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(42, 72, 96)
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, 0, 0, 0)
    }

    fun init(spawnX: Float, sections: Int) {
        x = spawnX
        sectionCount = sections.coerceIn(1, 3)
        width = Constants.CONTAINER_SEGMENT_WIDTH * sectionCount
        height = Constants.CONTAINER_HEIGHT
        active = true
        refreshHitbox()
    }

    fun update(dt: Float, worldSpeed: Float) {
        if (!active) return

        x -= worldSpeed * dt
        refreshHitbox()
    }

    private fun refreshHitbox() {
        sideHitbox.set(
            x + 1f,
            topY + 3f,
            right - 1f,
            Constants.GROUND_Y
        )
    }

    fun draw(canvas: Canvas) {
        if (!active) return

        // Ground shadow
        canvas.drawRect(
            x + 5f,
            Constants.GROUND_Y - 2f,
            right + 5f,
            Constants.GROUND_Y + 7f,
            shadowPaint
        )

        // Main container body
        canvas.drawRect(
            x,
            topY,
            right,
            Constants.GROUND_Y,
            bodyPaint
        )

        // Top edge
        canvas.drawRect(
            x,
            topY,
            right,
            topY + 7f,
            topPaint
        )

        // Vertical corrugations
        var ribX = x + 10f

        while (ribX < right - 8f) {
            canvas.drawLine(
                ribX,
                topY + 10f,
                ribX,
                Constants.GROUND_Y - 5f,
                ribPaint
            )

            canvas.drawLine(
                ribX + 2f,
                topY + 10f,
                ribX + 2f,
                Constants.GROUND_Y - 5f,
                highlightPaint
            )

            ribX += 13f
        }

        // Seams between connected container sections
        for (i in 1 until sectionCount) {
            val seamX = x + i * Constants.CONTAINER_SEGMENT_WIDTH

            canvas.drawLine(
                seamX,
                topY,
                seamX,
                Constants.GROUND_Y,
                seamPaint
            )
        }

        // Rear door detail
        canvas.drawRect(
            right - 34f,
            topY + 12f,
            right - 7f,
            Constants.GROUND_Y - 8f,
            doorPaint
        )

        canvas.drawLine(
            right - 20f,
            topY + 12f,
            right - 20f,
            Constants.GROUND_Y - 8f,
            highlightPaint
        )

        // Corner details
        canvas.drawRect(
            x,
            topY,
            x + 8f,
            topY + 10f,
            topPaint
        )

        canvas.drawRect(
            right - 8f,
            topY,
            right,
            topY + 10f,
            topPaint
        )
    }
}
