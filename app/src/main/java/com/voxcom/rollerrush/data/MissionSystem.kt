
package com.voxcom.rollerrush.data

/**
 * Phase 2: persistent mission progress and one-time rewards.
 *
 * Call recordCompletedRun() once when a run ends.
 * GameActivity's gameOverHandled guard protects this call from duplicates.
 */
class MissionSystem(
    private val prefs: GamePreferences,
    private val playerData: PlayerData
) {
    data class Mission(
        val id: String,
        val title: String,
        val description: String,
        val target: Int,
        val progress: Int,
        val rewardCoins: Int = 0,
        val rewardRubies: Int = 0,
        val rewardGranted: Boolean = false
    ) {
        val fraction: Float
            get() = (progress.toFloat() / target.coerceAtLeast(1))
                .coerceIn(0f, 1f)

        val completed: Boolean
            get() = progress >= target
    }

    /**
     * Record a completed run. Cumulative objectives persist across runs;
     * single-run objectives use the latest completed run.
     */
    fun recordCompletedRun(
        score: Int,
        distanceMeters: Int,
        coins: Int
    ) {
        increment(KEY_RUNS, 1)
        increment(KEY_TOTAL_SCORE, score.coerceAtLeast(0))
        increment(KEY_TOTAL_DISTANCE, distanceMeters.coerceAtLeast(0))
        increment(KEY_TOTAL_COINS, coins.coerceAtLeast(0))

        prefs.putInt(KEY_LAST_RUN_SCORE, score.coerceAtLeast(0))
        prefs.putInt(KEY_LAST_RUN_DISTANCE, distanceMeters.coerceAtLeast(0))
        prefs.putInt(KEY_LAST_RUN_COINS, coins.coerceAtLeast(0))

        // Check rewards after saving all progress.
        missionDefinitions().forEach { mission ->
            if (mission.progress >= mission.target &&
                !isClaimed(mission.id)
            ) {
                grantReward(mission)
                markClaimed(mission.id)
            }
        }
    }

    /** Current mission progress for the future Missions screen. */
    fun getMissions(): List<Mission> =
        missionDefinitions().map { mission ->
            mission.copy(rewardGranted = isClaimed(mission.id))
        }

    /** Useful for UI labels and reward status. */
    fun isClaimed(missionId: String): Boolean =
        missionId in prefs.getStringSet(KEY_CLAIMED)

    private fun missionDefinitions(): List<Mission> = listOf(
        Mission(
            id = "first_run",
            title = "First Ride",
            description = "Complete 1 run",
            target = 1,
            progress = get(KEY_RUNS),
            rewardCoins = 50
        ),
        Mission(
            id = "coin_collector",
            title = "Coin Collector",
            description = "Collect 50 coins across all runs",
            target = 50,
            progress = get(KEY_TOTAL_COINS),
            rewardCoins = 100
        ),
        Mission(
            id = "road_runner",
            title = "Road Runner",
            description = "Travel 1,000 metres across all runs",
            target = 1_000,
            progress = get(KEY_TOTAL_DISTANCE),
            rewardCoins = 150
        ),
        Mission(
            id = "score_chaser",
            title = "Score Chaser",
            description = "Earn 5,000 total score across runs",
            target = 5_000,
            progress = get(KEY_TOTAL_SCORE),
            rewardCoins = 200
        ),
        Mission(
            id = "long_ride",
            title = "Long Ride",
            description = "Travel 500 metres in one run",
            target = 500,
            progress = get(KEY_LAST_RUN_DISTANCE),
            rewardRubies = 2
        ),
        Mission(
            id = "high_scorer",
            title = "High Scorer",
            description = "Score 1,000 points in one run",
            target = 1_000,
            progress = get(KEY_LAST_RUN_SCORE),
            rewardRubies = 3
        ),
        Mission(
            id = "coin_sprint",
            title = "Coin Sprint",
            description = "Collect 20 coins in one run",
            target = 20,
            progress = get(KEY_LAST_RUN_COINS),
            rewardRubies = 1
        )
    )

    private fun grantReward(mission: Mission) {
        if (mission.rewardCoins > 0) {
            playerData.addCoins(mission.rewardCoins)
        }
        if (mission.rewardRubies > 0) {
            playerData.addRushRubies(mission.rewardRubies)
        }
    }

    private fun markClaimed(missionId: String) {
        prefs.putStringSet(
            KEY_CLAIMED,
            prefs.getStringSet(KEY_CLAIMED) + missionId
        )
    }

    private fun get(key: String): Int =
        prefs.getInt(key, 0).coerceAtLeast(0)

    private fun increment(key: String, amount: Int) {
        if (amount <= 0) return

        val next = get(key).toLong() + amount.toLong()
        prefs.putInt(
            key,
            next.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        )
    }

    private companion object {
        const val KEY_RUNS = "mission_total_runs"
        const val KEY_TOTAL_SCORE = "mission_total_score"
        const val KEY_TOTAL_DISTANCE = "mission_total_distance"
        const val KEY_TOTAL_COINS = "mission_total_coins"

        const val KEY_LAST_RUN_SCORE = "mission_last_run_score"
        const val KEY_LAST_RUN_DISTANCE = "mission_last_run_distance"
        const val KEY_LAST_RUN_COINS = "mission_last_run_coins"

        const val KEY_CLAIMED = "mission_claimed_ids"
    }
}
