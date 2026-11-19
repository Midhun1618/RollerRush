package com.voxcom.rollerrush.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import com.voxcom.rollerrush.R
import com.voxcom.rollerrush.data.MissionSystem
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.voxcom.rollerrush.data.LeaderboardEntry
import java.text.NumberFormat
import java.util.Locale

class MainMenuActivity : BaseActivity() {

    private lateinit var coinsLabel: TextView
    private lateinit var rushRubiesLabel: TextView
    private lateinit var highScoreLabel: TextView
    private lateinit var playButton: ImageView
    private lateinit var missionButton: ImageView
    private lateinit var shopButton: ImageView
    private lateinit var rushTitle: ImageView
    private lateinit var settingsButton: ImageView
    private lateinit var levelLabel: TextView
    private lateinit var missionBadge: TextView
    private lateinit var playerNameLabel: TextView
    private var profileLoadRequested = false

    private var titleFrameIndex = 0
    private var titleAnimator: ValueAnimator? = null
    private var playPulseAnimator: AnimatorSet? = null

    private lateinit var leaderboardPanel: LinearLayout
    private lateinit var leaderboardLeaderName: TextView
    private lateinit var leaderboardLeaderDistance: TextView
    private lateinit var leaderboardRows: LinearLayout
    private lateinit var leaderboardExpandIcon: TextView

    private var leaderboardExpanded = false
    private var leaderboardAnimation: AnimatorSet? = null

    private val titleFrames by lazy {
        intArrayOf(
            R.drawable.rush_title1,
            R.drawable.rush_title2,
            R.drawable.rush_title3,
            R.drawable.rush_title4
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        coinsLabel = findViewById(R.id.coinsLabel)
        rushRubiesLabel = findViewById(R.id.rubiesLabel)
        highScoreLabel = findViewById(R.id.scoreLabel)
        levelLabel = findViewById(R.id.levelLabel)
        playButton = findViewById(R.id.playButton)
        missionButton = findViewById(R.id.characterButton)
        shopButton = findViewById(R.id.shopButton)
        rushTitle = findViewById(R.id.rushTitle)
        settingsButton = findViewById(R.id.settingsButton)
        missionBadge = findViewById(R.id.missionBadge)
        playerNameLabel = findViewById(R.id.playerNameLabel)
        leaderboardPanel = findViewById(R.id.leaderboardPanel)
        leaderboardLeaderName = findViewById(R.id.leaderboardLeaderName)
        leaderboardLeaderDistance = findViewById(R.id.leaderboardLeaderDistance)
        leaderboardRows = findViewById(R.id.leaderboardRows)
        leaderboardExpandIcon = findViewById(R.id.leaderboardExpandIcon)

        leaderboardPanel.setOnClickListener {
            toggleLeaderboard()
        }

        playerNameLabel.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        playButton.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }

        missionButton.setOnClickListener {
            startActivity(Intent(this, MissionActivity::class.java))
        }

        shopButton.setOnClickListener {
            startActivity(Intent(this, ShopActivity::class.java))
        }

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        startPlayButtonPulse()
        startTitleAnimation()
    }

    private fun startPlayButtonPulse() {
        playPulseAnimator?.cancel()

        val zoomInX = ObjectAnimator.ofFloat(
            playButton, "scaleX", 1f, 1.05f
        ).apply {
            duration = 650L
            repeatCount = 1
            repeatMode = ValueAnimator.REVERSE
        }

        val zoomInY = ObjectAnimator.ofFloat(
            playButton, "scaleY", 1f, 1.05f
        ).apply {
            duration = 650L
            repeatCount = 1
            repeatMode = ValueAnimator.REVERSE
        }

        playPulseAnimator = AnimatorSet().apply {
            playTogether(zoomInX, zoomInY)
            interpolator = AccelerateDecelerateInterpolator()

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!isFinishing && !isDestroyed) {
                        playButton.postDelayed({
                            if (!isFinishing && !isDestroyed) {
                                startPlayButtonPulse()
                            }
                        }, 250L)
                    }
                }
            })

            start()
        }
    }

    private fun startTitleAnimation() {
        titleAnimator?.cancel()

        titleFrameIndex = 0
        rushTitle.setImageResource(titleFrames[titleFrameIndex])

        titleAnimator = ValueAnimator.ofInt(
            0, titleFrames.size - 1
        ).apply {
            duration = 420L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = android.view.animation.LinearInterpolator()

            addUpdateListener { animator ->
                val frame = animator.animatedValue as Int

                if (frame != titleFrameIndex) {
                    titleFrameIndex = frame
                    rushTitle.setImageResource(titleFrames[frame])
                }
            }

            start()
        }

        rushTitle.animate()
            .scaleX(1.04f)
            .scaleY(1.04f)
            .setDuration(650L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                if (!isFinishing && !isDestroyed) {
                    rushTitle.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(650L)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .withEndAction {
                            if (!isFinishing && !isDestroyed) {
                                startTitlePulseAgain()
                            }
                        }
                        .start()
                }
            }
            .start()
    }

    private fun startTitlePulseAgain() {
        if (isFinishing || isDestroyed) return

        rushTitle.animate()
            .scaleX(1.01f)
            .scaleY(1.01f)
            .setDuration(650L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                if (!isFinishing && !isDestroyed) {
                    rushTitle.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(650L)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .withEndAction {
                            startTitlePulseAgain()
                        }
                        .start()
                }
            }
            .start()
    }

    override fun onResume() {
        super.onResume()
        refreshPlayerName()
        refreshLeaderboard()
        app.firebaseGameService.syncPendingRuns()

        coinsLabel.text = "${app.playerData.coins}"
        highScoreLabel.text = "${app.playerData.highScore}"
        levelLabel.text = "LEVEL ${app.playerData.level}"
        rushRubiesLabel.text = "${app.playerData.rushRubies}"

        if (::missionBadge.isInitialized) {
            val pending = MissionSystem(
                app.prefs,
                app.playerData
            ).getPendingClaimCount()

            missionBadge.text =
                if (pending > 99) "99+" else pending.toString()

            missionBadge.visibility =
                if (pending > 0) android.view.View.VISIBLE
                else android.view.View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        playPulseAnimator?.cancel()
        playPulseAnimator = null

        titleAnimator?.cancel()
        titleAnimator = null

        rushTitle.animate().cancel()
        playButton.animate().cancel()
        leaderboardAnimation?.cancel()

        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        android.util.Log.d("RollerRushBGM", "MainMenuActivity onStart")
        app.bgmManager.playLobby()
    }

    private fun refreshPlayerName() {
        val service = app.firebaseGameService
        val currentName = service.displayName

        if (!currentName.isNullOrBlank()) {
            playerNameLabel.text = "$currentName"
            profileLoadRequested = false
            return
        }

        playerNameLabel.text = "CONNECTING..."

        if (profileLoadRequested) return

        profileLoadRequested = true

        service.initialize { success, error ->
            runOnUiThread {
                profileLoadRequested = false

                if (isFinishing || isDestroyed) return@runOnUiThread

                if (success) {
                    playerNameLabel.text =
                        "${service.displayName ?: "RUSHER"}"
                } else {
                    playerNameLabel.text = "OFFLINE"

                    android.util.Log.w(
                        "RollerRushFirebase",
                        "Could not load menu username: $error"
                    )
                }
            }
        }
    }

    private fun refreshLeaderboard() {
        app.firebaseGameService.fetchTopLeaderboard { entries, error ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread

                if (entries.isEmpty()) {
                    leaderboardLeaderName.text =
                        if (error != null) "LEADERBOARD OFFLINE" else "NO RUSHERS YET"

                    leaderboardLeaderDistance.text =
                        if (error != null) "TAP TO RETRY" else "BE THE FIRST TO SET A RECORD"

                    leaderboardRows.removeAllViews()
                    return@runOnUiThread
                }

                val leader = entries.first()

                leaderboardLeaderName.text = leader.displayName
                leaderboardLeaderDistance.text =
                    "${formatLeaderboardDistance(leader.bestDistance)}  •  BEST DISTANCE"

                renderLeaderboardRows(entries)
            }
        }
    }

    private fun renderLeaderboardRows(entries: List<LeaderboardEntry>) {
        leaderboardRows.removeAllViews()

        entries.drop(1).take(4).forEachIndexed { index, entry ->
            val rank = index + 2

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), dp(6), dp(8), dp(6))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(43)
                )
            }

            val rankView = TextView(this).apply {
                text = "#$rank"
                textSize = 14f
                setTextColor(Color.rgb(255, 211, 75))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(38), dp(35))
            }

            val details = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1f
                )
            }

            val isCurrentPlayer =
                entry.uid == app.firebaseGameService.currentUid

            val name = TextView(this).apply {
                text = if (isCurrentPlayer) {
                    "${entry.displayName} (you)"
                } else {
                    entry.displayName
                }

                textSize = 13f
                maxLines = 1
                setTextColor(Color.WHITE)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            val skate = TextView(this).apply {
                text = formatSkateName(entry.bestSkateId)
                textSize = 9f
                maxLines = 1
                setTextColor(Color.LTGRAY)
            }

            details.addView(name)
            details.addView(skate)

            val distance = TextView(this).apply {
                text = formatLeaderboardDistance(entry.bestDistance)
                textSize = 12f
                setTextColor(Color.rgb(255, 211, 75))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            row.addView(rankView)
            row.addView(details)
            row.addView(distance)

            leaderboardRows.addView(row)
        }
    }

    private fun toggleLeaderboard() {
        val expanding = !leaderboardExpanded
        leaderboardExpanded = expanding

        leaderboardAnimation?.cancel()

        val startWidth = leaderboardPanel.width
        val startHeight = leaderboardPanel.height

        val targetWidth = dp(if (expanding) 430 else 300)
        val targetHeight = dp(if (expanding) {
            56 + (entriesHeight())
        } else {
            56
        })

        if (expanding) {
            leaderboardRows.visibility = View.VISIBLE
            leaderboardRows.alpha = 0f
            leaderboardRows.animate()
                .alpha(1f)
                .setDuration(250)
                .start()
        } else {
            leaderboardRows.animate()
                .alpha(0f)
                .setDuration(150)
                .start()
        }

        leaderboardExpandIcon.text = if (expanding) "⌃" else "⌄"

        val widthAnimator = ValueAnimator.ofInt(startWidth, targetWidth)
        val heightAnimator = ValueAnimator.ofInt(startHeight, targetHeight)

        widthAnimator.addUpdateListener {
            leaderboardPanel.layoutParams.width = it.animatedValue as Int
            leaderboardPanel.requestLayout()
        }

        heightAnimator.addUpdateListener {
            leaderboardPanel.layoutParams.height = it.animatedValue as Int
            leaderboardPanel.requestLayout()
        }

        leaderboardAnimation = AnimatorSet().apply {
            playTogether(widthAnimator, heightAnimator)
            duration = 350L
            interpolator = AccelerateDecelerateInterpolator()

            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (!leaderboardExpanded) {
                        leaderboardRows.visibility = View.GONE
                        leaderboardRows.alpha = 1f
                    }
                }
            })

            start()
        }
    }

    private fun entriesHeight(): Int {
        val count = leaderboardRows.childCount.coerceAtMost(4)
        return 16 + (count * 43) + 8
    }

    private fun formatLeaderboardDistance(distance: Long): String {
        return "${NumberFormat.getIntegerInstance(Locale.getDefault()).format(distance)} m"
    }

    private fun formatSkateName(skateId: String): String {
        return skateId.removePrefix("skate_")
            .replace("_", " ")
            .uppercase(Locale.ROOT)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

}
