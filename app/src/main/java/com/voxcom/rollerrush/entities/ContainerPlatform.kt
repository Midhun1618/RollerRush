package com.voxcom.rollerrush.entities

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Bitmap
import com.voxcom.rollerrush.utils.Constants
import kotlin.random.Random

class ContainerPlatform {

    var active = false
    var x = 0f
    var width = 0f
    var sectionCount = 1

    // Each section gets its own independently selected artwork.
    private val sectionTextureIndices = mutableListOf<Int>()

    val right: Float
        get() = x + width

    val topY: Float
        get() = Constants.GROUND_Y - Constants.CONTAINER_HEIGHT

    val sideHitbox = RectF()

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

        // Randomize the artwork independently for each segment.
        sectionTextureIndices.clear()

        repeat(sectionCount) {
            sectionTextureIndices.add(Random.nextInt(6))
        }

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
    }
}
