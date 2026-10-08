package com.voxcom.rollerrush.game

import com.voxcom.rollerrush.data.SkateStats
import com.voxcom.rollerrush.entities.Coin
import com.voxcom.rollerrush.entities.Obstacle
import com.voxcom.rollerrush.entities.PowerUp
import com.voxcom.rollerrush.player.Player
import com.voxcom.rollerrush.player.PlayerAnimator
import com.voxcom.rollerrush.player.PlayerController
import com.voxcom.rollerrush.player.PlayerSprites
import com.voxcom.rollerrush.systems.CoinSystem
import com.voxcom.rollerrush.systems.CollisionSystem
import com.voxcom.rollerrush.systems.DifficultySystem
import com.voxcom.rollerrush.systems.ScoreSystem
import com.voxcom.rollerrush.systems.SpawnSystem
import com.voxcom.rollerrush.utils.Constants

/**
 * Owns ALL simulation state and runs one update step.
 * No Android drawing code in here.
 *
 * GameView/GameLoop call update(dt);
 * GameRenderer only reads the state.
 *
 * Methods that change [state] are @Synchronized and
 * GameView also locks on the world around update()/render(),
 * so UI-thread requests (pause, restart) never race the loop thread.
 */
class GameWorld(
    val camera: Camera,
    sprites: PlayerSprites,
    stats: SkateStats
) {

    @Volatile
    var state = GameState.MENU
        private set

    val player = Player(sprites)

    val controller =
        PlayerController(
            player,
            stats
        )

    val animator =
        PlayerAnimator(player)

    val difficulty =
        DifficultySystem()

    val scoreSystem =
        ScoreSystem()

    val coinSystem =
        CoinSystem()

    private val spawner =
        SpawnSystem()

    private val speedMultiplier =
        stats.speedMultiplier

    // ------------------------------------------------------------
    // POOLS
    // ------------------------------------------------------------

    val obstacles: List<Obstacle> =
        List(Constants.MAX_OBSTACLES) {
            Obstacle()
        }

    val coins: List<Coin> =
        List(Constants.MAX_COINS) {
            Coin()
        }

    val powerUps: List<PowerUp> =
        List(Constants.MAX_POWERUPS) {
            PowerUp()
        }

    // ------------------------------------------------------------
    // WORLD MOVEMENT
    // ------------------------------------------------------------

    /** Current world scroll speed (units/s). */
    var worldSpeed = 0f
        private set

    /** Total scrolled distance in world units. */
    var scrollX = 0f
        private set

    // ------------------------------------------------------------
    // GAME OVER
    // ------------------------------------------------------------

    private var gameOverTimer = 0f

    private var gameOverEventPending =
        false

    private var slowMotionTimer =
        0f

    private var timeScale =
        1f

    private var eventFired =
        false

    // ------------------------------------------------------------
    // SPEED BOOST
    // ------------------------------------------------------------

    private var speedBoostReady =
        false

    private var speedBoostTimer =
        0f

    // ------------------------------------------------------------
    // INTRO
    // ------------------------------------------------------------

    private var introTimer =
        0f

    // ------------------------------------------------------------
    // STATE CONTROL
    // ------------------------------------------------------------

    @Synchronized
    fun start() {

        // --------------------------------------------------------
        // RESET EVERYTHING
        // --------------------------------------------------------

        camera.resetEffects()

        player.reset()
        controller.reset()
        animator.reset()

        difficulty.reset()
        scoreSystem.reset()
        coinSystem.reset()
        spawner.reset()

        for (i in 0 until obstacles.size) {
            obstacles[i].active = false
        }

        for (i in 0 until coins.size) {
            coins[i].active = false
        }

        for (i in 0 until powerUps.size) {
            powerUps[i].active = false
        }

        scrollX = 0f

        worldSpeed =
            Constants.BASE_SPEED *
                    speedMultiplier

        gameOverTimer = 0f
        gameOverEventPending = false

        slowMotionTimer = 0f
        timeScale = 1f

        eventFired = false

        speedBoostReady = false
        speedBoostTimer = 0f

        camera.setSpeedBoost(false)

        // --------------------------------------------------------
        // START INTRO
        // --------------------------------------------------------

        introTimer = 0f

        player.x =
            Constants.INTRO_START_X

        player.bottomY =
            Constants.INTRO_START_Y

        player.grounded = false
        player.crashed = false
        player.vy = 0f
        player.setIntroRotation(Constants.INTRO_START_ROTATION)

        camera.startIntro()

        state =
            GameState.INTRO
    }

    @Synchronized
    fun pause() {

        if (
            state ==
            GameState.PLAYING
        ) {
            state =
                GameState.PAUSED
        }
    }

    @Synchronized
    fun resume() {

        if (
            state ==
            GameState.PAUSED
        ) {
            state =
                GameState.PLAYING
        }
    }

    // ------------------------------------------------------------
    // INPUT
    // ------------------------------------------------------------

    fun requestJump() =
        controller.requestJump()

    fun requestSlide() =
        controller.requestSlide()

    /** Double-tap activates a collected speed boost. */
    fun requestSpeedBoost() {

        if (
            state ==
            GameState.PLAYING &&
            speedBoostReady &&
            speedBoostTimer <= 0f
        ) {

            speedBoostReady = false

            speedBoostTimer =
                Constants.SPEED_BOOST_DURATION

            camera.setSpeedBoost(true)
        }
    }

    val speedBoostAvailable: Boolean
        get() = speedBoostReady

    val speedBoostActive: Boolean
        get() = speedBoostTimer > 0f

    // ------------------------------------------------------------
    // GAME OVER EVENT
    // ------------------------------------------------------------

    /** Returns true exactly once after the crash animation has finished. */
    @Synchronized
    fun consumeGameOverEvent(): Boolean {

        val v =
            gameOverEventPending

        gameOverEventPending =
            false

        return v
    }

    // ------------------------------------------------------------
    // POOLS
    // ------------------------------------------------------------

    fun acquireObstacle(): Obstacle? {

        for (i in 0 until obstacles.size) {

            if (!obstacles[i].active) {
                return obstacles[i]
            }
        }

        return null
    }

    fun acquireCoin(): Coin? {

        for (i in 0 until coins.size) {

            if (!coins[i].active) {
                return coins[i]
            }
        }

        return null
    }

    fun acquirePowerUp(): PowerUp? {

        for (i in 0 until powerUps.size) {

            if (!powerUps[i].active) {
                return powerUps[i]
            }
        }

        return null
    }

    // ------------------------------------------------------------
    // UPDATE
    // ------------------------------------------------------------

    /** One simulation step. */
    @Synchronized
    fun update(dt: Float) {

        when (state) {

            GameState.INTRO ->
                updateIntro(dt)

            GameState.PLAYING ->
                updatePlaying(dt)

            GameState.GAME_OVER ->
                updateGameOver(dt)

            else ->
                Unit
        }
    }

    // ------------------------------------------------------------
    // INTRO UPDATE
    // ------------------------------------------------------------

    private fun updateIntro(dt: Float) {

        introTimer += dt

        // Camera effects continue during the cinematic.
        camera.update(dt)

        // Keep the world at the beginning of the scene during the intro.
        // The intro image is a real part of the world and starts moving left
        // naturally once PLAYING begins.
        worldSpeed = Constants.BASE_SPEED * speedMultiplier
        scrollX = 0f

        animator.updateIntro(
            introTimer,
            dt
        )

        // Once the first skating push begins, start advancing the world
        // underneath the player. This makes intro.png travel LEFT as the
        // skater starts moving, instead of appearing to drift right when
        // the cinematic camera hands control back to gameplay.
        val introScrollStart =
            Constants.INTRO_START_X - Constants.PLAYER_X

        val introSkateRaw =
            ((introTimer - Constants.INTRO_SKATE_START) /
                    (Constants.INTRO_DURATION - Constants.INTRO_SKATE_START))
                .coerceIn(0f, 1f)

        val introSkateBlend =
            introSkateRaw * introSkateRaw * (3f - 2f * introSkateRaw)

        scrollX = introScrollStart * introSkateBlend

        player.updateHitboxes()

        // -------------------------------------------------------------
        // Finish cinematic
        // -------------------------------------------------------------
        if (introTimer >= Constants.INTRO_DURATION) {
            player.x = Constants.PLAYER_X
            player.bottomY = Constants.GROUND_Y
            player.grounded = true
            player.crashed = false
            player.vy = 0f
            player.setIntroRotation(0f)

            // Do NOT reset the animator here. The intro skating pose flows
            // directly into gameplay.
            camera.endIntro()
            state = GameState.PLAYING
        }
    }

    // ------------------------------------------------------------
    // NORMAL GAMEPLAY
    // ------------------------------------------------------------

    private fun updatePlaying(
        dt: Float
    ) {

        // Camera effects run in real time.
        camera.update(dt)

        // --------------------------------------------------------
        // SPEED BOOST
        // --------------------------------------------------------

        if (
            speedBoostTimer > 0f
        ) {

            speedBoostTimer =
                (
                        speedBoostTimer -
                                dt
                        )
                    .coerceAtLeast(0f)

            if (
                speedBoostTimer == 0f
            ) {

                camera.setSpeedBoost(false)
            }
        }

        // --------------------------------------------------------
        // HIT SLOW MOTION
        // --------------------------------------------------------

        if (
            slowMotionTimer > 0f
        ) {

            slowMotionTimer =
                (
                        slowMotionTimer -
                                dt
                        )
                    .coerceAtLeast(0f)

            timeScale =
                0.28f +
                        0.72f *
                        (
                                1f -
                                        slowMotionTimer /
                                        Constants.HIT_SLOW_MOTION_DURATION
                                )

        } else {

            timeScale =
                1f
        }

        val simDt =
            dt *
                    timeScale

        // --------------------------------------------------------
        // DIFFICULTY / WORLD SPEED
        // --------------------------------------------------------

        difficulty.update(
            simDt
        )

        val boostMultiplier =
            if (
                speedBoostTimer > 0f
            ) {
                Constants.SPEED_BOOST_MULTIPLIER
            } else {
                1f
            }

        worldSpeed =
            difficulty.speed *
                    speedMultiplier *
                    boostMultiplier

        scrollX +=
            worldSpeed *
                    simDt

        // --------------------------------------------------------
        // PLAYER
        // --------------------------------------------------------

        val wasGrounded =
            player.grounded

        val wasSliding =
            player.sliding

        controller.update(
            simDt
        )

        // --------------------------------------------------------
        // CAMERA EVENTS
        // --------------------------------------------------------

        if (
            wasGrounded &&
            !player.grounded &&
            !player.sliding
        ) {

            camera.triggerJump()
        }

        if (
            !wasSliding &&
            player.sliding
        ) {

            camera.triggerSlide()
        }

        if (
            wasSliding &&
            !player.sliding
        ) {

            camera.endSlide()
        }

        if (
            player.hasLandedEvent()
        ) {

            camera.triggerLanding(
                1f
            )
        }

        // --------------------------------------------------------
        // PLAYER ANIMATION
        // --------------------------------------------------------

        animator.update(
            simDt,
            worldSpeed,
            speedBoostTimer > 0f
        )

        player.updateHitboxes()

        // --------------------------------------------------------
        // SPAWN
        // --------------------------------------------------------

        spawner.update(
            simDt,
            this
        )

        // --------------------------------------------------------
        // OBSTACLES
        // --------------------------------------------------------

        for (
        i in 0 until obstacles.size
        ) {

            val o =
                obstacles[i]

            if (o.active) {

                o.update(
                    simDt,
                    worldSpeed
                )

                if (
                    o.isOffScreenLeft()
                ) {

                    o.active =
                        false
                }
            }
        }

        // --------------------------------------------------------
        // COINS
        // --------------------------------------------------------

        for (
        i in 0 until coins.size
        ) {

            val c =
                coins[i]

            if (c.active) {

                c.update(
                    simDt,
                    worldSpeed
                )

                if (
                    c.isOffScreenLeft()
                ) {

                    c.active =
                        false
                }
            }
        }

        // --------------------------------------------------------
        // POWER UPS
        // --------------------------------------------------------

        for (
        i in 0 until powerUps.size
        ) {

            val p =
                powerUps[i]

            if (p.active) {

                p.update(
                    simDt,
                    worldSpeed
                )

                if (
                    p.isOffScreenLeft()
                ) {

                    p.active =
                        false
                }
            }
        }

        // --------------------------------------------------------
        // COIN PICKUPS
        // --------------------------------------------------------

        coinSystem.update(
            coins,
            player
        )

        // --------------------------------------------------------
        // POWER UP PICKUPS
        // --------------------------------------------------------

        for (
        i in 0 until powerUps.size
        ) {

            val p =
                powerUps[i]

            if (!p.active) {
                continue
            }

            if (
                p.type ==
                com.voxcom.rollerrush.entities.PowerUpType.SPEED &&
                CollisionSystem.playerTouchesCircle(
                    player,
                    p.x +
                            p.width *
                            0.5f,
                    p.y +
                            p.height *
                            0.5f,
                    12f
                )
            ) {

                p.collect()

                speedBoostReady = true
            }
        }

        // --------------------------------------------------------
        // SCORE
        // --------------------------------------------------------

        scoreSystem.update(
            simDt,
            worldSpeed,
            coinSystem.runCoins
        )

        // --------------------------------------------------------
        // OBSTACLE COLLISION
        // --------------------------------------------------------

        if (
            CollisionSystem.playerHitsObstacle(
                player,
                obstacles
            )
        ) {

            player.crash()

            camera.triggerCrash()

            slowMotionTimer =
                Constants.HIT_SLOW_MOTION_DURATION

            timeScale =
                Constants.HIT_SLOW_MOTION_SCALE

            state =
                GameState.GAME_OVER

            gameOverTimer =
                0f
        }
    }

    // ------------------------------------------------------------
    // GAME OVER
    // ------------------------------------------------------------

    private fun updateGameOver(
        dt: Float
    ) {

        camera.update(dt)

        // --------------------------------------------------------
        // CRASH SLOW MOTION
        // --------------------------------------------------------

        val crashScale =
            if (
                slowMotionTimer > 0f
            ) {

                slowMotionTimer =
                    (
                            slowMotionTimer -
                                    dt
                            )
                        .coerceAtLeast(0f)

                Constants.HIT_SLOW_MOTION_SCALE

            } else {

                1f
            }

        controller.update(
            dt *
                    crashScale
        )

        animator.update(
            dt *
                    crashScale,
            0f
        )

        player.updateHitboxes()

        // --------------------------------------------------------
        // GAME OVER TIMER
        // --------------------------------------------------------

        /*
         * Timer uses real time so the game-over screen is not
         * delayed by slow motion.
         */
        gameOverTimer +=
            dt

        if (
            gameOverTimer >=
            Constants.GAME_OVER_DELAY &&
            !gameOverEventPending &&
            !eventFired
        ) {

            gameOverEventPending =
                true

            eventFired =
                true
        }
    }

    // ------------------------------------------------------------
    // PUBLIC GAME DATA
    // ------------------------------------------------------------

    val runCoins: Int
        get() =
            coinSystem.runCoins

    val score: Int
        get() =
            scoreSystem.score

    val distanceMeters: Int
        get() =
            scoreSystem.distanceMeters
}
