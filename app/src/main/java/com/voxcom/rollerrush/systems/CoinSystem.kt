package com.voxcom.rollerrush.systems

import com.voxcom.rollerrush.entities.Coin
import com.voxcom.rollerrush.player.Player
import com.voxcom.rollerrush.utils.Constants

/** Tracks the coins collected THIS run and handles pickup. Banking into PlayerData happens at game over. */
class CoinSystem {
    var runCoins = 0
        private set

    fun reset() { runCoins = 0 }

    fun update(coins: List<Coin>, player: Player) {
        for (i in 0 until coins.size) {
            val c = coins[i]
            if (!c.active) continue
            // Pickup radius is smaller than the drawn coin so pickups feel precise.
            if (CollisionSystem.playerTouchesCircle(player, c.centerX, c.centerY, Constants.COIN_PICKUP_RADIUS)) {
                c.active = false
                runCoins++
            }
        }
    }
}
