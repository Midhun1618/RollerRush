package com.voxcom.rollerrush.data

enum class MissionCategory(val label: String) {
    DISTANCE("DISTANCE"),
    SCORE("SCORE"),
    COINS("COINS"),
    RIDES("RIDES"),
    RECORDS("RECORDS")
}

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
        val rewardClaimed: Boolean = false,
        val unlockLevel: Int = 1,
        val category: MissionCategory = MissionCategory.RIDES,
        val locked: Boolean = false
    ) {
        val fraction: Float
            get() = (progress.toFloat() / target.coerceAtLeast(1))
                .coerceIn(0f, 1f)

        val readyToClaim: Boolean
            get() = !locked && completed && !rewardClaimed
    }

    private enum class Metric(
        val key: String,
        val category: MissionCategory
    ) {
        RUNS("mission_total_runs", MissionCategory.RIDES),
        TOTAL_SCORE("mission_total_score", MissionCategory.SCORE),
        TOTAL_DISTANCE("mission_total_distance", MissionCategory.DISTANCE),
        TOTAL_COINS("mission_total_coins", MissionCategory.COINS),
        BEST_SCORE("mission_best_run_score", MissionCategory.RECORDS),
        BEST_DISTANCE("mission_best_run_distance", MissionCategory.RECORDS),
        BEST_COINS("mission_best_run_coins", MissionCategory.RECORDS)
    }

    fun recordCompletedRun(
        score: Int,
        distanceMeters: Int,
        coins: Int
    ): List<String> {
        val pendingBefore = prefs.getStringSet(KEY_PENDING)

        increment(KEY_RUNS, 1)
        increment(KEY_TOTAL_SCORE, score)
        increment(KEY_TOTAL_DISTANCE, distanceMeters)
        increment(KEY_TOTAL_COINS, coins)

        putMax(KEY_BEST_RUN_SCORE, score)
        putMax(KEY_BEST_RUN_DISTANCE, distanceMeters)
        putMax(KEY_BEST_RUN_COINS, coins)

        syncPendingCompletions()

        return (prefs.getStringSet(KEY_PENDING) - pendingBefore).toList()
    }

    fun getMissions(): List<Mission> {
        syncPendingCompletions()

        val claimed = prefs.getStringSet(KEY_CLAIMED)
        val pending = prefs.getStringSet(KEY_PENDING)

        return missionDefinitions().map { mission ->
            val locked = playerData.level < mission.unlockLevel

            mission.copy(
                locked = locked,
                completed = !locked && (
                        mission.completed ||
                                mission.id in claimed ||
                                mission.id in pending
                        ),
                rewardClaimed = mission.id in claimed
            )
        }
    }

    fun getPendingClaimCount(): Int =
        getMissions().count { it.readyToClaim }

    fun getUnlockedMissionCount(): Int =
        getMissions().count { !it.locked }

    fun getNewlyUnlockedMissionCount(
        fromLevel: Int,
        toLevel: Int
    ): Int {
        if (toLevel <= fromLevel) return 0

        return getMissions().count {
            it.unlockLevel in (fromLevel + 1)..toLevel
        }
    }

    fun claimReward(missionId: String): Boolean {
        syncPendingCompletions()

        val pending = prefs.getStringSet(KEY_PENDING)
        val claimed = prefs.getStringSet(KEY_CLAIMED)

        if (missionId !in pending || missionId in claimed) {
            return false
        }

        val mission = missionDefinitions()
            .firstOrNull { it.id == missionId }
            ?: return false

        if (playerData.level < mission.unlockLevel || !mission.completed) {
            return false
        }

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

    private fun syncPendingCompletions() {
        val claimed = prefs.getStringSet(KEY_CLAIMED)
        val pending = prefs.getStringSet(KEY_PENDING)

        val newlyReady = missionDefinitions()
            .filter {
                !it.locked &&
                        it.completed &&
                        it.id !in claimed &&
                        it.id !in pending
            }
            .map { it.id }
            .toSet()

        if (newlyReady.isNotEmpty()) {
            prefs.putStringSet(KEY_PENDING, pending + newlyReady)
        }
    }

    private fun mission(
        id: String,
        title: String,
        description: String,
        target: Int,
        metric: Metric,
        unlockLevel: Int,
        coins: Int = 0,
        rubies: Int = 0
    ): Mission {
        val progress = get(metric.key)

        return Mission(
            id = id,
            title = title,
            description = description,
            target = target,
            progress = progress,
            rewardCoins = coins,
            rewardRubies = rubies,
            completed = progress >= target,
            unlockLevel = unlockLevel,
            category = metric.category,
            locked = playerData.level < unlockLevel
        )
    }

    /** Five missions per level: 50 missions in total. */
    private fun missionDefinitions(): List<Mission> = listOf(

        // LEVEL 1
        mission("first_run", "First Ride", "Complete 1 run", 1, Metric.RUNS, 1, coins = 50),
        mission("coin_collector", "Coin Collector", "Collect 50 coins across all runs", 50, Metric.TOTAL_COINS, 1, coins = 100),
        mission("road_runner", "Road Runner", "Travel 1,000 metres across all runs", 1_000, Metric.TOTAL_DISTANCE, 1, coins = 150),
        mission("score_chaser", "Score Chaser", "Earn 5,000 total score across runs", 5_000, Metric.TOTAL_SCORE, 1, coins = 200),
        mission("coin_sprint", "Coin Sprint", "Collect 20 coins in one run", 20, Metric.BEST_COINS, 1, rubies = 1),

        // LEVEL 2
        mission("long_ride", "Long Ride", "Travel 500 metres in one run", 500, Metric.BEST_DISTANCE, 2, rubies = 2),
        mission("high_scorer", "High Scorer", "Score 1,000 points in one run", 1_000, Metric.BEST_SCORE, 2, rubies = 3),
        mission("run_regular", "Getting Rolling", "Complete 5 runs", 5, Metric.RUNS, 2, coins = 120),
        mission("pocket_change", "Pocket Change", "Collect 150 coins across all runs", 150, Metric.TOTAL_COINS, 2, coins = 150),
        mission("street_distance", "Street Explorer", "Travel 3,000 metres across all runs", 3_000, Metric.TOTAL_DISTANCE, 2, coins = 175),

        // LEVEL 3
        mission("road_trip_5k", "Five-K Road Trip", "Travel 5,000 metres across all runs", 5_000, Metric.TOTAL_DISTANCE, 3, coins = 200),
        mission("score_builder_15k", "Score Builder", "Earn 15,000 total score", 15_000, Metric.TOTAL_SCORE, 3, coins = 225),
        mission("coin_stack_300", "Coin Stack", "Collect 300 coins across all runs", 300, Metric.TOTAL_COINS, 3, coins = 225),
        mission("ten_runs", "Regular Rider", "Complete 10 runs", 10, Metric.RUNS, 3, coins = 200),
        mission("best_ride_750", "Long-Distance Rider", "Travel 750 metres in one run", 750, Metric.BEST_DISTANCE, 3, rubies = 1),

        // LEVEL 4
        mission("distance_10k", "Road Warrior", "Travel 10,000 metres across all runs", 10_000, Metric.TOTAL_DISTANCE, 4, coins = 250),
        mission("score_30k", "Score Specialist", "Earn 30,000 total score", 30_000, Metric.TOTAL_SCORE, 4, coins = 250),
        mission("coin_500", "Coin Magnet", "Collect 500 coins across all runs", 500, Metric.TOTAL_COINS, 4, coins = 250),
        mission("runs_20", "Twenty Rides", "Complete 20 runs", 20, Metric.RUNS, 4, coins = 250),
        mission("best_score_2500", "Big Run Score", "Score 2,500 points in one run", 2_500, Metric.BEST_SCORE, 4, rubies = 2),

        // LEVEL 5
        mission("distance_20k", "City Cruiser", "Travel 20,000 metres across all runs", 20_000, Metric.TOTAL_DISTANCE, 5, coins = 300),
        mission("score_50k", "Fifty-K Club", "Earn 50,000 total score", 50_000, Metric.TOTAL_SCORE, 5, coins = 300),
        mission("coins_1000", "Coin Hoarder", "Collect 1,000 coins across all runs", 1_000, Metric.TOTAL_COINS, 5, coins = 300),
        mission("runs_35", "Dedicated Skater", "Complete 35 runs", 35, Metric.RUNS, 5, coins = 300),
        mission("best_distance_1500", "One-and-a-Half K", "Travel 1,500 metres in one run", 1_500, Metric.BEST_DISTANCE, 5, rubies = 2),

        // LEVEL 6
        mission("distance_35k", "Endless Streets", "Travel 35,000 metres across all runs", 35_000, Metric.TOTAL_DISTANCE, 6, coins = 350),
        mission("score_80k", "Score Veteran", "Earn 80,000 total score", 80_000, Metric.TOTAL_SCORE, 6, coins = 350),
        mission("coins_2000", "Treasure Hunter", "Collect 2,000 coins across all runs", 2_000, Metric.TOTAL_COINS, 6, coins = 350),
        mission("runs_50", "Half-Century Rider", "Complete 50 runs", 50, Metric.RUNS, 6, coins = 350),
        mission("best_score_5000", "Five-K Scorer", "Score 5,000 points in one run", 5_000, Metric.BEST_SCORE, 6, rubies = 2),

        // LEVEL 7
        mission("distance_55k", "Highway Legend", "Travel 55,000 metres across all runs", 55_000, Metric.TOTAL_DISTANCE, 7, coins = 400),
        mission("score_120k", "Score Champion", "Earn 120,000 total score", 120_000, Metric.TOTAL_SCORE, 7, coins = 400),
        mission("coins_3500", "Golden Pockets", "Collect 3,500 coins across all runs", 3_500, Metric.TOTAL_COINS, 7, coins = 400),
        mission("runs_75", "Skate Regular", "Complete 75 runs", 75, Metric.RUNS, 7, coins = 400),
        mission("best_distance_2500", "Two-and-a-Half K", "Travel 2,500 metres in one run", 2_500, Metric.BEST_DISTANCE, 7, rubies = 3),

        // LEVEL 8
        mission("distance_80k", "Urban Marathon", "Travel 80,000 metres across all runs", 80_000, Metric.TOTAL_DISTANCE, 8, coins = 450),
        mission("score_175k", "Score Master", "Earn 175,000 total score", 175_000, Metric.TOTAL_SCORE, 8, coins = 450),
        mission("coins_5000", "Five Thousand Coins", "Collect 5,000 coins across all runs", 5_000, Metric.TOTAL_COINS, 8, coins = 450),
        mission("runs_100", "Century Rider", "Complete 100 runs", 100, Metric.RUNS, 8, coins = 450),
        mission("best_score_10000", "Ten-K Run", "Score 10,000 points in one run", 10_000, Metric.BEST_SCORE, 8, rubies = 3),

        // LEVEL 9
        mission("distance_110k", "Road Conqueror", "Travel 110,000 metres across all runs", 110_000, Metric.TOTAL_DISTANCE, 9, coins = 500),
        mission("score_240k", "Score Icon", "Earn 240,000 total score", 240_000, Metric.TOTAL_SCORE, 9, coins = 500),
        mission("coins_7500", "Coin Empire", "Collect 7,500 coins across all runs", 7_500, Metric.TOTAL_COINS, 9, coins = 500),
        mission("runs_150", "Dedicated Legend", "Complete 150 runs", 150, Metric.RUNS, 9, coins = 500),
        mission("best_distance_4000", "Four-K Flight", "Travel 4,000 metres in one run", 4_000, Metric.BEST_DISTANCE, 9, rubies = 4),

        // LEVEL 10
        mission("distance_150k", "Master of the Road", "Travel 150,000 metres across all runs", 150_000, Metric.TOTAL_DISTANCE, 10, coins = 600),
        mission("score_350k", "Ultimate Score", "Earn 350,000 total score", 350_000, Metric.TOTAL_SCORE, 10, coins = 600),
        mission("coins_10000", "Ten-Thousand Treasure", "Collect 10,000 coins across all runs", 10_000, Metric.TOTAL_COINS, 10, coins = 600),
        mission("runs_200", "Two Hundred Rides", "Complete 200 runs", 200, Metric.RUNS, 10, coins = 600),
        mission("best_score_20000", "Perfect Run", "Score 20,000 points in one run", 20_000, Metric.BEST_SCORE, 10, rubies = 5)
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
