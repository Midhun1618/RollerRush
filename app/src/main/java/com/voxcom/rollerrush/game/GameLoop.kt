package com.voxcom.rollerrush.game

import android.graphics.Canvas
import android.os.Build
import android.view.SurfaceHolder
import com.voxcom.rollerrush.utils.Constants
import kotlin.math.min

/**
 * Dedicated game thread.
 *
 * GAME LOOP (conceptually):
 *   while running:
 *       dt = time since last frame (clamped)
 *       update(dt)   -> physics, animation, spawning, collisions, score (in small fixed sub-steps)
 *       render()     -> draw one frame
 *       sleep to hold ~60 FPS
 *
 * Sub-stepping keeps physics/collision stable even if a frame takes long (no tunnelling,
 * no frame-rate dependence).
 */
class GameLoop(
    private val holder: SurfaceHolder,
    private val callbacks: Callbacks
) : Thread("RollerRush-GameLoop") {

    interface Callbacks {
        fun onUpdate(dt: Float)
        fun onRender(canvas: Canvas)
        /** True while nothing is simulated (paused); the loop then redraws at a lower rate. */
        fun isIdle(): Boolean
    }

    @Volatile private var running = false

    fun startLoop() { running = true; start() }

    fun stopLoop() {
        running = false
        var retry = true
        while (retry) {
            try { join(); retry = false } catch (e: InterruptedException) { /* keep waiting */ }
        }
    }

    override fun run() {
        var last = System.nanoTime()
        while (running) {
            val frameStart = System.nanoTime()
            var dt = (frameStart - last) / 1_000_000_000f
            last = frameStart
            if (dt > Constants.MAX_FRAME_DT) dt = Constants.MAX_FRAME_DT

            var remaining = dt
            while (remaining > 1e-5f) {
                val step = min(remaining, Constants.MAX_FIXED_STEP)
                callbacks.onUpdate(step)
                remaining -= step
            }

            render()

            val frameNs = if (callbacks.isIdle()) Constants.PAUSED_FRAME_MS * 1_000_000L
            else 1_000_000_000L / Constants.TARGET_FPS
            val sleepMs = (frameNs - (System.nanoTime() - frameStart)) / 1_000_000L
            if (sleepMs > 0) {
                try { sleep(sleepMs) } catch (e: InterruptedException) { /* loop re-checks `running` */ }
            }
        }
    }

    private fun render() {
        var canvas: Canvas? = null
        try {
            canvas = if (Build.VERSION.SDK_INT >= 26) holder.lockHardwareCanvas() else holder.lockCanvas()
            if (canvas != null) callbacks.onRender(canvas)
        } catch (e: Exception) {
            // Surface may disappear mid-frame during lifecycle changes; skip this frame.
        } finally {
            if (canvas != null) {
                try { holder.unlockCanvasAndPost(canvas) } catch (e: Exception) { /* surface gone */ }
            }
        }
    }
}
