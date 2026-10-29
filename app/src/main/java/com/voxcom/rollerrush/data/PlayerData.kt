package com.voxcom.rollerrush.data

/** Persistent player profile (currency, best score, equipped items). */
class PlayerData(private val prefs: GamePreferences) {

    var coins: Int
        get() = prefs.getInt(KEY_COINS, 0)
        private set(v) = prefs.putInt(KEY_COINS, v.coerceAtLeast(0))

    var highScore: Int
        get() = prefs.getInt(KEY_HIGH_SCORE, 0)
        set(v) = prefs.putInt(KEY_HIGH_SCORE, v)

    /**
     * Total score accumulated across completed runs.
     * Separate from the best single-run score.
     */
    var careerScore: Int
        get() = prefs.getInt(KEY_CAREER_SCORE, 0).coerceAtLeast(0)
        private set(value) = prefs.putInt(
            KEY_CAREER_SCORE,
            value.coerceAtLeast(0)
        )

    /** Level is calculated from career score. */
    val level: Int
        get() = LEVEL_THRESHOLDS
            .indexOfLast { careerScore >= it }
            .plus(1)
            .coerceAtLeast(1)

    /** Score required for the next level; null at the maximum level. */
    val scoreForNextLevel: Int?
        get() = LEVEL_THRESHOLDS.getOrNull(level)

    /** Progress through the current level, between 0 and 1. */
    val levelProgress: Float
        get() {
            val currentThreshold = LEVEL_THRESHOLDS[level - 1]
            val nextThreshold = scoreForNextLevel ?: return 1f
            val range = (nextThreshold - currentThreshold).coerceAtLeast(1)

            return ((careerScore - currentThreshold).toFloat() / range)
                .coerceIn(0f, 1f)
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

    val skateStats: SkateStats get() = SkateStats.forSkate(equippedSkateId)

    var rushRubies: Int
        get() = prefs.getInt(KEY_RUSH_RUBIES, 0)
        private set(v) = prefs.putInt(KEY_RUSH_RUBIES, v.coerceAtLeast(0))

    fun addRushRubies(amount: Int = 1) {
        if (amount <= 0) return

        rushRubies = (rushRubies.toLong() + amount)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    /** Returns true if the ruby purchase went through. */
    fun spendRushRubies(amount: Int): Boolean {
        if (amount <= 0 || amount > rushRubies) return false

        rushRubies -= amount
        return true
    }

    fun addCoins(amount: Int) { if (amount > 0) coins += amount }

    /** @return true if the purchase went through. */
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

        /**
         * Minimum career score required for each level.
         * Level 2 starts at 500; Level 5 starts at 20,000.
         */
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
