package com.voxcom.rollerrush.utils

/** Central place for every tunable number. */
object Constants {
    const val WORLD_HEIGHT = 360f
    const val MIN_VIEW_WIDTH = 560f
    const val GROUND_Y = 300f
    const val GROUND_THICKNESS = 90f
    const val PLAYER_X = 100f
    const val SPAWN_MARGIN = 40f
    const val BITMAP_SCALE = 4f

    const val GRAVITY = 1700f
    const val JUMP_HEIGHT_BASE = 95f

    // Intro / cinematic entrance (all times in seconds from run start)
    //   0.00-0.15  reveal: tight scene shot, skater waits above the frame
    //   0.15-0.77  drop + half flip, tuck -> reach (landing anticipation)
    //   0.77       IMPACT: deep superhero lunge, dust, shake, punch-in
    //   0.77-1.27  hero beat: settle, look up, camera slowly pushes in
    //   1.27-1.63  rise into the skating stance
    //   1.35-2.55  2-3 power strokes, world accelerates, camera pulls out
    //   2.55       hand-off to PLAYING (camera already back to normal)
    const val INTRO_REVEAL = 0.15f
    const val INTRO_FALL_DURATION = 0.62f
    const val INTRO_IMPACT_TIME = INTRO_REVEAL + INTRO_FALL_DURATION
    const val INTRO_ROTATION_START = 0.40f      // flip happens while the skater is on screen
    const val INTRO_ROTATION_DURATION = 0.30f
    const val INTRO_COMPRESS_DURATION = 0.07f
    const val INTRO_LANDING_HOLD = 0.50f        // hero beat (seconds after impact)
    const val INTRO_RECOVER_DURATION = 0.36f
    const val INTRO_SKATE_START = 1.35f
    const val INTRO_DURATION = 2.55f
    const val INTRO_STROKE_RATE = 2.0f
    const val INTRO_START_X = 250f
    const val INTRO_START_Y = 20f               // feet start above the zoomed frame
    const val INTRO_START_ROTATION = -200f
    const val INTRO_LANDING_ROTATION = 0f
    const val INTRO_LANDING_SHAKE = 0.7f
    const val INTRO_CAMERA_ZOOM = 1.6f
    const val INTRO_CAMERA_PAN_X = 0f
    const val INTRO_CAMERA_START_BLEND = 0.6f
    const val INTRO_CAMERA_IMPACT_BLEND = 0.92f
    const val INTRO_CAMERA_RELEASE = 1.25f

    const val INTRO_BACKGROUND_WORLD_WIDTH = 1000f
    const val INTRO_BACKGROUND_WORLD_HEIGHT = 360f
    const val INTRO_BACKGROUND_OFFSET_X = -100f

    const val TARGET_FPS = 60
    const val MAX_FRAME_DT = 0.05f
    const val MAX_FIXED_STEP = 0.02f
    const val PAUSED_FRAME_MS = 50L

    const val BASE_SPEED = 200f
    const val MAX_SPEED = 420f
    const val SPEED_INCREASE = 3.2f
    const val SPAWN_INTERVAL_START = 2.1f
    const val SPAWN_INTERVAL_MIN = 0.95f
    const val SPAWN_JITTER = 0.15f
    const val FIRST_SPAWN_DELAY = 1.5f
    const val TALL_UNLOCK_METERS = 120
    const val MOVING_UNLOCK_METERS = 300
    const val MOVING_EXTRA_SPEED = 70f
    const val MOVING_HOP_HEIGHT = 18f
    const val SLIDE_DURATION = 0.78f
    const val SLIDE_SWIPE_DISTANCE = 70f
    const val JUMP_SWIPE_DISTANCE = 70f
    const val DOUBLE_TAP_WINDOW = 0.28f
    const val OVERHEAD_UNLOCK_METERS = 180
    const val SPEED_BOOST_DURATION = 2.0f
    const val SPEED_BOOST_MULTIPLIER = 1.65f
    const val SPEED_BOOST_UNLOCK_METERS = 80
    const val SPEED_BOOST_SPAWN_MIN = 8.0f
    const val SPEED_BOOST_SPAWN_MAX = 14.0f
    const val SPEED_BOOST_CAMERA_ZOOM = 1.12f
    const val SPEED_BOOST_CAMERA_PAN = 42f

    const val UNITS_PER_METER = 10f
    const val SCORE_PER_METER = 2
    const val SCORE_PER_COIN = 30

    const val MAX_OBSTACLES = 16
    const val MAX_COINS = 48
    const val MAX_POWERUPS = 4

    const val COIN_RADIUS = 9f
    const val COIN_PICKUP_RADIUS = 7.5f

    const val GAME_OVER_DELAY = 1.2f
    const val HIT_SLOW_MOTION_DURATION = 0.38f
    const val HIT_SLOW_MOTION_SCALE = 0.28f

    const val EXTRA_SCORE = "extra_score"
    const val EXTRA_DISTANCE = "extra_distance"
    const val EXTRA_COINS = "extra_coins"
    const val EXTRA_HIGH_SCORE = "extra_high_score"
}
