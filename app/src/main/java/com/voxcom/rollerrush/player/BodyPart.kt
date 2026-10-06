package com.voxcom.rollerrush.player

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

class BodyPart(
    val name: String,
    var bitmap: Bitmap?,
    val width: Float,      // size in WORLD UNITS
    val height: Float,
    val pivotX: Float,     // local pivot inside the bitmap, in world units
    val pivotY: Float
) {
    var x = 0f
    var y = 0f
    var rotation = 0f
    var scaleX = 1f
    var scaleY = 1f
    var parent: BodyPart? = null
        private set

    /** If true this part is drawn BEFORE its parent's own bitmap (used for far-side limbs). */
    var behindParent = false
        private set

    private val children = ArrayList<BodyPart>(3)
    private val dst = RectF() // reused every frame, never reallocated

    fun addChild(child: BodyPart, attachX: Float, attachY: Float, behind: Boolean = false) {
        child.parent = this
        child.x = attachX
        child.y = attachY
        child.behindParent = behind
        children.add(child)
    }

    fun draw(canvas: Canvas, paint: Paint) {
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(rotation)
        canvas.scale(scaleX, scaleY)

        for (i in 0 until children.size) {
            val c = children[i]
            if (c.behindParent) c.draw(canvas, paint)
        }

        val bmp = bitmap
        if (bmp != null) {
            // Destination rect in world units, offset so the pivot lands on the origin.
            dst.set(-pivotX, -pivotY, width - pivotX, height - pivotY)
            canvas.drawBitmap(bmp, null, dst, paint)
        }

        for (i in 0 until children.size) {
            val c = children[i]
            if (!c.behindParent) c.draw(canvas, paint)
        }
        canvas.restore()
    }
}
