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
import com.voxcom.rollerrush.audio.CoinSoundManager
import com.voxcom.rollerrush.entities.RushRuby
import com.voxcom.rollerrush.entities.MysteryBubble
import com.voxcom.rollerrush.systems.PigeonSystem
import com.voxcom.rollerrush.entities.ContainerPlatform
import com.voxcom.rollerrush.systems.ContainerSystem
import com.voxcom.rollerrush.player.ContainerStuntController

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
    stats: SkateStats,
    coinSoundManager: CoinSoundManager,
    private val playBoosterSound: (String) -> Unit,
    private val onRushRubyCollected: () -> Unit,
    private val onObstacleHit: () -> Unit,
    private val gotBooster: () -> Unit,
    private val equippedSkateId: () -> String,
    private val onBoosterActivated: () -> Unit,
    private val onMysteryBubbleCollected: () -> Unit
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

    val coinSystem = CoinSystem(coinSoundManager)

    private val spawner = SpawnSystem()
    val pigeonSystem = PigeonSystem()

    private val containerSystem = ContainerSystem()
    private val containerStunt = ContainerStuntController(player)
    private var lastSupportY = Constants.GROUND_Y

    private val speedMultiplier =
        stats.speedMultiplier

    // ------------------------------------------------------------
    // POOLS
    // ------------------------------------------------------------

    val obstacles: List<Obstacle> =
        List(Constants.MAX_OBSTACLES) {
            Obstacle()
        }

    val containerPlatforms: List<ContainerPlatform> =
        List(Constants.MAX_CONTAINER_PLATFORMS) {
            ContainerPlatform()
        }

    val coins: List<Coin> =
        List(Constants.MAX_COINS) {
            Coin()
        }

    val powerUps: List<PowerUp> =
        List(Constants.MAX_POWERUPS) {
            PowerUp()
        }

    val rushRuby = RushRuby()
    var runRubies: Int = 0
        private set

    val mysteryBubble = MysteryBubble()

    var mysteryRewardType: String = ""
        private set

    var mysteryRewardAmount: Int = 0
        private set

    val mysteryRewardCollected: Boolean
        get() = mysteryRewardType.isNotEmpty()

    private val mysteryRewards = listOf(
        "coins" to 30,
        "coins" to 100,
        "coins" to 200,
        "coins" to 20,
        "coins" to 150,
        "coins" to 50,
        "rubies" to 3,
        "rubies" to 5,
        "rubies" to 10
    )

    private fun collectMysteryBubble() {
        if (!mysteryBubble.active || mysteryRewardCollected) return

        mysteryBubble.active = false

        val reward = mysteryRewards.random()
        mysteryRewardType = reward.first
        mysteryRewardAmount = reward.second

        // Play the mystery sound exactly when the bubble is collected.
        onMysteryBubbleCollected()
    }

    fun acquireRushRuby(): RushRuby? {
        return if (!rushRuby.active) rushRuby else null
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

    private var speedBoostReady = false

    private var speedBoostTimer = 0f

    private var introTimer = 0f


    // True when the intro was interrupted by pause(); resume() returns to INTRO.
    private var pausedFromIntro =
        false

    // One-shot: set at the intro touchdown, consumed by the renderer (dust).
    private var introImpactPending =
        false

    /** Returns true exactly once per intro landing. */
    @Synchronized
    fun consumeIntroImpact(): Boolean {
        val v = introImpactPending
        introImpactPending = false
        return v
    }

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

        containerSystem.reset()
        containerStunt.reset()

        for (platform in containerPlatforms) {
            platform.active = false
        }

        mysteryBubble.active = false
        mysteryRewardType = ""
        mysteryRewardAmount = 0

        for (i in 0 until obstacles.size) {
            obstacles[i].active = false
            obstacles[i].collisionEnabled = true
        }

        for (i in 0 until coins.size) {
            coins[i].active = false
        }

        for (i in 0 until powerUps.size) {
            powerUps[i].active = false
        }
        rushRuby.active = false
        runRubies = 0

        // Player stays at PLAYER_X during the intro, so the background is
        // staged INTRO_START_X - PLAYER_X units further along.
        scrollX = Constants.INTRO_START_X - Constants.PLAYER_X
        // Create the first pigeon flock before the intro begins.
        pigeonSystem.reset(scrollX)

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
        pausedFromIntro = false
        introImpactPending = false

        player.x =
            Constants.PLAYER_X

        player.bottomY =
            Constants.INTRO_START_Y

        player.grounded = false
        player.crashed = false
        player.vy = 0f
        player.setIntroRotation(Constants.INTRO_START_ROTATION)

        camera.setIntroBlend(Constants.INTRO_CAMERA_START_BLEND)

        // Pose the skater at t=0 now, so no stale (e.g. crashed) pose is
        // drawn for a frame before the first intro update.
        animator.updateIntro(0f, 0f)

        state =
            GameState.INTRO
    }

    @Synchronized
    fun pause() {

        if (state == GameState.PLAYING) {
            pausedFromIntro = false
            state = GameState.PAUSED
        } else if (state == GameState.INTRO) {
            // Freeze the intro; introTimer only advances in updateIntro().
            pausedFromIntro = true
            state = GameState.PAUSED
        }
    }

    @Synchronized
    fun resume() {

        if (state == GameState.PAUSED) {
            state =
                if (pausedFromIntro) GameState.INTRO else GameState.PLAYING
            pausedFromIntro = false
        }
    }

    // ------------------------------------------------------------
    // INPUT
    // ------------------------------------------------------------

    @Synchronized
    fun requestJump() {
        if (state != GameState.PLAYING) return

        val target = containerSystem.findStuntTarget(player.x)

        if (target != null && containerStunt.tryStart(target)) {
            return
        }

        controller.requestJump()
    }

    fun requestSlide() {
        if (state != GameState.INTRO) controller.requestSlide()
    }

    /** Double-tap activates a collected speed boost. */
    fun requestSpeedBoost() {
        if (
            state == GameState.PLAYING &&
            speedBoostReady &&
            speedBoostTimer <= 0f
        ) {
            speedBoostReady = false
            speedBoostTimer = Constants.SPEED_BOOST_DURATION

            playBoosterSound(equippedSkateId())

            camera.setSpeedBoost(true)
            onBoosterActivated()
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

    fun acquireContainerPlatform(): ContainerPlatform? {
        return containerPlatforms.firstOrNull { !it.active }
    }

    fun isContainerNear(x: Float, margin: Float): Boolean {
        return containerSystem.isContainerNear(x, margin)
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

    //------------------------------------------------------------
    // INTRO UPDATE
    //------------------------------------------------------------

    private fun updateIntro(dt: Float) {

        val prevTimer = introTimer
        introTimer += dt

        // Touchdown event: light shake + dust, exactly once.
        if (
            prevTimer < Constants.INTRO_IMPACT_TIME &&
            introTimer >= Constants.INTRO_IMPACT_TIME
        ) {
            camera.triggerLanding(Constants.INTRO_LANDING_SHAKE)
            introImpactPending = true
        }

        camera.setIntroBlend(introCameraBlend(introTimer))
        camera.update(dt)

        animator.updateIntro(
            introTimer,
            dt
        )

        // World is still until the first push, then eases out to the normal
        // gameplay speed (zero acceleration at the hand-off).
        val pushRaw = ((introTimer - Constants.INTRO_SKATE_START) / (Constants.INTRO_DURATION - Constants.INTRO_SKATE_START)).coerceIn(0f, 1f)

        val inv = 1f - pushRaw
        val speedBlend = 1f - inv * inv

        worldSpeed = Constants.BASE_SPEED * speedMultiplier * speedBlend
        scrollX += worldSpeed * dt

        // Keep the intro pigeons moving with the scrolling building.
        pigeonSystem.update(
            dt = dt,
            worldSpeed = worldSpeed,
            playerX = player.x,
            visibleLeft = camera.visibleLeft,
            visibleRight = camera.visibleRight
        )

        player.updateHitboxes()

        // -------------------------------------------------------------
        // Hand-off (runs once: state leaves INTRO immediately)
        // -------------------------------------------------------------
        if (introTimer >= Constants.INTRO_DURATION) {
            player.x = Constants.PLAYER_X
            player.bottomY = Constants.GROUND_Y
            player.grounded = true
            player.crashed = false
            player.vy = 0f
            player.setIntroRotation(0f)

            // The animator is NOT reset: its skating cycle continues.
            camera.setIntroBlend(0f)
            state = GameState.PLAYING
        }
    }

    /** Push in toward the landing, hold through impact, then ease back to normal. */
    /** Tight push-in during the drop, slow push through the hero beat, long pull-out. */
    private fun introCameraBlend(t: Float): Float {
        return when {
            t < Constants.INTRO_IMPACT_TIME -> {
                val x = (t / Constants.INTRO_IMPACT_TIME).coerceIn(0f, 1f)
                val s = x * x * (3f - 2f * x)
                Constants.INTRO_CAMERA_START_BLEND +
                        (Constants.INTRO_CAMERA_IMPACT_BLEND -
                                Constants.INTRO_CAMERA_START_BLEND) * s
            }
            t < Constants.INTRO_CAMERA_RELEASE -> {
                // Slow push-in during the hero beat.
                val x = ((t - Constants.INTRO_IMPACT_TIME) /
                        (Constants.INTRO_CAMERA_RELEASE - Constants.INTRO_IMPACT_TIME))
                    .coerceIn(0f, 1f)
                val s = x * x * (3f - 2f * x)
                Constants.INTRO_CAMERA_IMPACT_BLEND +
                        (1f - Constants.INTRO_CAMERA_IMPACT_BLEND) * s
            }
            else -> {
                val x = ((t - Constants.INTRO_CAMERA_RELEASE) /
                        (Constants.INTRO_DURATION - Constants.INTRO_CAMERA_RELEASE))
                    .coerceIn(0f, 1f)
                1f - x * x * (3f - 2f * x)
            }
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
        // Ambient roadside pigeon behaviour.
        pigeonSystem.update(
            dt = simDt,
            worldSpeed = worldSpeed,
            playerX = player.x,
            visibleLeft = camera.visibleLeft,
            visibleRight = camera.visibleRight
        )

        containerSystem.update(simDt, this)

        // --------------------------------------------------------
        // PLAYER
        // --------------------------------------------------------

        val wasGrounded = player.grounded
        val wasSliding = player.sliding
        val wasOnStreet = player.bottomY >= Constants.GROUND_Y - 1f

        // The stunt controller owns movement while a container stunt is active.
        // Otherwise, normal jump/slide physics run with container-roof support.
        val stuntWasActive = containerStunt.active

        if (stuntWasActive) {
            containerStunt.update(simDt)
        } else {
            controller.update(
                simDt,
                containerSystem.platformUnderPlayer(player.x)
            )
        }

        // Start the crouch animation before actual takeoff.
        if (controller.consumeJumpAnticipationStarted()) {
            animator.requestJumpAnticipation()
            camera.triggerJumpAnticipation()
        }

        // --------------------------------------------------------
        // CAMERA EVENTS
        // --------------------------------------------------------

        if (
            wasGrounded &&
            !player.grounded &&
            !player.sliding
        ) {
            // Camera effect happens only when the player actually takes off.
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

        if (containerStunt.active) {
            containerStunt.applyPose()
        }

        // Jump camera follow: height above the surface the skater last stood on.
        if (player.grounded || containerStunt.active) {
            lastSupportY = player.bottomY
            camera.setAirHeight(0f)
        } else {
            camera.setAirHeight(lastSupportY - player.bottomY)
        }

        // Container stunt camera: goals every frame + one-shot punches (grab / landing).
        camera.setStuntTarget(containerStunt.cameraTarget)
        val stuntImpact = containerStunt.consumeCameraImpact()
        if (stuntImpact > 0f) camera.triggerStuntImpact(stuntImpact)

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

                if (o.isOffScreenLeft(camera.visibleLeft)) {
                    o.active = false
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

                if (c.isOffScreenLeft(camera.visibleLeft)) {
                    c.active = false
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

                if (p.isOffScreenLeft(camera.visibleLeft)) {
                    p.active = false
                }
            }
        }

        // --------------------------------------------------------
        // RUSH RUBY MOVEMENT AND PICKUP
        // --------------------------------------------------------

        if (rushRuby.active) {
            // Move the ruby toward the player with the scrolling world.
            rushRuby.update(simDt, worldSpeed)

            // Remove it after it passes the visible left edge.
            if (rushRuby.isOffScreenLeft(camera.visibleLeft)) {
                rushRuby.active = false
            } else if (
                CollisionSystem.playerTouchesCircle(
                    player,
                    rushRuby.centerX,
                    rushRuby.centerY,
                    Constants.COIN_PICKUP_RADIUS
                )
            ) {
                rushRuby.active = false
                runRubies++
                onRushRubyCollected()
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
                p.type == com.voxcom.rollerrush.entities.PowerUpType.SPEED &&
                CollisionSystem.playerTouchesCircle(
                    player,
                    p.x + p.width * 0.5f,
                    p.y + p.height * 0.5f,
                    12f
                )
            ) {
                // Process pickup effects only once per booster.
                if (p.collect()) {
                    gotBooster()
                    speedBoostReady = true
                }
            }

        }
        // --------------------------------------------------------
        // RUSH RUBY PICKUP
        // --------------------------------------------------------

        if (
            rushRuby.active &&
            CollisionSystem.playerTouchesCircle(
                player,
                rushRuby.centerX,
                rushRuby.centerY,
                Constants.COIN_PICKUP_RADIUS
            )
        ) {
            rushRuby.active = false
            onRushRubyCollected()
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
            CollisionSystem.playerHitsObstacle(player, obstacles) ||
            (
                    !containerStunt.active &&
                            CollisionSystem.playerHitsContainerSide(
                                player,
                                containerPlatforms
                            )
                    )
        ) {

            onObstacleHit()

            // Disable obstacle collisions without removing
            // their visual appearance from the crash scene.
            for (obstacle in obstacles) {
                if (obstacle.active) {
                    obstacle.collisionEnabled = false
                }
            }

            // Cancel booster effects.
            speedBoostTimer = 0f
            speedBoostReady = false
            camera.setSpeedBoost(false)

            // Cancel active container stunt without removing
            // containers from the crash scene.
            containerStunt.reset()

            // Trigger crash animation and cinematic effects.
            player.crash()
            camera.triggerCrash()

            slowMotionTimer = Constants.HIT_SLOW_MOTION_DURATION
            timeScale = Constants.HIT_SLOW_MOTION_SCALE

            state = GameState.GAME_OVER
            gameOverTimer = 0f
        }

        if (mysteryBubble.active) {
            mysteryBubble.update(simDt, worldSpeed)

            if (mysteryBubble.isOffScreenLeft(camera.visibleLeft)) {
                mysteryBubble.active = false
            } else if (
                CollisionSystem.playerTouchesCircle(
                    player,
                    mysteryBubble.centerX,
                    mysteryBubble.centerY,
                    Constants.COIN_PICKUP_RADIUS * 2f
                )
            ) {
                collectMysteryBubble()
            }
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

        // Let the crash animation finish at normal animation speed.
        animator.update(dt, 0f)

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