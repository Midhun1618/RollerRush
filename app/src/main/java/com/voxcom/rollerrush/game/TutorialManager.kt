package com.voxcom.rollerrush.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.voxcom.rollerrush.R
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

class TutorialManager(context: Context) {

    enum class Action {
        JUMP,
        SLIDE
    }

    private enum class Step {
        JUMP_DEMO,
        WAIT_JUMP,
        SHOW_JUMP,
        SLIDE_DEMO,
        WAIT_SLIDE,
        SHOW_SLIDE,
        COMPLETE
    }

    private var step = Step.COMPLETE
    private var elapsed = 0f
    private var completionEvent = false

    val active: Boolean
        get() = step != Step.COMPLETE

    /**
     * Keep the game world frozen while demonstrating or waiting
     * for the player's gesture.
     */
    val shouldPauseWorld: Boolean
        get() = when (step) {
            Step.JUMP_DEMO,
            Step.WAIT_JUMP,
            Step.SLIDE_DEMO,
            Step.WAIT_SLIDE -> true

            Step.SHOW_JUMP,
            Step.SHOW_SLIDE,
            Step.COMPLETE -> false
        }

    // Load each image only once.
    private val jumpFinger = loadBitmap(context, R.drawable.tutorial_swipe_up)
    private val slideFinger = loadBitmap(context, R.drawable.tutorial_swipe_down)

    private val jumpText = loadBitmap(context, R.drawable.swipe_up_text)
    private val slideText = loadBitmap(context, R.drawable.swipe_down_text)
    private val successText = loadBitmap(context, R.drawable.nice_move_text)

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(105, 0, 0, 0)
    }

    private val imagePaint = Paint(
        Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG
    ).apply {
        isFilterBitmap = true
        isDither = true
    }

    private val textRect = RectF()
    private val fingerRect = RectF()

    private fun loadBitmap(context: Context, resourceId: Int): Bitmap {
        return BitmapFactory.decodeResource(
            context.resources,
            resourceId
        ) ?: throw IllegalStateException(
            "Unable to load tutorial image resource: $resourceId"
        )
    }

    fun start() {
        step = Step.JUMP_DEMO
        elapsed = 0f
        completionEvent = false
    }

    fun update(dt: Float) {
        if (!active) return

        elapsed += dt.coerceAtLeast(0f)

        when (step) {
            Step.JUMP_DEMO -> {
                if (elapsed >= 2f) {
                    step = Step.WAIT_JUMP
                    elapsed = 0f
                }
            }

            Step.SHOW_JUMP -> {
                if (elapsed >= 0.9f) {
                    step = Step.SLIDE_DEMO
                    elapsed = 0f
                }
            }

            Step.SLIDE_DEMO -> {
                if (elapsed >= 2f) {
                    step = Step.WAIT_SLIDE
                    elapsed = 0f
                }
            }

            Step.SHOW_SLIDE -> {
                if (elapsed >= 0.9f) {
                    step = Step.COMPLETE
                    elapsed = 0f
                    completionEvent = true
                }
            }

            else -> Unit
        }
    }

    /**
     * Negative deltaY = swipe up.
     * Positive deltaY = swipe down.
     */
    fun handleSwipe(deltaY: Float): Action? {
        return when (step) {
            Step.WAIT_JUMP -> {
                if (deltaY < 0f) {
                    step = Step.SHOW_JUMP
                    elapsed = 0f
                    Action.JUMP
                } else {
                    null
                }
            }

            Step.WAIT_SLIDE -> {
                if (deltaY > 0f) {
                    step = Step.SHOW_SLIDE
                    elapsed = 0f
                    Action.SLIDE
                } else {
                    null
                }
            }

            else -> null
        }
    }

    fun consumeCompletionEvent(): Boolean {
        val result = completionEvent
        completionEvent = false
        return result
    }

    fun draw(canvas: Canvas, width: Int, height: Int) {
        if (!active || width <= 0 || height <= 0) return

        val w = width.toFloat()
        val h = height.toFloat()

        // Keep the gameplay visible behind the tutorial.
        canvas.drawRect(0f, 0f, w, h, dimPaint)

        // All tutorial artwork is positioned in the right half.
        val centerX = w * 0.75f

        when (step) {
            Step.JUMP_DEMO,
            Step.WAIT_JUMP -> {
                drawInstruction(
                    canvas = canvas,
                    width = w,
                    height = h,
                    centerX = centerX,
                    textBitmap = jumpText,
                    fingerBitmap = jumpFinger,
                    movingUp = true
                )
            }

            Step.SLIDE_DEMO,
            Step.WAIT_SLIDE -> {
                drawInstruction(
                    canvas = canvas,
                    width = w,
                    height = h,
                    centerX = centerX,
                    textBitmap = slideText,
                    fingerBitmap = slideFinger,
                    movingUp = false
                )
            }

            Step.SHOW_JUMP,
            Step.SHOW_SLIDE -> {
                drawSuccess(
                    canvas = canvas,
                    width = w,
                    height = h,
                    centerX = centerX
                )
            }

            Step.COMPLETE -> Unit
        }
    }

    private fun drawInstruction(
        canvas: Canvas,
        width: Float,
        height: Float,
        centerX: Float,
        textBitmap: Bitmap,
        fingerBitmap: Bitmap,
        movingUp: Boolean
    ) {
        // Instruction image near the top of the right half.
        val textWidth = width * 0.46f
        val textHeight = textWidth *
                textBitmap.height.toFloat() / textBitmap.width.toFloat()

        val textCenterY = height * 0.22f

        textRect.set(
            centerX - textWidth / 2f,
            textCenterY - textHeight / 2f,
            centerX + textWidth / 2f,
            textCenterY + textHeight / 2f
        )

        canvas.drawBitmap(
            textBitmap,
            null,
            textRect,
            imagePaint
        )

        // Keep the finger artwork proportional and within the screen.
        val maxFingerWidth = width * 0.31f
        val maxFingerHeight = height * 0.48f

        val fingerScale = min(
            maxFingerWidth / fingerBitmap.width.toFloat(),
            maxFingerHeight / fingerBitmap.height.toFloat()
        )

        val fingerWidth = fingerBitmap.width * fingerScale
        val fingerHeight = fingerBitmap.height * fingerScale

        // Repeating smooth movement.
        // The image moves upward for jump and downward for slide.
        val wave = sin(elapsed * PI * 1.5).toFloat()

        val travel = height * 0.075f

        val movement = if (movingUp) {
            -wave * travel
        } else {
            wave * travel
        }

        val fingerCenterY = height * 0.66f + movement

        fingerRect.set(
            centerX - fingerWidth / 2f,
            fingerCenterY - fingerHeight / 2f,
            centerX + fingerWidth / 2f,
            fingerCenterY + fingerHeight / 2f
        )

        canvas.drawBitmap(
            fingerBitmap,
            null,
            fingerRect,
            imagePaint
        )
    }

    private fun drawSuccess(
        canvas: Canvas,
        width: Float,
        height: Float,
        centerX: Float
    ) {
        val imageWidth = width * 0.46f
        val imageHeight = imageWidth *
                successText.height.toFloat() / successText.width.toFloat()

        val centerY = height * 0.50f

        textRect.set(
            centerX - imageWidth / 2f,
            centerY - imageHeight / 2f,
            centerX + imageWidth / 2f,
            centerY + imageHeight / 2f
        )

        canvas.drawBitmap(
            successText,
            null,
            textRect,
            imagePaint
        )
    }

    /**
     * Call when GameView is released.
     */
    fun release() {
        jumpFinger.recycle()
        slideFinger.recycle()
        jumpText.recycle()
        slideText.recycle()
        successText.recycle()
    }
}