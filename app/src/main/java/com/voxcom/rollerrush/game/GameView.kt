package com.voxcom.rollerrush.game

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.voxcom.rollerrush.RollerRushApp

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
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            val i = e.actionIndex
            if (renderer.pauseRect.contains(e.getX(i), e.getY(i))) {
                listener?.onPauseRequested()
            } else if (world.state == GameState.PLAYING) {
                world.requestJump()            // tap anywhere else = jump (ignored in the air by PlayerController)
            }
        }
        return true
    }
}
