package com.voxcom.rollerrush.systems

import com.voxcom.rollerrush.utils.Constants

/** Distance-based score. score = meters * 2 + coins * 30. */
class ScoreSystem {
    private var distanceUnits = 0f
    var distanceMeters = 0
        private set
    var score = 0
        private set

    fun reset() { distanceUnits = 0f; distanceMeters = 0; score = 0 }

    fun update(dt: Float, worldSpeed: Float, runCoins: Int) {
        distanceUnits += worldSpeed * dt
        distanceMeters = (distanceUnits / Constants.UNITS_PER_METER).toInt()
        score = distanceMeters * Constants.SCORE_PER_METER + runCoins * Constants.SCORE_PER_COIN
    }
}
