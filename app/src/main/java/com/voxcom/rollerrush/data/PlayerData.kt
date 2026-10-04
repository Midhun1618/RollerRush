package com.voxcom.rollerrush.data

/** Persistent player profile (currency, best score, equipped items). */
class PlayerData(private val prefs: GamePreferences) {

    var coins: Int
        get() = prefs.getInt(KEY_COINS, 0)
        private set(v) = prefs.putInt(KEY_COINS, v.coerceAtLeast(0))

    var highScore: Int
        get() = prefs.getInt(KEY_HIGH_SCORE, 0)
        set(v) = prefs.putInt(KEY_HIGH_SCORE, v)

    var characterPreset: Int
        get() = prefs.getInt(KEY_PRESET, 0).coerceIn(0, SkinCatalog.presets.lastIndex)
        set(v) = prefs.putInt(KEY_PRESET, v)

    var equippedSkateId: String
        get() = prefs.getString(KEY_SKATE, "skate_basic")
        set(v) = prefs.putString(KEY_SKATE, v)

    val character: CharacterData get() = SkinCatalog.presets[characterPreset]
    val skateStats: SkateStats get() = SkateStats.forSkate(equippedSkateId)

    fun addCoins(amount: Int) { if (amount > 0) coins += amount }

    /** @return true if the purchase went through. */
    fun spendCoins(amount: Int): Boolean {
        if (amount > coins) return false
        coins -= amount
        return true
    }

    fun resetAll() = prefs.clearAll()

    private companion object {
        const val KEY_COINS = "player_coins"
        const val KEY_HIGH_SCORE = "player_high_score"
        const val KEY_PRESET = "player_character_preset"
        const val KEY_SKATE = "player_equipped_skate"
    }
}
