package com.voxcom.rollerrush.game

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.voxcom.rollerrush.RollerRushApp
import com.voxcom.rollerrush.audio.BgmManager
import com.voxcom.rollerrush.audio.CoinSoundManager
import com.voxcom.rollerrush.audio.GameSoundEffects
import com.voxcom.rollerrush.audio.HapticFeedbackManager
import com.voxcom.rollerrush.utils.Constants

/**
 * The only View used for gameplay. It owns the surface lifecycle,
 * the loop thread and touch input, and delegates everything else
 * (simulation -> GameWorld, drawing -> GameRenderer).
 */
@SuppressLint("ViewConstructor")
class GameView(context: Context) :
    SurfaceView(context),
    SurfaceHolder.Callback,
    GameLoop.Callbacks {

    interface Listener {
        /** Called on the game thread after the crash animation. */
        fun onGameOver(
            score: Int,
            distanceMeters: Int,
            coins: Int,
            mysteryCollected: Boolean,
            mysteryType: String,
            mysteryAmount: Int
        )

        /** Called on the UI thread. */
        fun onPauseRequested()
        fun onBoosterAvailabilityChanged(available: Boolean)
        fun onBoosterActivated()
        fun onMysteryRewardCollected()
    }

    var listener: Listener? = null

    private val app = context.applicationContext as RollerRushApp

    private val hapticFeedback =
        HapticFeedbackManager(context)
    private val camera = Camera()
    private val renderer = GameRenderer(app.gameAssets, app.prefs)
    private val sprites = app.gameAssets.createPlayerSprites(app.playerData.character)

    private val coinSoundManager = CoinSoundManager(context.applicationContext)
    private val gameSoundEffects =
        GameSoundEffects(context.applicationContext)

    private val bgmManager: BgmManager
        get() = (context.applicationContext as RollerRushApp).bgmManager


    private val world = GameWorld(
        camera,
        sprites,
        app.playerData.skateStats,
        coinSoundManager,
        { skateId ->
            gameSoundEffects.playBoosterForSkate(skateId)
        },
        {
            app.playerData.addRushRubies(1)
            gameSoundEffects.playRubyCollected()
        },
        {
            gameSoundEffects.playObstacleHit()
            hapticFeedback.obstacleHit()
        },
        {
            gameSoundEffects.playGotBooster()
        },
        {
            app.playerData.equippedSkateId
        },
        onBoosterActivated = {
            hapticFeedback.boosterActivated()
        },
        onMysteryBubbleCollected = {
            gameSoundEffects.playMysteryBubbleCollected()
        }
    )

    private var loop: GameLoop? = null
    private var touchDownY = 0f
    private var touchMoved = false
    private var lastTapTime = 0L

    private var lastBoosterAvailable = false
    private var lastBoosterActive = false
    private var lastMysteryCollected = false

    private val tutorialManager = TutorialManager(context)



    val state: GameState
        get() = world.state

    init {
        holder.addCallback(this)
        isFocusable = true
        world.start()
    }

    // ---- Controls used by GameActivity ---------------------------------------

    fun pauseGame() {
        world.pause()
        bgmManager.pauseGameplay()
    }

    fun resumeGame() {
        world.resume()

        if (world.state == GameState.PLAYING ||
            world.state == GameState.INTRO
        ) {
            bgmManager.playGameplay()
        }
    }


    fun restartGame() {
        synchronized(world) {
            world.start()
        }

        lastBoosterAvailable = false
        lastBoosterActive = false
        lastMysteryCollected = false

        bgmManager.playGameplay()

        listener?.onBoosterAvailabilityChanged(false)
    }


    /**
     * Stop the game thread and release resources.
     * Call from Activity.onDestroy().
     */
    fun release() {
        stopLoop()
        tutorialManager.release()
        sprites.recycle()
        gameSoundEffects.release()
        coinSoundManager.release()
    }

    // ---- Surface lifecycle ---------------------------------------------------

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (
            world.state == GameState.INTRO ||
            world.state == GameState.PLAYING
        ) {
            bgmManager.playGameplay()
        }
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
        camera.onSizeChanged(width, height)
        renderer.onSizeChanged(width, height)

        if (loop == null) {
            loop = GameLoop(holder, this).also {
                it.startLoop()
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopLoop()
        bgmManager.pauseGameplay()
    }

    private fun stopLoop() {
        loop?.stopLoop()
        loop = null
    }

    // ---- GameLoop callbacks --------------------------------------------------


    override fun onUpdate(dt: Float) {
        var over = false
        var boosterAvailable = false
        var boosterActive = false
        var mysteryCollected = false

        val stateBeforeUpdate = world.state

        synchronized(world) {
            world.update(dt)

            if (
                !tutorialManager.active &&
                !app.prefs.tutorialCompleted &&
                stateBeforeUpdate == GameState.INTRO &&
                world.state == GameState.PLAYING
            ) {
                tutorialManager.start()

                synchronized(world) {
                    world.pause()
                }
            }

            if (tutorialManager.active) {
                tutorialManager.update(dt)

                synchronized(world) {
                    if (
                        tutorialManager.shouldPauseWorld &&
                        world.state == GameState.PLAYING
                    ) {
                        world.pause()
                    } else if (
                        !tutorialManager.shouldPauseWorld &&
                        world.state == GameState.PAUSED
                    ) {
                        world.resume()
                    }
                }

                if (tutorialManager.consumeCompletionEvent()) {
                    app.prefs.tutorialCompleted = true
                }
            }

            over = world.consumeGameOverEvent()
            boosterAvailable = world.speedBoostAvailable
            boosterActive = world.speedBoostActive
            mysteryCollected = world.mysteryRewardCollected
        }

        if (boosterAvailable != lastBoosterAvailable) {
            lastBoosterAvailable = boosterAvailable
            listener?.onBoosterAvailabilityChanged(boosterAvailable)
        }

        if (boosterActive && !lastBoosterActive) {
            listener?.onBoosterActivated()
        }
        lastBoosterActive = boosterActive

        if (mysteryCollected && !lastMysteryCollected) {
            listener?.onMysteryRewardCollected()
        }
        lastMysteryCollected = mysteryCollected

        if (over) {
            bgmManager.pauseGameplay()

            listener?.onGameOver(
                world.score,
                world.distanceMeters,
                world.runCoins,
                world.mysteryRewardCollected,
                world.mysteryRewardType,
                world.mysteryRewardAmount
            )
        }
    }



    override fun onRender(canvas: Canvas) {
        synchronized(world) {
            renderer.render(canvas, world, camera)
        }

        if (tutorialManager.active) {
            tutorialManager.draw(
                canvas,
                canvas.width,
                canvas.height
            )
        }
    }



    override fun isIdle(): Boolean = world.state == GameState.PAUSED && !tutorialManager.active


    // ---- Input ---------------------------------------------------------------

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownY = e.y
                touchMoved = false

                if (renderer.pauseRect.contains(e.x, e.y)) {
                    listener?.onPauseRequested()
                }

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (
                    kotlin.math.abs(e.y - touchDownY) >
                    Constants.SLIDE_SWIPE_DISTANCE * 0.35f
                ) {
                    touchMoved = true
                }

                return true
            }

            MotionEvent.ACTION_UP -> {
                if (renderer.pauseRect.contains(e.x, e.y)) {
                    return true
                }

                if (tutorialManager.active) {
                    val tutorialDy = e.y - touchDownY

                    if (
                        kotlin.math.abs(tutorialDy) >
                        Constants.JUMP_SWIPE_DISTANCE
                    ) {
                        val action = tutorialManager.handleSwipe(tutorialDy)

                        if (action != null) {
                            synchronized(world) {
                                world.resume()

                                when (action) {
                                    TutorialManager.Action.JUMP ->
                                        world.requestJump()

                                    TutorialManager.Action.SLIDE ->
                                        world.requestSlide()
                                }
                            }
                        }
                    }

                    performClick()
                    return true
                }


                if (world.state == GameState.PLAYING) {
                    val dy = e.y - touchDownY
                    val now = android.os.SystemClock.uptimeMillis()

                    if (dy < -Constants.JUMP_SWIPE_DISTANCE) {
                        // Swipe up = jump.
                        world.requestJump()
                        lastTapTime = 0L

                    } else if (dy > Constants.SLIDE_SWIPE_DISTANCE) {
                        // Swipe down = aggressive slide.
                        world.requestSlide()
                        lastTapTime = 0L

                    } else if (!touchMoved) {
                        // Double tap = activate a collected speed boost.
                        if (
                            now - lastTapTime <=
                            (Constants.DOUBLE_TAP_WINDOW * 1000f).toLong()
                        ) {
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

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}