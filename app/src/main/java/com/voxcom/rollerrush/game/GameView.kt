package com.voxcom.rollerrush.game

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.voxcom.rollerrush.RollerRushApp
import com.voxcom.rollerrush.utils.Constants

/**
 * The only View used for gameplay. It owns the surface lifecycle, the loop thread and touch
 * input, and delegates everything else (simulation -> GameWorld, drawing -> GameRenderer).
 */
@SuppressLint("ViewConstructor")
class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, GameLoop.Callbacks {

    interface Listener {
        /** Called on the game thread. */
        fun onGameOver(score: Int, distanceMeters: Int, coins: Int)
        /** Called on the UI thread (touch). */
        fun onPauseRequested()
    }

    var listener: Listener? = null

    private val app = context.applicationContext as RollerRushApp
    private val camera = Camera()
    private val renderer = GameRenderer(app.gameAssets, app.prefs)
    private val sprites = app.gameAssets.createPlayerSprites(app.playerData.character)
    private val world = GameWorld(camera, sprites, app.playerData.skateStats)
    private var loop: GameLoop? = null
    private var touchDownY = 0f
    private var touchMoved = false
    private var lastTapTime = 0L

    val state: GameState get() = world.state

    init {
        holder.addCallback(this)
        isFocusable = true
        world.start()
    }

    // ---- Controls used by GameActivity ---------------------------------------
    fun pauseGame() = world.pause()
    fun resumeGame() = world.resume()
    fun restartGame() = world.start()

    /** Stop the thread and free the skater's bitmaps. Call from Activity.onDestroy. */
    fun release() {
        stopLoop()
        sprites.recycle()
    }

    // ---- Surface lifecycle ---------------------------------------------------
    override fun surfaceCreated(holder: SurfaceHolder) = Unit

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        camera.onSizeChanged(width, height)
        renderer.onSizeChanged(width, height)
        if (loop == null) {
            loop = GameLoop(holder, this).also { it.startLoop() }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) = stopLoop()

    private fun stopLoop() {
        loop?.stopLoop()
        loop = null
    }

    // ---- GameLoop callbacks ----------------------------------------------------
    override fun onUpdate(dt: Float) {
        var over = false
        synchronized(world) {
            world.update(dt)
            over = world.consumeGameOverEvent()
        }
        if (over) listener?.onGameOver(world.score, world.distanceMeters, world.runCoins)
    }

    override fun onRender(canvas: Canvas) {
        synchronized(world) { renderer.render(canvas, world, camera) }
    }

    override fun isIdle(): Boolean = world.state == GameState.PAUSED

    // ---- Input -----------------------------------------------------------------
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val action = e.actionMasked
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                touchDownY = e.getY()
                touchMoved = false

                if (renderer.pauseRect.contains(e.getX(), e.getY())) {
                    listener?.onPauseRequested()
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (kotlin.math.abs(e.getY() - touchDownY) > Constants.SLIDE_SWIPE_DISTANCE * 0.35f) {
                    touchMoved = true
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (renderer.pauseRect.contains(e.getX(), e.getY())) return true
                if (world.state == GameState.PLAYING) {
                    val dy = e.getY() - touchDownY
                    val now = android.os.SystemClock.uptimeMillis()

                    if (dy < -Constants.JUMP_SWIPE_DISTANCE) {
                        // Swipe UP = jump.
                        world.requestJump()
                        lastTapTime = 0L
                    } else if (dy > Constants.SLIDE_SWIPE_DISTANCE) {
                        // Swipe DOWN = aggressive slide.
                        world.requestSlide()
                        lastTapTime = 0L
                    } else if (!touchMoved) {
                        // Double tap = activate a collected speed boost.
                        if (now - lastTapTime <= (Constants.DOUBLE_TAP_WINDOW * 1000f).toLong()) {
                            world.requestSpeedBoost()
                            lastTapTime = 0L
                        } else {
                            lastTapTime = now
                        }
                    }
                }
                performClick()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                touchMoved = false
                return true
            }
        }
        return true
    }
}
