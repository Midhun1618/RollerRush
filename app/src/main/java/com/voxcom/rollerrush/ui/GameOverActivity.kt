package com.voxcom.rollerrush.ui

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.voxcom.rollerrush.R
import com.voxcom.rollerrush.data.MissionSystem
import com.voxcom.rollerrush.utils.Constants
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class GameOverActivity : BaseActivity() {

    private val runningAnimators = mutableListOf<Animator>()

    private var bubblePopPlayer: MediaPlayer? = null
    private var rewardRevealPlayer: MediaPlayer? = null

    private var revealStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_over)

        val score = intent.getIntExtra(Constants.EXTRA_SCORE, 0)
        val distance = intent.getIntExtra(Constants.EXTRA_DISTANCE, 0)
        val coins = intent.getIntExtra(Constants.EXTRA_COINS, 0)
        val best = intent.getIntExtra(Constants.EXTRA_HIGH_SCORE, 0)

        val mysteryCollected =
            intent.getBooleanExtra("mystery_collected", false)

        val mysteryType =
            intent.getStringExtra("mystery_type").orEmpty()

        val mysteryAmount =
            intent.getIntExtra("mystery_amount", 0)

        // The title is an ImageView in the current XML.
        val title = findViewById<ImageView>(R.id.gameOverTitle)

        val distanceView =
            findViewById<TextView>(R.id.distanceValue)

        val scoreView =
            findViewById<TextView>(R.id.scoreValue)

        val bestView =
            findViewById<TextView>(R.id.bestValue)

        val walletView =
            findViewById<TextView>(R.id.totalCoinsValue)

        val runCoinsView =
            findViewById<TextView>(R.id.runCoinsValue)

        val rewardCard =
            findViewById<View>(R.id.mysteryRewardCard)

        val bubble =
            findViewById<ImageView>(R.id.mysteryBubbleImage)

        val rewardTitle =
            findViewById<TextView>(R.id.mysteryRewardTitle)

        val rewardValue =
            findViewById<TextView>(R.id.mysteryRewardValue)
        val rushRubiesLabel =
            findViewById<TextView>(R.id.rubiesLabel)

        displayCompletedMissions(
            intent.getStringArrayListExtra(
                "missions_completed_this_run"
            ).orEmpty()
        )

        // Initial result values.
        distanceView.text = "0 m"
        scoreView.text = "0"
        bestView.text = "0"

        runCoinsView.text = "+0 COINS THIS RUN"
        walletView.text = "${app.playerData.coins}"
        rushRubiesLabel.text = "${app.playerData.rushRubies}"

        // Prepare the screen for a staged arcade-style entrance.
        val scoreCard = findViewById<View>(R.id.imageView4)
        val scoreStats = findViewById<View>(R.id.scoreStats)
        val buttons = findViewById<View>(R.id.linearLayout11)
        val coinsCounter = findViewById<View>(R.id.linearLayout2)
        val rubiesCounter = findViewById<View>(R.id.linearLayout3)
        val missionsPanel = findViewById<View>(R.id.completedMissionsPanel)

        title.alpha = 0f
        title.translationY = -90f
        title.rotation = -5f
        title.scaleX = 0.82f
        title.scaleY = 0.82f

        scoreCard.alpha = 0f
        scoreCard.translationX = -100f
        scoreCard.scaleX = 0.92f
        scoreCard.scaleY = 0.92f

        scoreStats.alpha = 0f
        scoreStats.translationX = -80f

        runCoinsView.alpha = 0f
        runCoinsView.translationY = 25f

        buttons.alpha = 0f
        buttons.translationY = 70f

        coinsCounter.alpha = 0f
        coinsCounter.translationY = -35f

        rubiesCounter.alpha = 0f
        rubiesCounter.translationY = -35f

        rewardCard.visibility = View.GONE

// 1. Game-over artwork slams into the scene.
        animateEntrance(
            view = title,
            delay = 100L,
            duration = 650L,
            fromX = 0f,
            fromY = -90f,
            fromScale = 0.82f,
            rotation = -5f
        )

// 2. Distance card slides in from the left.
        animateEntrance(
            view = scoreCard,
            delay = 420L,
            duration = 600L,
            fromX = -100f,
            fromY = 0f,
            fromScale = 0.92f
        )

// 3. Score appears after the distance card.
        animateEntrance(
            view = scoreStats,
            delay = 760L,
            duration = 480L,
            fromX = -80f,
            fromY = 0f
        )

// 4. Currency counters drop into place.
        animateEntrance(
            view = rubiesCounter,
            delay = 520L,
            duration = 420L,
            fromX = 0f,
            fromY = -35f
        )

        animateEntrance(
            view = coinsCounter,
            delay = 650L,
            duration = 420L,
            fromX = 0f,
            fromY = -35f
        )

// 5. Coins earned this run pop in.
        animateEntrance(
            view = runCoinsView,
            delay = 1050L,
            duration = 420L,
            fromX = 0f,
            fromY = 25f,
            fromScale = 0.85f
        )

// 6. Action buttons arrive last.
        animateEntrance(
            view = buttons,
            delay = 1250L,
            duration = 550L,
            fromX = 0f,
            fromY = 70f
        )

// Count the values in the same order as their visual entrance.
        title.postDelayed({
            if (!canAnimate()) return@postDelayed

            animateNumber(0, distance, 900) {
                distanceView.text = "$it m"
            }

            scoreView.postDelayed({
                if (!canAnimate()) return@postDelayed

                animateNumber(0, score, 1050) {
                    scoreView.text = it.toString()
                }
            }, 180)

            bestView.postDelayed({
                if (!canAnimate()) return@postDelayed

                animateNumber(0, best, 1100) {
                    bestView.text = it.toString()
                }
            }, 380)

            runCoinsView.postDelayed({
                if (!canAnimate()) return@postDelayed

                animateNumber(0, coins, 950) {
                    runCoinsView.text = "+$it COINS THIS RUN"
                }
            }, 550)

        }, 650L)

// Completed mission panel enters from the right.
        if (missionsPanel.visibility == View.VISIBLE) {
            missionsPanel.alpha = 0f
            missionsPanel.translationX = 110f

            animateEntrance(
                view = missionsPanel,
                delay = 850L,
                duration = 600L,
                fromX = 110f,
                fromY = 0f
            )
        }

// Small continuous movement so the buttons do not feel static.
        buttons.postDelayed({
            if (!canAnimate()) return@postDelayed

            val retryButton = findViewById<Button>(R.id.playAgainButton)

            retryButton.animate()
                .scaleX(1.035f)
                .scaleY(1.035f)
                .setDuration(650L)
                .withEndAction {
                    if (canAnimate()) {
                        retryButton.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(650L)
                            .withEndAction {
                                if (canAnimate()) {
                                    retryButton.postDelayed({
                                        if (canAnimate()) {
                                            retryButton.animate()
                                                .scaleX(1.035f)
                                                .scaleY(1.035f)
                                                .setDuration(650L)
                                                .start()
                                        }
                                    }, 300L)
                                }
                            }
                            .start()
                    }
                }
                .start()
        }, 1900L)
        title.alpha = 0f
        title.translationY = -24f

        rewardCard.visibility = View.GONE

        // Game-over title entrance.
        title.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(450)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Animate the statistics in sequence.
        title.postDelayed({
            if (!canAnimate()) return@postDelayed

            animateNumber(0, distance, 850) {
                distanceView.text = "$it m"
            }

            scoreView.postDelayed({
                if (!canAnimate()) return@postDelayed

                animateNumber(0, score, 1000) {
                    scoreView.text = it.toString()
                }
            }, 160)

            bestView.postDelayed({
                if (!canAnimate()) return@postDelayed

                animateNumber(0, best, 1100) {
                    bestView.text = it.toString()
                }
            }, 300)

            runCoinsView.postDelayed({
                if (!canAnimate()) return@postDelayed

                animateNumber(0, coins, 950) {
                    runCoinsView.text = "+$it COINS THIS RUN"
                }
            }, 450)
        }, 250)

        // Mystery reward reveal.
        if (mysteryCollected && mysteryAmount > 0) {
            rewardCard.visibility = View.VISIBLE

            rewardCard.alpha = 0f
            rewardCard.translationY = 30f

            bubble.setImageResource(R.drawable.mystery_bubble)
            bubble.alpha = 0f
            bubble.scaleX = 0.15f
            bubble.scaleY = 0.15f
            bubble.rotation = -20f
            bubble.rotationY = 0f

            rewardTitle.alpha = 0f
            rewardTitle.translationY = 10f

            rewardValue.alpha = 0f
            rewardValue.scaleX = 0.65f
            rewardValue.scaleY = 0.65f

            rewardValue.setTextColor(
                if (mysteryType == "rubies") {
                    Color.rgb(255, 111, 145)
                } else {
                    Color.rgb(255, 214, 107)
                }
            )

            // Enter the reward panel first.
            rewardCard.postDelayed({
                if (!canAnimate()) return@postDelayed

                rewardCard.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .start()

                // Bubble pop begins after the card enters.
                bubble.postDelayed({
                    if (!canAnimate()) return@postDelayed
                    playBubblePop()

                    bubble.animate()
                        .alpha(1f)
                        .scaleX(1.12f)
                        .scaleY(1.12f)
                        .rotation(8f)
                        .setDuration(380)
                        .setInterpolator(OvershootInterpolator(2f))
                        .withEndAction {
                            if (!canAnimate()) return@withEndAction

                            // Pop the bubble out.
                            bubble.animate()
                                .scaleX(0.05f)
                                .scaleY(0.05f)
                                .alpha(0f)
                                .setDuration(150)
                                .withEndAction {
                                    if (!canAnimate()) {
                                        return@withEndAction
                                    }

                                    // Show the unrevealed card.
                                    bubble.setImageResource(
                                        R.drawable.mystery_card_back
                                    )

                                    bubble.alpha = 1f
                                    bubble.scaleX = 1f
                                    bubble.scaleY = 1f
                                    bubble.rotation = 0f
                                    bubble.rotationY = 0f

                                    // Give the card a short entrance.
                                    bubble.scaleX = 0.85f
                                    bubble.scaleY = 0.85f

                                    bubble.animate()
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .setDuration(180)
                                        .start()

                                    // Flip the card.
                                    bubble.postDelayed({
                                        if (!canAnimate()) {
                                            return@postDelayed
                                        }

                                        flipRewardCard(
                                            bubble = bubble,
                                            rewardTitle = rewardTitle,
                                            rewardValue = rewardValue,
                                            mysteryType = mysteryType,
                                            mysteryAmount = mysteryAmount
                                        )
                                    }, 220)
                                }
                                .start()
                        }
                        .start()
                }, 300)
            }, 650)
        }

        findViewById<Button>(R.id.playAgainButton)
            .setOnClickListener {
                startActivity(
                    Intent(this, GameActivity::class.java)
                )
                finish()
            }

        findViewById<Button>(R.id.homeButton)
            .setOnClickListener {
                goHome()
            }
    }


    private fun displayCompletedMissions(missionIds: List<String>) {
        val panel = findViewById<View>(R.id.completedMissionsPanel)
        val title = findViewById<ImageView>(R.id.gameOverTitle)
        val list = findViewById<LinearLayout>(R.id.completedMissionsList)

        if (missionIds.isEmpty()) {
            panel.visibility = View.GONE
            title.visibility = View.VISIBLE
            return
        }

        val missions = MissionSystem(
            app.prefs,
            app.playerData
        ).getMissions().filter { it.id in missionIds }

        if (missions.isEmpty()) {
            panel.visibility = View.GONE
            title.visibility = View.VISIBLE
            return
        }

        list.removeAllViews()

        val inflater = LayoutInflater.from(this)

        missions.forEach { mission ->
            val row = inflater.inflate(
                R.layout.item_completed_mission,
                list,
                false
            )

            row.findViewById<TextView>(
                R.id.completedMissionName
            ).text = mission.title

            row.findViewById<TextView>(
                R.id.completedMissionDescription
            ).text = mission.description

            row.findViewById<TextView>(
                R.id.completedMissionRewardHint
            ).text = "REWARD READY TO CLAIM"

            list.addView(row)
        }

        missions.forEach { mission ->
            val row = inflater.inflate(
                R.layout.item_completed_mission,
                list,
                false
            )

            row.findViewById<TextView>(
                R.id.completedMissionName
            ).text = mission.title

            row.findViewById<TextView>(
                R.id.completedMissionDescription
            ).text = mission.description

            row.findViewById<TextView>(
                R.id.completedMissionRewardHint
            ).text = "REWARD READY TO CLAIM"

            row.alpha = 0f
            row.translationX = 70f

            list.addView(row)

            row.animate()
                .alpha(1f)
                .translationX(0f)
                .setStartDelay(list.childCount * 130L)
                .setDuration(420L)
                .setInterpolator(OvershootInterpolator(1.1f))
                .start()
        }

        title.visibility = View.VISIBLE
        panel.visibility = View.VISIBLE
    }

    private fun animateEntrance(
        view: View,
        delay: Long,
        duration: Long,
        fromX: Float = 0f,
        fromY: Float = 0f,
        fromScale: Float = 1f,
        rotation: Float = 0f
    ) {
        view.animate().cancel()

        view.alpha = 0f
        view.translationX = fromX
        view.translationY = fromY
        view.scaleX = fromScale
        view.scaleY = fromScale
        view.rotation = rotation

        view.animate()
            .alpha(1f)
            .translationX(0f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .rotation(0f)
            .setStartDelay(delay)
            .setDuration(duration)
            .setInterpolator(OvershootInterpolator(1.15f))
            .start()
    }

    private fun flipRewardCard(
        bubble: ImageView,
        rewardTitle: TextView,
        rewardValue: TextView,
        mysteryType: String,
        mysteryAmount: Int
    ) {
        if (!canAnimate() || revealStarted) return
        revealStarted = true

        // Rotate the card to its edge.
        bubble.animate()
            .rotationY(90f)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                if (!canAnimate()) return@withEndAction

                // Swap the card back for the actual reward
                // while the image is edge-on.
                bubble.setImageResource(
                    getRewardDrawable(mysteryType)
                )

                bubble.rotationY = -90f

                // Reveal the front of the card.
                bubble.animate()
                    .rotationY(0f)
                    .setDuration(320)
                    .setInterpolator(
                        AccelerateDecelerateInterpolator()
                    )
                    .withEndAction {
                        if (!canAnimate()) return@withEndAction

                        playRewardReveal()

                        // Particle burst around the revealed card.
                        createParticleBurst(
                            findViewById(R.id.mysteryRewardCard),
                            mysteryType
                        )

                        // Reveal the reward title.
                        rewardTitle.alpha = 0f
                        rewardTitle.translationY = 12f

                        rewardTitle.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(280)
                            .start()

                        // Start the reward amount from zero.
                        rewardValue.text = when (mysteryType) {
                            "coins" -> "+0 COINS"
                            "rubies" -> "+0 RUBIES"
                            "ice_glide", "skate" -> "ICE GLIDE"
                            else -> "REWARD COLLECTED!"
                        }

                        rewardValue.alpha = 0f
                        rewardValue.scaleX = 0.65f
                        rewardValue.scaleY = 0.65f

                        rewardValue.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(300)
                            .setInterpolator(OvershootInterpolator(1.8f))
                            .withEndAction {
                                if (!canAnimate()) {
                                    return@withEndAction
                                }

                                // Count only currency rewards.
                                if (mysteryType == "coins" ||
                                    mysteryType == "rubies"
                                ) {
                                    animateNumber(
                                        0,
                                        mysteryAmount,
                                        1000
                                    ) { value ->
                                        rewardValue.text =
                                            if (mysteryType == "rubies") {
                                                "+$value RUBIES"
                                            } else {
                                                "+$value COINS"
                                            }
                                    }

                                    rewardValue.postDelayed({
                                        if (!canAnimate()) {
                                            return@postDelayed
                                        }
                                        rewardValue.animate()
                                            .scaleX(1.12f)
                                            .scaleY(1.12f)
                                            .setDuration(120)
                                            .withEndAction {
                                                rewardValue.animate()
                                                    .scaleX(1f)
                                                    .scaleY(1f)
                                                    .setDuration(130)
                                                    .start()

                                                rewardValue.postDelayed({
                                                    if (!canAnimate()) return@postDelayed

                                                    findViewById<View>(R.id.mysteryRewardCard)
                                                        .animate()
                                                        .alpha(0f)
                                                        .setStartDelay(500)
                                                        .setDuration(350)
                                                        .withEndAction {
                                                            findViewById<View>(R.id.mysteryRewardCard)
                                                                .visibility = View.GONE
                                                        }
                                                        .start()
                                                }, 1500)

                                            }
                                            .start()
                                    }, 1050)
                                }
                            }
                            .start()
                    }
                    .start()
            }
            .start()
    }

    private fun getRewardDrawable(type: String): Int {
        return when (type) {
            "rubies" -> R.drawable.reward_rubies
            "ice_glide", "skate" -> R.drawable.rush_title3
            else -> R.drawable.reward_coins
        }
    }

    /**
     * Small procedural sparkle burst.
     * The particles are temporary views; no particle image assets are needed.
     */
    private fun createParticleBurst(
        rewardCard: View,
        mysteryType: String
    ) {
        val root = findViewById<ViewGroup>(R.id.gameOverRoot)

        // Use a FrameLayout overlay so particles can move freely
        // without affecting the layout of other views.
        val overlay = root.findViewWithTag<FrameLayout>("rewardParticleOverlay")
            ?: FrameLayout(this).apply {
                tag = "rewardParticleOverlay"
                clipChildren = false
                clipToPadding = false

                root.addView(
                    this,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            }

        overlay.bringToFront()

        val density = resources.displayMetrics.density
        val particleSize = (21 * density).toInt()
        val random = kotlin.random.Random

        // Find the reward card's position relative to the overlay.
        val cardLocation = IntArray(2)
        val overlayLocation = IntArray(2)

        rewardCard.getLocationOnScreen(cardLocation)
        overlay.getLocationOnScreen(overlayLocation)

        val originX = cardLocation[0] - overlayLocation[0] +
                rewardCard.width * 0.5f
        val originY = cardLocation[1] - overlayLocation[1] +
                rewardCard.height * 0.5f

        val particleCount = 18

        repeat(particleCount) {
            val particle = ImageView(this).apply {
                setImageResource(R.drawable.rush_ruby)
                scaleType = ImageView.ScaleType.FIT_CENTER
                alpha = 1f
            }

            val params = FrameLayout.LayoutParams(
                particleSize,
                particleSize
            )

            overlay.addView(particle, params)

            particle.x = originX - particleSize / 2f
            particle.y = originY - particleSize / 2f

            // Fly diagonally toward the top-right.
            val dx = random.nextInt(100, 191) * density
            val dy = -random.nextInt(100, 191) * density

            particle.animate()
                .translationX(dx)
                .translationY(dy)
                .alpha(0f)
                .scaleX(0.25f)
                .scaleY(0.25f)
                .setStartDelay(random.nextLong(0, 101))
                .setDuration(random.nextLong(450, 751))
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .withEndAction {
                    (particle.parent as? ViewGroup)?.removeView(particle)
                }
                .start()
        }
    }

    private fun animateNumber(
        from: Int,
        to: Int,
        duration: Long,
        onUpdate: (Int) -> Unit
    ) {
        if (to <= 0) {
            onUpdate(to.coerceAtLeast(0))
            return
        }

        val animator = ValueAnimator.ofInt(from, to).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener {
                onUpdate(it.animatedValue as Int)
            }
        }

        runningAnimators.add(animator)
        animator.start()
    }

    private fun playBubblePop() {
        playSound(R.raw.mystery_bubble_pop, true)
    }

    private fun playRewardReveal() {
        playSound(R.raw.mystery_reward_reveal, false)
    }

    private fun playSound(resourceId: Int, isBubbleSound: Boolean) {
        if (!canAnimate()) return

        val previous = if (isBubbleSound) {
            bubblePopPlayer
        } else {
            rewardRevealPlayer
        }

        previous?.release()

        val player = MediaPlayer.create(this, resourceId)

        if (isBubbleSound) {
            bubblePopPlayer = player
        } else {
            rewardRevealPlayer = player
        }

        player?.setOnCompletionListener { completed ->
            completed.release()

            if (isBubbleSound && bubblePopPlayer === completed) {
                bubblePopPlayer = null
            }

            if (!isBubbleSound &&
                rewardRevealPlayer === completed
            ) {
                rewardRevealPlayer = null
            }
        }

        try {
            player?.start()
        } catch (_: IllegalStateException) {
            player?.release()

            if (isBubbleSound) {
                bubblePopPlayer = null
            } else {
                rewardRevealPlayer = null
            }
        }
    }

    private fun canAnimate(): Boolean {
        return !isFinishing && !isDestroyed
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onBackPressed() {
        goHome()
    }

    private fun goHome() {
        startActivity(
            Intent(this, MainMenuActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        finish()
    }

    override fun onDestroy() {
        runningAnimators.forEach { it.cancel() }
        runningAnimators.clear()

        bubblePopPlayer?.release()
        bubblePopPlayer = null

        rewardRevealPlayer?.release()
        rewardRevealPlayer = null

        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        app.bgmManager.playLobby()
    }
}
