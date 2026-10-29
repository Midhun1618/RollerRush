
package com.voxcom.rollerrush.data

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
        val completed: Boolean = false,
        val rewardClaimed: Boolean = false
    ) {
        val fraction: Float
            get() = (progress.toFloat() / target.coerceAtLeast(1))
                .coerceIn(0f, 1f)

        val readyToClaim: Boolean
            get() = completed && !rewardClaimed
    }

    /**
     * Records one completed run and returns IDs of missions
     * newly completed during this run.
     *
     * Does NOT grant mission rewards.
     */
    fun recordCompletedRun(
        score: Int,
        distanceMeters: Int,
        coins: Int
    ): List<String> {
        val before = missionDefinitions()
            .associate { it.id to it.completed }

        increment(KEY_RUNS, 1)
        increment(KEY_TOTAL_SCORE, score)
        increment(KEY_TOTAL_DISTANCE, distanceMeters)
        increment(KEY_TOTAL_COINS, coins)

        // Best-run progress must not decrease after a shorter run.
        putMax(KEY_BEST_RUN_SCORE, score)
        putMax(KEY_BEST_RUN_DISTANCE, distanceMeters)
        putMax(KEY_BEST_RUN_COINS, coins)

        val newlyCompleted = missionDefinitions()
            .filter { it.completed && before[it.id] != true }
            .map { it.id }
            .toSet()

        if (newlyCompleted.isNotEmpty()) {
            prefs.putStringSet(
                KEY_PENDING,
                prefs.getStringSet(KEY_PENDING) + newlyCompleted
            )
        }

        return newlyCompleted.toList()
    }

    fun getMissions(): List<Mission> {
        val claimed = prefs.getStringSet(KEY_CLAIMED)
        val pending = prefs.getStringSet(KEY_PENDING)

        return missionDefinitions().map { mission ->
            mission.copy(
                completed = mission.completed ||
                        mission.id in claimed ||
                        mission.id in pending,
                rewardClaimed = mission.id in claimed
            )
        }
    }

    fun getPendingClaimCount(): Int =
        getMissions().count { it.readyToClaim }

    /**
     * Returns true only when a pending reward was successfully claimed.
     */
    fun claimReward(missionId: String): Boolean {
        val pending = prefs.getStringSet(KEY_PENDING)
        val claimed = prefs.getStringSet(KEY_CLAIMED)

        if (missionId !in pending || missionId in claimed) {
            return false
        }

        val mission = missionDefinitions()
            .firstOrNull { it.id == missionId }
            ?: return false

        if (!mission.completed) return false

        // Remove pending status and mark claimed before crediting wallet.
        prefs.putStringSet(KEY_PENDING, pending - missionId)
        prefs.putStringSet(KEY_CLAIMED, claimed + missionId)

        if (mission.rewardCoins > 0) {
            playerData.addCoins(mission.rewardCoins)
        }

        if (mission.rewardRubies > 0) {
            playerData.addRushRubies(mission.rewardRubies)
        }

        return true
    }

    private fun missionDefinitions(): List<Mission> = listOf(
        Mission(
            id = "first_run",
            title = "First Ride",
            description = "Complete 1 run",
            target = 1,
            progress = get(KEY_RUNS),
            rewardCoins = 50,
            completed = get(KEY_RUNS) >= 1
        ),
        Mission(
            id = "coin_collector",
            title = "Coin Collector",
            description = "Collect 50 coins across all runs",
            target = 50,
            progress = get(KEY_TOTAL_COINS),
            rewardCoins = 100,
            completed = get(KEY_TOTAL_COINS) >= 50
        ),
        Mission(
            id = "road_runner",
            title = "Road Runner",
            description = "Travel 1,000 metres across all runs",
            target = 1000,
            progress = get(KEY_TOTAL_DISTANCE),
            rewardCoins = 150,
            completed = get(KEY_TOTAL_DISTANCE) >= 1000
        ),
        Mission(
            id = "score_chaser",
            title = "Score Chaser",
            description = "Earn 5,000 total score across runs",
            target = 5000,
            progress = get(KEY_TOTAL_SCORE),
            rewardCoins = 200,
            completed = get(KEY_TOTAL_SCORE) >= 5000
        ),
        Mission(
            id = "long_ride",
            title = "Long Ride",
            description = "Travel 500 metres in one run",
            target = 500,
            progress = get(KEY_BEST_RUN_DISTANCE),
            rewardRubies = 2,
            completed = get(KEY_BEST_RUN_DISTANCE) >= 500
        ),
        Mission(
            id = "high_scorer",
            title = "High Scorer",
            description = "Score 1,000 points in one run",
            target = 1000,
            progress = get(KEY_BEST_RUN_SCORE),
            rewardRubies = 3,
            completed = get(KEY_BEST_RUN_SCORE) >= 1000
        ),
        Mission(
            id = "coin_sprint",
            title = "Coin Sprint",
            description = "Collect 20 coins in one run",
            target = 20,
            progress = get(KEY_BEST_RUN_COINS),
            rewardRubies = 1,
            completed = get(KEY_BEST_RUN_COINS) >= 20
        )
    )

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

    private fun putMax(key: String, value: Int) {
        val safeValue = value.coerceAtLeast(0)
        if (safeValue > get(key)) {
            prefs.putInt(key, safeValue)
        }
    }

    private companion object {
        const val KEY_RUNS = "mission_total_runs"
        const val KEY_TOTAL_SCORE = "mission_total_score"
        const val KEY_TOTAL_DISTANCE = "mission_total_distance"
        const val KEY_TOTAL_COINS = "mission_total_coins"

        const val KEY_BEST_RUN_SCORE = "mission_best_run_score"
        const val KEY_BEST_RUN_DISTANCE = "mission_best_run_distance"
        const val KEY_BEST_RUN_COINS = "mission_best_run_coins"

        const val KEY_PENDING = "mission_pending_claim_ids"
        const val KEY_CLAIMED = "mission_claimed_ids"
    }
}
