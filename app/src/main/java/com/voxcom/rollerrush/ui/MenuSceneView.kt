
package com.voxcom.rollerrush.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.voxcom.rollerrush.R
import com.voxcom.rollerrush.RollerRushApp
import com.voxcom.rollerrush.data.CharacterData
import com.voxcom.rollerrush.player.Player
import com.voxcom.rollerrush.player.PlayerAnimator
import com.voxcom.rollerrush.player.PlayerSprites
import com.voxcom.rollerrush.utils.Constants
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class MenuSceneView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val app = context.applicationContext as RollerRushApp

    private val scenePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val characterPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val leafStemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
        strokeCap = Paint.Cap.ROUND
    }

    private val dst = RectF()
    private val leafOval = RectF()

    private val farBitmap: Bitmap? =
        BitmapFactory.decodeResource(resources, R.drawable.far_bg)

    private val midBitmap: Bitmap? =
        BitmapFactory.decodeResource(resources, R.drawable.mid_bg)

    private val groundBitmap: Bitmap? =
        BitmapFactory.decodeResource(resources, R.drawable.ground_bg)

    private var sprites: PlayerSprites? = null
    private var player: Player? = null
    private var playerAnimator: PlayerAnimator? = null

    private var lastFrameNs = 0L
    private var sceneTime = 0f

    private var farOffset = 0f
    private var midOffset = 0f
    private var groundOffset = 0f

    private val density = resources.displayMetrics.density

    private data class Leaf(
        var x: Float,
        var y: Float,
        var speed: Float,
        var size: Float,
        var rotation: Float,
        var spin: Float,
        var phase: Float,
        var bobSpeed: Float,
        var color: Int
    )

    private val leaves = mutableListOf<Leaf>()

    private val leafColors = intArrayOf(
        Color.rgb(173, 92, 35),
        Color.rgb(204, 126, 42),
        Color.rgb(137, 106, 47),
        Color.rgb(190, 69, 35),
        Color.rgb(218, 157, 62)
    )

    init {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        isClickable = false

        leafPaint.style = Paint.Style.FILL
        leafStemPaint.color = Color.argb(175, 75, 53, 28)
    }

    fun setCharacter(data: CharacterData) {
        sprites = app.gameAssets.createPlayerSprites(data)

        val newPlayer = Player(sprites!!).apply {
            x = 0f
            bottomY = 0f
            grounded = true
            crashed = false
            stopSlide()
            setIntroRotation(0f)
        }

        player = newPlayer
        playerAnimator = PlayerAnimator(newPlayer).also {
            it.reset()
        }

        lastFrameNs = 0L
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width <= 0 || height <= 0) return

        val now = System.nanoTime()
        val dt = if (lastFrameNs == 0L) {
            0f
        } else {
            ((now - lastFrameNs) / 1_000_000_000f).coerceIn(0f, 0.033f)
        }

        lastFrameNs = now
        sceneTime += dt

        updateBackgroundOffsets(dt)

        // Background layers move at different speeds to create depth.
        drawScrollingLayer(
            canvas = canvas,
            bitmap = farBitmap,
            topFraction = 0.02f,
            heightFraction = 0.58f,
            offset = farOffset,
            clipTop = 0f,
            clipBottom = height * 0.78f
        )

        drawScrollingLayer(
            canvas = canvas,
            bitmap = midBitmap,
            topFraction = 0.17f,
            heightFraction = 0.72f,
            offset = midOffset,
            clipTop = height * 0.12f,
            clipBottom = height.toFloat()
        )

        drawScrollingGround(canvas)

        updateSkatingAnimation(dt)
        drawCharacter(canvas)

        // Leaves are drawn after the skater, so some pass in front of the body.
        updateAndDrawLeaves(canvas, dt)

        postInvalidateOnAnimation()
    }

    private fun updateBackgroundOffsets(dt: Float) {
        // Pixels per second. Increase these values for a more intense speed effect.
        farOffset += 32f * density * dt
        midOffset += 88f * density * dt
        groundOffset += 190f * density * dt
    }

    /**
     * Repeats a horizontally scrolling image. Alternating mirrored tiles help
     * hide hard edges when a background was not designed as a seamless texture.
     */

    private fun drawScrollingLayer(
        canvas: Canvas,
        bitmap: Bitmap?,
        topFraction: Float,
        heightFraction: Float,
        offset: Float,
        clipTop: Float,
        clipBottom: Float
    ) {
        bitmap ?: return
        if (width <= 0 || height <= 0) return

        val layerHeight = height * heightFraction

        // Use one consistent tile size for every copy of this layer.
        val tileWidth = maxOf(
            width.toFloat(),
            layerHeight * bitmap.width.toFloat() /
                    bitmap.height.coerceAtLeast(1)
        )

        val tileHeight = tileWidth *
                bitmap.height.toFloat() / bitmap.width.coerceAtLeast(1)

        val top = height * topFraction + layerHeight - tileHeight

        // Positive offset scrolls continuously from right to left.
        val shift = offset % tileWidth

        canvas.save()
        canvas.clipRect(
            0f,
            clipTop,
            width.toFloat(),
            clipBottom
        )

        // Draw identical, unmirrored copies. The extra copies ensure
        // the viewport stays covered throughout the wraparound.
        var left = -shift - tileWidth

        while (left < width) {
            dst.set(
                left,
                top,
                left + tileWidth,
                top + tileHeight
            )

            canvas.drawBitmap(bitmap, null, dst, scenePaint)
            left += tileWidth
        }

        canvas.restore()
    }


    private fun drawScrollingGround(canvas: Canvas) {
        val bitmap = groundBitmap ?: return

        val tileHeight = height * 0.25f
        val tileWidth = maxOf(
            width * 0.78f,
            tileHeight * bitmap.width.toFloat() /
                    bitmap.height.coerceAtLeast(1)
        )
        val actualHeight = tileWidth *
                bitmap.height.toFloat() / bitmap.width.coerceAtLeast(1)

        val top = height - actualHeight
        val shift = groundOffset % tileWidth

        canvas.save()
        canvas.clipRect(
            0f,
            height * 0.72f,
            width.toFloat(),
            height.toFloat()
        )

        var tileIndex = -1
        var left = -shift - tileWidth

        while (left < width) {
            drawTile(
                canvas = canvas,
                bitmap = bitmap,
                left = left,
                top = top,
                tileWidth = tileWidth,
                tileHeight = actualHeight,
                mirrored = abs(tileIndex % 2) == 1
            )

            left += tileWidth
            tileIndex++
        }

        canvas.restore()
    }

    private fun drawTile(
        canvas: Canvas,
        bitmap: Bitmap,
        left: Float,
        top: Float,
        tileWidth: Float,
        tileHeight: Float,
        mirrored: Boolean
    ) {
        dst.set(
            left,
            top,
            left + tileWidth,
            top + tileHeight
        )

        if (mirrored) {
            canvas.save()
            canvas.scale(
                -1f,
                1f,
                left + tileWidth * 0.5f,
                top + tileHeight * 0.5f
            )
            canvas.drawBitmap(bitmap, null, dst, scenePaint)
            canvas.restore()
        } else {
            canvas.drawBitmap(bitmap, null, dst, scenePaint)
        }
    }

    /**
     * Let the real gameplay animator control all body joints and hip placement.
     * bottomY = 0 becomes the ground contact point after the canvas transform.
     */
    private fun updateSkatingAnimation(dt: Float) {
        val p = player ?: return
        val animator = playerAnimator ?: return

        p.x = 0f
        p.bottomY = 0f
        p.grounded = true
        p.crashed = false
        p.stopSlide()
        p.setIntroRotation(0f)

        if (dt > 0f) {
            animator.update(
                dt = dt,
                worldSpeed = Constants.BASE_SPEED,
                speedBoostActive = false
            )
        }
    }

    private fun drawCharacter(canvas: Canvas) {
        val p = player ?: return
        if (width <= 0 || height <= 0) return

        // The rig's world-unit proportions remain unchanged.
        val characterScale = min(
            height * 0.72f / 112f,
            width * 0.43f / 65f
        )

        val characterCenterX = width * 0.255f
        val skateGroundY = height * 0.86f

        canvas.save()
        canvas.translate(characterCenterX, skateGroundY)
        canvas.scale(characterScale, characterScale)

        p.x = 0f
        p.bottomY = 0f

        // Do not overwrite torso.y here. PlayerAnimator computes it from
        // the actual skating pose and leg/foot-drop calculations.
        p.draw(canvas, characterPaint)

        canvas.restore()
    }

    private fun updateAndDrawLeaves(canvas: Canvas, dt: Float) {
        if (width <= 0 || height <= 0) return

        ensureLeaves()

        for (leaf in leaves) {
            leaf.x -= leaf.speed * density * dt
            leaf.y += sin(
                sceneTime * leaf.bobSpeed + leaf.phase
            ) * 24f * density * dt

            leaf.rotation += leaf.spin * dt

            // Recycle leaves after they leave the right edge.
            if (leaf.x < -45f * density) {
                resetLeaf(leaf, spawnAtLeft = false)
            }

            drawLeaf(canvas, leaf)
        }
    }

    private fun ensureLeaves() {
        if (leaves.isNotEmpty()) return

        // Spread the initial particles across the screen so the scene looks
        // alive immediately rather than waiting for every leaf to enter.
        val count = 5

        for (i in 0 until count) {
            val fraction = i.toFloat() / count

            leaves += Leaf(
                x = fraction * width,
                y = height * (0.20f + ((i * 37) % 58) / 100f),
                speed = (450f + ((i * 37) % 300)),
                size = (5f + ((i * 7) % 8)) * density,
                rotation = (i * 41f) % 360f,
                spin = if (i % 2 == 0) -300f else 360f,
                phase = i * 1.73f,
                bobSpeed = 1.8f + (i % 4) * 0.45f,
                color = leafColors[i % leafColors.size]
            )
        }
    }

    private fun resetLeaf(leaf: Leaf, spawnAtLeft: Boolean) {
        val index = leaves.indexOf(leaf).coerceAtLeast(0)

        leaf.x = if (spawnAtLeft) {
            -20f * density
        } else {
            width.toFloat()
        }

        leaf.y = height * (
                0.18f + ((index * 31 + sceneTime.toInt() * 7) % 62) / 100f
                )

        leaf.speed = 115f + ((index * 29) % 95)
        leaf.size = (5f + ((index * 7) % 8)) * density
        leaf.rotation = (index * 41f + sceneTime * 30f) % 360f
        leaf.spin = if (index % 2 == 0) 170f else -210f
        leaf.phase = index * 1.73f
        leaf.bobSpeed = 1.8f + (index % 4) * 0.45f
        leaf.color = leafColors[index % leafColors.size]
    }

    private fun drawLeaf(canvas: Canvas, leaf: Leaf) {
        canvas.save()
        canvas.translate(leaf.x, leaf.y)
        canvas.rotate(leaf.rotation)

        val w = leaf.size * 1.55f
        val h = leaf.size * 0.72f

        leafPaint.color = leaf.color
        leafPaint.alpha = 225

        leafOval.set(-w * 0.5f, -h * 0.5f, w * 0.5f, h * 0.5f)
        canvas.drawOval(leafOval, leafPaint)

        // Small center vein.
        leafStemPaint.color = Color.argb(180, 75, 53, 28)
        canvas.drawLine(
            -w * 0.38f,
            0f,
            w * 0.38f,
            0f,
            leafStemPaint
        )

        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        lastFrameNs = 0L
        super.onDetachedFromWindow()
    }
}
