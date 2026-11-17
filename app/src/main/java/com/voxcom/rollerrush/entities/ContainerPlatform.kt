
package com.voxcom.rollerrush.entities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.voxcom.rollerrush.utils.Constants

class ContainerPlatform {

    var active = false
    var x = 0f
    var width = 0f
    var sectionCount = 1

    val right: Float
        get() = x + width

    val topY: Float
        get() = Constants.GROUND_Y - Constants.CONTAINER_HEIGHT

    val sideHitbox = RectF()

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(48, 91, 112)
    }

    private val topPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(91, 139, 153)
    }

    private val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(30, 59, 75)
    }

    private val ribPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(111, 151, 161)
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val seamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(25, 54, 70)
        strokeWidth = 4f
    }

    fun init(spawnX: Float, sections: Int) {
        x = spawnX
        sectionCount = sections.coerceIn(1, 3)
        width = sectionCount * Constants.CONTAINER_SEGMENT_WIDTH
        active = true
        refreshHitbox()
    }

    fun update(dt: Float, worldSpeed: Float) {
        if (!active) return

        x -= worldSpeed * dt
        refreshHitbox()
    }

    fun overlapsPlayer(playerX: Float, halfWidth: Float = 18f): Boolean {
        return playerX + halfWidth > x &&
                playerX - halfWidth < right
    }

    fun isOffScreenLeft(visibleLeft: Float): Boolean {
        return right < visibleLeft - 80f
    }

    private fun refreshHitbox() {
        sideHitbox.set(
            x,
            topY + 3f,
            right,
            Constants.GROUND_Y
        )
    }

    fun draw(canvas: Canvas) {
        if (!active) return

        val body = RectF(
            x,
            topY,
            right,
            Constants.GROUND_Y
        )

        // Main container body.
        canvas.drawRoundRect(body, 5f, 5f, bodyPaint)

        // Top surface / landing edge.
        canvas.drawRect(
            x,
            topY,
            right,
            topY + 7f,
            topPaint
        )

        // Bottom shadow.
        canvas.drawRect(
            x,
            Constants.GROUND_Y - 7f,
            right,
            Constants.GROUND_Y,
            darkPaint
        )

        // Vertical corrugated metal ribs.
        var ribX = x + 14f
        while (ribX < right - 8f) {
            canvas.drawLine(
                ribX,
                topY + 12f,
                ribX,
                Constants.GROUND_Y - 12f,
                ribPaint
            )
            ribX += 24f
        }

        // Section separators.
        for (i in 1 until sectionCount) {
            val seamX = x + i * Constants.CONTAINER_SEGMENT_WIDTH
            canvas.drawLine(
                seamX,
                topY + 2f,
                seamX,
                Constants.GROUND_Y - 2f,
                seamPaint
            )
        }

        // Reinforced end edges.
        canvas.drawRect(
            x,
            topY + 7f,
            x + 7f,
            Constants.GROUND_Y - 5f,
            darkPaint
        )

        canvas.drawRect(
            right - 7f,
            topY + 7f,
            right,
            Constants.GROUND_Y - 5f,
            darkPaint
        )

        // Door details on each section.
        for (i in 0 until sectionCount) {
            val sectionX = x + i * Constants.CONTAINER_SEGMENT_WIDTH
            val doorX = sectionX + Constants.CONTAINER_SEGMENT_WIDTH - 52f

            canvas.drawLine(
                doorX,
                topY + 15f,
                doorX,
                Constants.GROUND_Y - 12f,
                seamPaint
            )

            canvas.drawLine(
                doorX + 8f,
                topY + 15f,
                doorX + 8f,
                Constants.GROUND_Y - 12f,
                seamPaint
            )
        }
    }
}
