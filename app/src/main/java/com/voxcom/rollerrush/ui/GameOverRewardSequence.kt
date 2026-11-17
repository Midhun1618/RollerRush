
package com.voxcom.rollerrush.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import com.voxcom.rollerrush.R
import com.voxcom.rollerrush.data.GamePreferences
import com.voxcom.rollerrush.data.InventoryData
import com.voxcom.rollerrush.data.MissionSystem
import com.voxcom.rollerrush.data.PlayerData
import com.voxcom.rollerrush.data.ShopCatalog
import com.voxcom.rollerrush.data.ShopItem
import com.voxcom.rollerrush.data.SkateStats
import java.util.Locale
import kotlin.math.roundToInt

class GameOverRewardSequence(
    private val activity: Activity,
    private val prefs: GamePreferences,
    private val playerData: PlayerData
) {

    private enum class RecommendationType {
        NEWLY_UNLOCKED,
        AFFORDABLE,
        SAVE_COINS,
        NEXT_LEVEL
    }

    private data class Recommendation(
        val type: RecommendationType,
        val skate: ShopItem? = null,
        val otherUnlocks: Int = 0
    )

    private val inventory = InventoryData(prefs)

    private lateinit var panel: View
    private lateinit var heading: TextView
    private lateinit var subtitle: TextView
    private lateinit var hint: TextView
    private lateinit var scroll: ScrollView
    private lateinit var list: LinearLayout

    // -------------------------------------------------------------------------
    // ENTRY POINT
    // -------------------------------------------------------------------------

    fun start(
        missionIds: List<String>,
        previousLevel: Int,
        newLevel: Int
    ) {
        panel = activity.findViewById(R.id.completedMissionsPanel)
        heading = activity.findViewById(R.id.completedMissionsTitle)
        subtitle = activity.findViewById(R.id.completedMissionsSubtitle)
        hint = activity.findViewById(R.id.completedMissionsHint)
        scroll = activity.findViewById(R.id.completedMissionsScroll)
        list = activity.findViewById(R.id.completedMissionsList)

        val completedMissions = MissionSystem(
            prefs,
            playerData
        ).getMissions().filter {
            it.id in missionIds
        }

        val recommendation = buildRecommendation(
            previousLevel,
            newLevel
        )

        if (completedMissions.isEmpty() && recommendation == null) {
            panel.visibility = View.GONE
            return
        }

        panel.visibility = View.VISIBLE
        panel.alpha = 0f
        panel.translationX = 65f
        panel.scaleX = 0.96f
        panel.scaleY = 0.96f

        panel.animate()
            .alpha(1f)
            .translationX(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(900L)
            .setDuration(450L)
            .setInterpolator(OvershootInterpolator(1.05f))
            .withEndAction {
                if (!canAnimate()) return@withEndAction

                if (completedMissions.isNotEmpty()) {
                    showCompletedMissions(
                        completedMissions,
                        recommendation
                    )
                } else if (recommendation != null) {
                    showRecommendation(recommendation)
                } else {
                    panel.visibility = View.GONE
                }
            }
            .start()
    }

    // -------------------------------------------------------------------------
    // COMPLETED MISSIONS
    // -------------------------------------------------------------------------

    private fun showCompletedMissions(
        missions: List<MissionSystem.Mission>,
        recommendation: Recommendation?
    ) {
        heading.text = "MISSIONS COMPLETED!"

        subtitle.text =
            "Amazing ride! You completed ${missions.size} mission(s)."

        hint.text =
            "Mission rewards can be claimed from the Missions screen."

        scroll.visibility = View.VISIBLE
        list.removeAllViews()
        scroll.scrollTo(0, 0)

        missions.forEachIndexed { index, mission ->

            val row = activity.layoutInflater.inflate(
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

            val rewardText = buildString {

                if (mission.rewardCoins > 0) {
                    append("+${mission.rewardCoins} COINS")
                }

                if (mission.rewardRubies > 0) {
                    if (isNotEmpty()) append("  •  ")
                    append("+${mission.rewardRubies} RUBIES")
                }

                if (isEmpty()) {
                    append("MISSION COMPLETED")
                }

                append("  •  CLAIM IN MISSIONS")
            }

            row.findViewById<TextView>(
                R.id.completedMissionRewardHint
            ).text = rewardText

            row.alpha = 0f
            row.translationX = 75f
            row.scaleX = 0.88f
            row.scaleY = 0.88f

            list.addView(row)

            row.animate()
                .alpha(1f)
                .translationX(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(index * 180L)
                .setDuration(430L)
                .setInterpolator(OvershootInterpolator(1.15f))
                .start()
        }

        val displayDuration =
            (missions.size - 1).coerceAtLeast(0) * 180L + 1500L

        panel.postDelayed({

            if (!canAnimate()) return@postDelayed

            transitionToRecommendation(recommendation)

        }, displayDuration)
    }

    // -------------------------------------------------------------------------
    // MISSION TO RECOMMENDATION TRANSITION
    // -------------------------------------------------------------------------

    private fun transitionToRecommendation(
        recommendation: Recommendation?
    ) {
        val childCount = list.childCount

        if (childCount == 0) {
            if (recommendation == null) {
                panel.visibility = View.GONE
            } else {
                showRecommendation(recommendation)
            }
            return
        }

        var completedExits = 0

        for (index in 0 until childCount) {

            val child = list.getChildAt(index)

            child.animate()
                .alpha(0f)
                .translationX(-45f)
                .scaleX(0.82f)
                .scaleY(0.82f)
                .setStartDelay(index * 65L)
                .setDuration(260L)
                .withEndAction {

                    completedExits++

                    if (completedExits == childCount) {

                        list.removeAllViews()

                        if (recommendation == null) {

                            panel.animate()
                                .alpha(0f)
                                .setDuration(350L)
                                .withEndAction {
                                    panel.visibility = View.GONE
                                }
                                .start()

                        } else {

                            playTransitionSweep {
                                showRecommendation(recommendation)
                            }
                        }
                    }
                }
                .start()
        }
    }

    private fun playTransitionSweep(
        onFinished: () -> Unit
    ) {
        val sweep = View(activity).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.rgb(70, 205, 255),
                    Color.WHITE,
                    Color.rgb(110, 115, 255),
                    Color.TRANSPARENT
                )
            )
            alpha = 0f
        }

        list.addView(
            sweep,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(5)
            )
        )

        sweep.animate()
            .alpha(1f)
            .translationX(dp(15).toFloat())
            .setDuration(180L)
            .withEndAction {

                sweep.animate()
                    .alpha(0f)
                    .translationX(-dp(15).toFloat())
                    .setDuration(220L)
                    .withEndAction {
                        list.removeView(sweep)
                        onFinished()
                    }
                    .start()
            }
            .start()
    }

    // -------------------------------------------------------------------------
    // RECOMMENDATION LOGIC
    // -------------------------------------------------------------------------

    private fun buildRecommendation(
        previousLevel: Int,
        newLevel: Int
    ): Recommendation? {

        // Priority 1: newly unlocked skates.

        val newlyUnlocked = if (newLevel > previousLevel) {

            ShopCatalog.allSkates().filter {
                it.unlockLevel in (previousLevel + 1)..newLevel &&
                        !inventory.owns(it.id)
            }

        } else {
            emptyList()
        }

        val bestNewUnlock = bestSkate(newlyUnlocked)

        if (bestNewUnlock != null) {
            return Recommendation(
                type = RecommendationType.NEWLY_UNLOCKED,
                skate = bestNewUnlock,
                otherUnlocks = (newlyUnlocked.size - 1).coerceAtLeast(0)
            )
        }

        // Only recommend level-unlocked and unowned skates.

        val availableSkates = ShopCatalog.allSkates().filter {
            playerData.level >= it.unlockLevel &&
                    !inventory.owns(it.id) &&
                    it.price > 0
        }

        val currentStats = SkateStats.forSkate(
            playerData.equippedSkateId
        )

        val betterSkates = availableSkates.filter {
            isBetterSkate(
                SkateStats.forSkate(it.id),
                currentStats
            )
        }

        // Priority 2: affordable upgrades.

        val affordable = betterSkates.filter {
            it.price <= playerData.coins
        }

        val bestAffordable = bestSkate(affordable)

        if (bestAffordable != null) {
            return Recommendation(
                type = RecommendationType.AFFORDABLE,
                skate = bestAffordable
            )
        }

        // Priority 3: best upgrade to save for.

        val bestUnaffordable = bestSkate(betterSkates)

        if (bestUnaffordable != null) {
            return Recommendation(
                type = RecommendationType.SAVE_COINS,
                skate = bestUnaffordable
            )
        }

        // Priority 4: next-level progress.

        if (playerData.scoreForNextLevel != null) {
            return Recommendation(
                type = RecommendationType.NEXT_LEVEL
            )
        }

        return null
    }

    private fun bestSkate(
        items: List<ShopItem>
    ): ShopItem? {

        return items.maxWithOrNull(
            compareBy<ShopItem> {
                SkateStats.forSkate(it.id).overallRating
            }.thenBy {
                SkateStats.forSkate(it.id).speedRating
            }.thenBy {
                SkateStats.forSkate(it.id).jumpRating
            }.thenBy {
                it.price
            }
        )
    }

    private fun isBetterSkate(
        candidate: SkateStats,
        current: SkateStats
    ): Boolean {

        return candidate.overallRating > current.overallRating ||
                candidate.speedMultiplier > current.speedMultiplier ||
                candidate.jumpHeight > current.jumpHeight ||
                candidate.controlRating > current.controlRating
    }

    // -------------------------------------------------------------------------
    // RECOMMENDATION DISPLAY
    // -------------------------------------------------------------------------

    private fun showRecommendation(
        recommendation: Recommendation
    ) {
        list.removeAllViews()
        scroll.visibility = View.VISIBLE

        when (recommendation.type) {

            RecommendationType.NEWLY_UNLOCKED -> {

                heading.text = "NEW SKATE AVAILABLE!"

                subtitle.text =
                    "A new ride has entered your collection."

                hint.text = if (recommendation.otherUnlocks > 0) {
                    "+${recommendation.otherUnlocks} more skate(s) also unlocked!"
                } else {
                    "Your next upgrade is waiting in the shop."
                }

                recommendation.skate?.let {
                    showSkate(it, newlyUnlocked = true)
                }
            }

            RecommendationType.AFFORDABLE -> {

                heading.text = "UPGRADE YOUR RIDE!"

                subtitle.text =
                    "You have enough coins for a better skate."

                hint.text =
                    "Visit the shop and choose your next ride."

                recommendation.skate?.let {
                    showSkate(it, newlyUnlocked = false)
                }
            }

            RecommendationType.SAVE_COINS -> {

                heading.text =
                    "YOUR NEXT UPGRADE IS CLOSE!"

                subtitle.text =
                    "Keep collecting coins to get this skate."

                hint.text =
                    "Every run brings you closer."

                recommendation.skate?.let {
                    showSkate(it, newlyUnlocked = false)
                }
            }

            RecommendationType.NEXT_LEVEL -> {

                heading.text = "KEEP ROLLING!"

                subtitle.text =
                    "Your next career level is waiting."

                hint.text =
                    "Earn more score to unlock new rewards."

                showNextLevelHint()
            }
        }

        list.alpha = 0f
        list.translationY = 18f
        list.scaleX = 0.94f
        list.scaleY = 0.94f

        list.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500L)
            .setInterpolator(OvershootInterpolator(1.05f))
            .start()
    }

    // -------------------------------------------------------------------------
    // CUSTOM XML: SKATE RECOMMENDATION
    // -------------------------------------------------------------------------

    private fun showSkate(
        item: ShopItem,
        newlyUnlocked: Boolean
    ) {
        val root = activity.layoutInflater.inflate(
            R.layout.item_game_over_recommendation,
            list,
            false
        )

        val skateGroup = root.findViewById<View>(
            R.id.skateRecommendationGroup
        )

        val nextLevelGroup = root.findViewById<View>(
            R.id.nextLevelRecommendationGroup
        )

        skateGroup.visibility = View.VISIBLE
        nextLevelGroup.visibility = View.GONE

        val skateImage = root.findViewById<ImageView>(
            R.id.recommendationSkateImage
        )

        val skateName = root.findViewById<TextView>(
            R.id.recommendationSkateName
        )

        val unlockLevel = root.findViewById<TextView>(
            R.id.recommendationUnlockLevel
        )

        val price = root.findViewById<TextView>(
            R.id.recommendationPrice
        )

        val statsLabel = root.findViewById<TextView>(
            R.id.recommendationStats
        )

        val status = root.findViewById<TextView>(
            R.id.recommendationStatus
        )

        val coinProgress = root.findViewById<ProgressBar>(
            R.id.recommendationCoinProgress
        )

        val shopButton = root.findViewById<Button>(
            R.id.recommendationShopButton
        )

        val glow = root.findViewById<View>(
            R.id.recommendationGlow
        )

        // -------------------------------------------------------------
        // SKATE INFORMATION
        // -------------------------------------------------------------

        val stats = SkateStats.forSkate(item.id)

        val currentStats = SkateStats.forSkate(
            playerData.equippedSkateId
        )

        skateImage.setImageResource(
            getSkateDrawable(item)
        )

        skateImage.contentDescription = item.name

        skateName.text = item.name.uppercase(Locale.US)

        unlockLevel.text =
            "UNLOCKED AT LEVEL ${item.unlockLevel}"

        price.text =
            "${formatCoins(item.price)} COINS"

        // -------------------------------------------------------------
        // DISPLAY ONLY THE BEST STAT IMPROVEMENT
        // -------------------------------------------------------------

        val improvements = listOf(
            "SPEED" to percentageGain(
                stats.speedMultiplier,
                currentStats.speedMultiplier
            ),

            "JUMP" to percentageGain(
                stats.jumpHeight,
                currentStats.jumpHeight
            ),

            "CONTROL" to percentageGain(
                stats.controlRating.toFloat(),
                currentStats.controlRating.toFloat()
            )
        )

        val bestImprovement = improvements.maxByOrNull {
            it.second
        }

        statsLabel.text = if (
            bestImprovement != null &&
            bestImprovement.second > 0
        ) {
            "BEST UPGRADE: ${bestImprovement.first} +${bestImprovement.second}%"
        } else {
            "YOUR NEXT SKATE UPGRADE"
        }

        // -------------------------------------------------------------
        // COIN STATUS AND PROGRESS
        // -------------------------------------------------------------

        val shortfall = (
                item.price - playerData.coins
                ).coerceAtLeast(0)

        status.text = when {

            newlyUnlocked && shortfall == 0 ->
                "NEWLY UNLOCKED — BUY IT NOW!"

            newlyUnlocked ->
                "${formatCoins(shortfall)} MORE COINS NEEDED"

            shortfall == 0 ->
                "YOU CAN BUY THIS SKATE NOW!"

            else ->
                "ONLY ${formatCoins(shortfall)} MORE COINS NEEDED"
        }

        status.setTextColor(
            if (shortfall == 0) {
                Color.rgb(75, 220, 145)
            } else {
                Color.rgb(255, 205, 90)
            }
        )

        coinProgress.max = item.price.coerceAtLeast(1)

        coinProgress.progress = playerData.coins.coerceIn(
            0,
            coinProgress.max
        )

        coinProgress.visibility = if (shortfall > 0) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // -------------------------------------------------------------
        // SHOP BUTTON
        // -------------------------------------------------------------

        shopButton.setOnClickListener {

            activity.startActivity(
                Intent(activity, ShopActivity::class.java)
            )
        }

        // -------------------------------------------------------------
        // ADD CUSTOM XML TO THE EXISTING LIST
        // -------------------------------------------------------------

        list.addView(
            root,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // -------------------------------------------------------------
        // GLOW EFFECT
        // -------------------------------------------------------------

        glow.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL

            setColor(
                Color.argb(45, 80, 180, 255)
            )

            setStroke(
                dp(2),
                Color.argb(130, 80, 205, 255)
            )
        }

        val glowAnimator = ObjectAnimator.ofFloat(
            glow,
            View.ALPHA,
            0.4f,
            0.9f
        ).apply {
            duration = 1100L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }

        glow.addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {

                override fun onViewAttachedToWindow(v: View) = Unit

                override fun onViewDetachedFromWindow(v: View) {
                    glowAnimator.cancel()
                }
            }
        )

        // -------------------------------------------------------------
        // SKATE ENTRANCE ANIMATION
        // -------------------------------------------------------------

        skateImage.alpha = 0f
        skateImage.scaleX = 0.45f
        skateImage.scaleY = 0.45f
        skateImage.rotation = -12f

        skateImage.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .rotation(0f)
            .setDuration(700L)
            .setInterpolator(OvershootInterpolator(1.4f))
            .start()
    }

    // -------------------------------------------------------------------------
    // CUSTOM XML: NEXT LEVEL RECOMMENDATION
    // -------------------------------------------------------------------------

    private fun showNextLevelHint() {

        val root = activity.layoutInflater.inflate(
            R.layout.item_game_over_recommendation,
            list,
            false
        )

        root.findViewById<View>(
            R.id.skateRecommendationGroup
        ).visibility = View.GONE

        val nextGroup = root.findViewById<View>(
            R.id.nextLevelRecommendationGroup
        )

        nextGroup.visibility = View.VISIBLE

        val title = root.findViewById<TextView>(
            R.id.nextLevelTitle
        )

        val remainingText = root.findViewById<TextView>(
            R.id.nextLevelRemaining
        )

        val progress = root.findViewById<ProgressBar>(
            R.id.nextLevelProgress
        )

        val progressText = root.findViewById<TextView>(
            R.id.nextLevelProgressText
        )

        // -------------------------------------------------------------
        // LEVEL INFORMATION
        // -------------------------------------------------------------

        val currentLevel = playerData.level
        val nextLevel = currentLevel + 1

        val nextThreshold = playerData.scoreForNextLevel ?: return

        val currentScore = playerData.careerScore

        val startScore = playerData.levelStartScore(
            currentLevel
        )

        val remaining = (
                nextThreshold - currentScore
                ).coerceAtLeast(0)

        val range = (
                nextThreshold - startScore
                ).coerceAtLeast(1)

        val earned = (
                currentScore - startScore
                ).coerceAtLeast(0)

        title.text = "LEVEL $nextLevel"

        remainingText.text =
            "${formatCoins(remaining)} MORE XP TO GO"

        progress.max = range

        progress.progress = earned.coerceIn(
            0,
            range
        )

        progressText.text =
            "${formatCoins(currentScore)} / ${formatCoins(nextThreshold)} XP"

        // -------------------------------------------------------------
        // ADD CUSTOM XML TO THE LIST
        // -------------------------------------------------------------

        list.addView(
            root,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // -------------------------------------------------------------
        // PROGRESSION CARD ANIMATION
        // -------------------------------------------------------------

        nextGroup.alpha = 0f
        nextGroup.translationY = 25f
        nextGroup.scaleX = 0.92f
        nextGroup.scaleY = 0.92f

        nextGroup.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(550L)
            .setInterpolator(OvershootInterpolator(1.1f))
            .start()
    }

    // -------------------------------------------------------------------------
    // SKATE DRAWABLE MAPPING
    // -------------------------------------------------------------------------

    private fun getSkateDrawable(
        item: ShopItem
    ): Int {

        val aliases = mapOf(
            "skate_basic" to "skate1",
            "skate_ice" to "skate2",
            "skate_aero" to "skate3",
            "skate_inferno" to "skate4",
            "skate_volt" to "skate5",
            "skate_sunset_rush" to "skate_sunset_rush",
            "skate_pixel_pop" to "skate_pixel_pop",
            "skate_night_shift" to "skate_void_reaper",
            "skate_nitro_edge" to "skate_nitro_edge",
            "skate_sonic_wave" to "skate_sonic_wave",
            "skate_gold_standard" to "skate_gold_standard",
            "skate_drift_king" to "skate_cross_rider",
            "skate_cross_rider" to "skate_cross_rider",
            "skate_street_tox" to "skate_street_tox",
            "skate_thunderbolt" to "skate_thunderbolt",
            "skate_titanium_x" to "skate_titanium",
            "skate_neon_phantom" to "skate_sonic_wave",
            "skate_skull_ride" to "skate_skull_ride",
            "skate_cosmic_drift" to "skate_cosmic_drift",
            "skate_solar_flare" to "skate_solar_flare",
            "skate_void_reaper" to "skate_void_reaper",
            "skate_infinity" to "skate_infinity"
        )

        val drawableName = aliases[item.id]
            ?: item.assetName
                .substringAfterLast('/')
                .substringBeforeLast('.')

        val resourceId = activity.resources.getIdentifier(
            drawableName,
            "drawable",
            activity.packageName
        )

        return if (resourceId != 0) {
            resourceId
        } else {
            R.drawable.skate1
        }
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private fun percentageGain(
        newValue: Float,
        currentValue: Float
    ): Int {

        if (currentValue <= 0f) return 0

        return (
                ((newValue / currentValue) - 1f) * 100f
                ).roundToInt()
    }

    private fun formatCoins(
        value: Int
    ): String {
        return String.format(
            Locale.US,
            "%,d",
            value
        )
    }

    private fun dp(
        value: Int
    ): Int {
        return (
                value * activity.resources.displayMetrics.density
                ).roundToInt()
    }

    private fun canAnimate(): Boolean {
        return !activity.isFinishing && !activity.isDestroyed
    }
}
