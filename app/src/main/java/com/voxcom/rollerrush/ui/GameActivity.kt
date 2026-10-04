package com.voxcom.rollerrush.ui

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import com.voxcom.rollerrush.game.GameState
import com.voxcom.rollerrush.game.GameView
import com.voxcom.rollerrush.utils.Constants

/**
 * Hosts the GameView and bridges it to the Android lifecycle / navigation.
 * No gameplay logic lives here.
 */
@Suppress("DEPRECATION")
class GameActivity : BaseActivity(), GameView.Listener {
    private lateinit var gameView: GameView
    private var pauseScreenOpen = false
    private var gameOverHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        app.gameAssets.ensureLoaded()                      // no-op if Splash already did it
        gameView = GameView(this).also { it.listener = this }
        setContentView(gameView)
    }

    override fun onPause() {
        super.onPause()
        gameView.pauseGame()                               // freeze simulation; state is kept exactly
    }

    override fun onResume() {
        super.onResume()
        // Returned from the app switcher / lock screen while paused: show the pause menu.
        if (gameView.state == GameState.PAUSED && !pauseScreenOpen) openPauseScreen()
    }

    override fun onDestroy() {
        gameView.release()
        super.onDestroy()
    }

    // ---- GameView.Listener -------------------------------------------------------
    override fun onPauseRequested() = runOnUiThread {
        gameView.pauseGame()
        if (!pauseScreenOpen) openPauseScreen()
    }

    override fun onGameOver(score: Int, distanceMeters: Int, coins: Int) = runOnUiThread {
        if (gameOverHandled) return@runOnUiThread
        gameOverHandled = true
        val pd = app.playerData
        pd.addCoins(coins)                                 // bank this run's coins exactly once
        if (score > pd.highScore) pd.highScore = score
        startActivity(Intent(this, GameOverActivity::class.java)
            .putExtra(Constants.EXTRA_SCORE, score)
            .putExtra(Constants.EXTRA_DISTANCE, distanceMeters)
            .putExtra(Constants.EXTRA_COINS, coins)
            .putExtra(Constants.EXTRA_HIGH_SCORE, pd.highScore))
        finish()
    }

    // ---- Pause screen -----------------------------------------------------------
    private fun openPauseScreen() {
        pauseScreenOpen = true
        startActivityForResult(Intent(this, PauseActivity::class.java), REQ_PAUSE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_PAUSE) return
        pauseScreenOpen = false
        when (resultCode) {
            PauseActivity.RESULT_HOME -> goHome()
            PauseActivity.RESULT_RESTART -> gameView.restartGame()
            else -> gameView.resumeGame()                  // RESUME, or any cancel
        }
    }

    override fun onBackPressed() {
        gameView.pauseGame()
        if (!pauseScreenOpen) openPauseScreen()
    }

    private fun goHome() {
        startActivity(Intent(this, MainMenuActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }

    private companion object { const val REQ_PAUSE = 1 }
}
