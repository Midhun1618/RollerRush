package com.voxcom.rollerrush.utils

/**
 * Central place for every tunable number.
 *
 * All gameplay distances are in WORLD UNITS (not pixels). The world is always
 * WORLD_HEIGHT units tall; the Camera scales that to the real screen.
 */
object Constants {
    // ---------------------------------------------------------------- World
    const val WORLD_HEIGHT = 360f          // logical height, always fully visible
    const val MIN_VIEW_WIDTH = 560f        // narrowest visible world width (4:3 tablets)
    const val GROUND_Y = 300f              // y of the ground surface (y grows downward)
    const val GROUND_THICKNESS = 60f
    const val PLAYER_X = 100f              // fixed horizontal position of the skater
    const val SPAWN_MARGIN = 40f           // spawn this far beyond the right screen edge
    const val BITMAP_SCALE = 4f            // generated sprite pixels per world unit

    // -------------------------------------------------------------- Physics
    const val GRAVITY = 1700f              // units / s^2
    const val JUMP_HEIGHT_BASE = 95f       // units (modified by SkateStats.jumpLevel)

    // ------------------------------------------------------------ Game loop
    const val TARGET_FPS = 60
    const val MAX_FRAME_DT = 0.05f         // clamp huge frame gaps (e.g. after a hiccup)
    const val MAX_FIXED_STEP = 0.02f       // update() is called with steps <= this
    const val PAUSED_FRAME_MS = 50L        // idle redraw rate while paused

    // ----------------------------------------------------------- Difficulty
    const val BASE_SPEED = 200f            // world units / s at start
    const val MAX_SPEED = 420f
    const val SPEED_INCREASE = 3.2f        // units / s gained per second played
    const val SPAWN_INTERVAL_START = 2.1f  // seconds between obstacles at BASE_SPEED
    const val SPAWN_INTERVAL_MIN = 0.95f   // seconds between obstacles at MAX_SPEED
    const val SPAWN_JITTER = 0.15f         // +/- random fraction applied to the interval
    const val FIRST_SPAWN_DELAY = 1.5f
    const val TALL_UNLOCK_METERS = 120     // obstacle variety unlocks by distance
    const val MOVING_UNLOCK_METERS = 300
    const val MOVING_EXTRA_SPEED = 70f     // moving obstacles approach faster than the world
    const val MOVING_HOP_HEIGHT = 18f

    // ---------------------------------------------------------------- Score
    const val UNITS_PER_METER = 10f
    const val SCORE_PER_METER = 2
    const val SCORE_PER_COIN = 30

    // ------------------------------------------------------- Entity pooling
    const val MAX_OBSTACLES = 16
    const val MAX_COINS = 48
    const val MAX_POWERUPS = 4

    // ---------------------------------------------------------------- Coins
    const val COIN_RADIUS = 9f
    const val COIN_PICKUP_RADIUS = 7.5f    // smaller than the visual radius on purpose

    // ----------------------------------------------------------- Game over
    const val GAME_OVER_DELAY = 0.9f       // seconds of crash animation before the screen switches

    // ------------------------------------------------------- Intent extras
    const val EXTRA_SCORE = "extra_score"
    const val EXTRA_DISTANCE = "extra_distance"
    const val EXTRA_COINS = "extra_coins"
    const val EXTRA_HIGH_SCORE = "extra_high_score"
}
