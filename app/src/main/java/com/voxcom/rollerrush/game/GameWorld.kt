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
 * Owns ALL simulation state and runs one update step. No Android drawing code in here.
 * GameView/GameLoop call update(dt); GameRenderer only reads the state.
 *
 * Methods that change [state] are @Synchronized and GameView also locks on the world around
 * update()/render(), so UI-thread requests (pause, restart) never race the loop thread.
 */
class GameWorld(val camera: Camera, sprites: PlayerSprites, stats: SkateStats) {

    @Volatile var state = GameState.MENU
        private set

    val player = Player(sprites)
    val controller = PlayerController(player, stats)
    val animator = PlayerAnimator(player)

    val difficulty = DifficultySystem()
    val scoreSystem = ScoreSystem()
    val coinSystem = CoinSystem()
    private val spawner = SpawnSystem()
    private val speedMultiplier = stats.speedMultiplier

    // Pools: allocated once, re-used for the whole session.
    val obstacles: List<Obstacle> = List(Constants.MAX_OBSTACLES) { Obstacle() }
    val coins: List<Coin> = List(Constants.MAX_COINS) { Coin() }
    val powerUps: List<PowerUp> = List(Constants.MAX_POWERUPS) { PowerUp() }

    /** Current world scroll speed (units/s). Also drives parallax. */
    var worldSpeed = 0f
        private set
    /** Total scrolled distance in world units; used by the renderer for parallax. */
    var scrollX = 0f
        private set

    private var gameOverTimer = 0f
    private var gameOverEventPending = false
    private var eventFired = false      // game-over event is raised only once per run

    // ---- State control -----------------------------------------------------
    @Synchronized fun start() {
        player.reset(); controller.reset(); animator.reset()
        difficulty.reset(); scoreSystem.reset(); coinSystem.reset(); spawner.reset()
        for (i in 0 until obstacles.size) obstacles[i].active = false
        for (i in 0 until coins.size) coins[i].active = false
        for (i in 0 until powerUps.size) powerUps[i].active = false
        scrollX = 0f
        worldSpeed = Constants.BASE_SPEED * speedMultiplier
        gameOverTimer = 0f
        gameOverEventPending = false
        eventFired = false
        state = GameState.PLAYING
    }

    @Synchronized fun pause() { if (state == GameState.PLAYING) state = GameState.PAUSED }
    @Synchronized fun resume() { if (state == GameState.PAUSED) state = GameState.PLAYING }

    fun requestJump() = controller.requestJump()

    /** Returns true exactly once after the crash animation has finished. */
    @Synchronized fun consumeGameOverEvent(): Boolean {
        val v = gameOverEventPending
        gameOverEventPending = false
        return v
    }

    // ---- Pools ---------------------------------------------------------------
    // Indexed loops (not for-in) so no Iterator is allocated.
    fun acquireObstacle(): Obstacle? { for (i in 0 until obstacles.size) if (!obstacles[i].active) return obstacles[i]; return null }
    fun acquireCoin(): Coin? { for (i in 0 until coins.size) if (!coins[i].active) return coins[i]; return null }

    // ---- Update --------------------------------------------------------------
    /** One simulation step. [dt] is already clamped to a small fixed maximum by GameLoop. */
    @Synchronized fun update(dt: Float) {
        when (state) {
            GameState.PLAYING -> updatePlaying(dt)
            GameState.GAME_OVER -> updateGameOver(dt)
            else -> Unit // MENU / PAUSED: nothing moves, nothing spawns, physics frozen
        }
    }

    private fun updatePlaying(dt: Float) {
        // 1. difficulty -> world speed (skate speed level scales it)
        difficulty.update(dt)
        worldSpeed = difficulty.speed * speedMultiplier
        scrollX += worldSpeed * dt

        // 2. player physics, 3. skating animation, hitboxes follow the new position
        controller.update(dt)
        animator.update(dt, worldSpeed)
        player.updateHitboxes()

        // 4. spawn + move + cull obstacles / coins / power-ups
        spawner.update(dt, this)
        for (i in 0 until obstacles.size) { val o = obstacles[i]; if (o.active) { o.update(dt, worldSpeed); if (o.isOffScreenLeft()) o.active = false } }
        for (i in 0 until coins.size) { val c = coins[i]; if (c.active) { c.update(dt, worldSpeed); if (c.isOffScreenLeft()) c.active = false } }
        for (i in 0 until powerUps.size) { val p = powerUps[i]; if (p.active) { p.update(dt, worldSpeed); if (p.isOffScreenLeft()) p.active = false } }

        // 5. collisions: coins first so a coin and an obstacle in the same frame still pays out
        coinSystem.update(coins, player)
        scoreSystem.update(dt, worldSpeed, coinSystem.runCoins)
        if (CollisionSystem.playerHitsObstacle(player, obstacles)) {
            player.crash()
            state = GameState.GAME_OVER
            gameOverTimer = 0f
        }
    }

    private fun updateGameOver(dt: Float) {
        // Gameplay is stopped; only the crash tumble keeps animating.
        controller.update(dt)
        animator.update(dt, 0f)
        player.updateHitboxes()
        gameOverTimer += dt
        if (gameOverTimer >= Constants.GAME_OVER_DELAY && !gameOverEventPending && !eventFired) {
            gameOverEventPending = true
            eventFired = true
        }
    }

    val runCoins: Int get() = coinSystem.runCoins
    val score: Int get() = scoreSystem.score
    val distanceMeters: Int get() = scoreSystem.distanceMeters
}
