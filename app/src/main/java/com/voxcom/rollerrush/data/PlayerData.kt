package com.voxcom.rollerrush.data

/** Persistent player profile (currency, best score, equipped items and career progression). */
class PlayerData(private val prefs: GamePreferences) {

    var coins: Int
        get() = prefs.getInt(KEY_COINS, 0)
        private set(v) = prefs.putInt(KEY_COINS, v.coerceAtLeast(0))

    var highScore: Int
        get() = prefs.getInt(KEY_HIGH_SCORE, 0)
        set(v) = prefs.putInt(KEY_HIGH_SCORE, v)

    /** Total score accumulated across completed runs. */
    var careerScore: Int
        get() = prefs.getInt(KEY_CAREER_SCORE, 0).coerceAtLeast(0)
        private set(value) = prefs.putInt(KEY_CAREER_SCORE, value.coerceAtLeast(0))

    val maxLevel: Int
        get() = LEVEL_THRESHOLDS.size

    /** Level is calculated from career score; existing thresholds are preserved. */
    val level: Int
        get() = levelForCareerScore(careerScore)

    /** Score required for the next level; null at max level. */
    val scoreForNextLevel: Int?
        get() = scoreForNextLevel(level)

    /** Progress through the current level, between 0 and 1. */
    val levelProgress: Float
        get() = levelProgressForCareerScore(careerScore)

    fun levelForCareerScore(score: Int): Int = LEVEL_THRESHOLDS
        .indexOfLast { score.coerceAtLeast(0) >= it }
        .plus(1)
        .coerceIn(1, maxLevel)

    /** First career-score value belonging to [level]. */
    fun levelStartScore(level: Int): Int =
        LEVEL_THRESHOLDS[(level - 1).coerceIn(0, LEVEL_THRESHOLDS.lastIndex)]

    /** Career-score threshold for the level after [level], or null at max level. */
    fun scoreForNextLevel(level: Int): Int? =
        LEVEL_THRESHOLDS.getOrNull(level.coerceIn(1, maxLevel))

    fun levelProgressForCareerScore(score: Int): Float {
        val safeScore = score.coerceAtLeast(0)
        val currentLevel = levelForCareerScore(safeScore)
        val start = levelStartScore(currentLevel)
        val next = scoreForNextLevel(currentLevel) ?: return 1f
        val range = (next - start).coerceAtLeast(1)
        return ((safeScore - start).toFloat() / range).coerceIn(0f, 1f)
    }

    /** Add the score from a completed run. */
    fun addCareerScore(amount: Int) {
        if (amount <= 0) return
        careerScore = (careerScore.toLong() + amount)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    var characterPreset: Int
        get() = prefs.getInt(KEY_PRESET, 0).coerceIn(0, SkinCatalog.presets.lastIndex)
        set(v) = prefs.putInt(KEY_PRESET, v.coerceIn(0, SkinCatalog.presets.lastIndex))

    var equippedSkateId: String
        get() = prefs.getString(KEY_SKATE, "skate_basic")
        set(v) = prefs.putString(KEY_SKATE, v)

    /** Character preset + the currently equipped shop skate. */
    val character: CharacterData
        get() = SkinCatalog.presets[characterPreset].copy(skateSkin = equippedSkateId)

    val skateStats: SkateStats
        get() = SkateStats.forSkate(equippedSkateId)

    var rushRubies: Int
        get() = prefs.getInt(KEY_RUSH_RUBIES, 0)
        private set(v) = prefs.putInt(KEY_RUSH_RUBIES, v.coerceAtLeast(0))

    fun addRushRubies(amount: Int = 1) {
        if (amount <= 0) return
        rushRubies = (rushRubies.toLong() + amount)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    fun spendRushRubies(amount: Int): Boolean {
        if (amount <= 0 || amount > rushRubies) return false
        rushRubies -= amount
        return true
    }

    fun addCoins(amount: Int) {
        if (amount > 0) coins += amount
    }

    fun spendCoins(amount: Int): Boolean {
        if (amount < 0 || amount > coins) return false
        coins -= amount
        return true
    }

    fun resetAll() = prefs.clearAll()

    private companion object {
        const val KEY_COINS = "player_coins"
        const val KEY_HIGH_SCORE = "player_high_score"
        const val KEY_PRESET = "player_character_preset"
        const val KEY_SKATE = "player_equipped_skate"
        const val KEY_RUSH_RUBIES = "player_rush_rubies"
        const val KEY_CAREER_SCORE = "player_career_score"

        // Do not change these values: existing players' saved levels depend on them.
        val LEVEL_THRESHOLDS = intArrayOf(
            0,          // Level 1
            500,        // Level 2
            2_500,      // Level 3
            8_000,      // Level 4
            20_000,     // Level 5
            40_000,     // Level 6
            70_000,     // Level 7
            110_000,    // Level 8
            160_000,    // Level 9
            225_000     // Level 10
        )
    }
}
