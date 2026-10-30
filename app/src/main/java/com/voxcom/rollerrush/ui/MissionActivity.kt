
package com.voxcom.rollerrush.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.voxcom.rollerrush.R
import com.voxcom.rollerrush.data.MissionSystem

class MissionActivity : BaseActivity() {

    private lateinit var careerLabel: TextView
    private lateinit var levelLabel: TextView
    private lateinit var levelProgress: ProgressBar
    private lateinit var nextLevelLabel: TextView
    private lateinit var missionList: LinearLayout
    private lateinit var missionStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mission)

        careerLabel = findViewById(R.id.careerLabel)
        levelLabel = findViewById(R.id.levelLabel)
        levelProgress = findViewById(R.id.levelProgress)
        nextLevelLabel = findViewById(R.id.nextLevelLabel)
        missionList = findViewById(R.id.missionList)
        missionStatus = findViewById(R.id.missionStatus)

        findViewById<View>(R.id.backButton).setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshScreen()
    }

    private fun refreshScreen() {
        val player = app.playerData
        val careerScore = player.careerScore

        careerLabel.text = "CAREER SCORE: $careerScore"
        levelLabel.text = "LEVEL ${player.level}"
        levelProgress.progress =
            (player.levelProgress * 1000f).toInt()

        val nextLevel = player.scoreForNextLevel
        nextLevelLabel.text = if (nextLevel == null) {
            "MAX LEVEL REACHED"
        } else {
            "$careerScore / $nextLevel XP"
        }

        val system = MissionSystem(app.prefs, player)

        // Pending rewards first, unfinished missions second,
        // already claimed missions last.
        val missions = system.getMissions().sortedWith(
            compareBy(
                { if (it.readyToClaim) 0 else if (!it.completed) 1 else 2 },
                { it.title }
            )
        )

        missionList.removeAllViews()
        val inflater = LayoutInflater.from(this)

        missions.forEach { mission ->
            val card = inflater.inflate(
                R.layout.item_mission,
                missionList,
                false
            )

            card.findViewById<TextView>(R.id.missionName).text =
                mission.title

            card.findViewById<TextView>(R.id.missionDescription).text =
                mission.description

            val rewardText = buildString {
                if (mission.rewardCoins > 0) {
                    append("+${mission.rewardCoins}")
                }
                if (mission.rewardRubies > 0) {
                    if (isNotEmpty()) append("  ")
                    append("+${mission.rewardRubies}")
                }
            }

            card.findViewById<TextView>(R.id.missionReward).text =
                when {
                    mission.rewardClaimed -> "REWARDED"
                    mission.readyToClaim -> "READY"
                    else -> rewardText
                }

            val progressBar =
                card.findViewById<ProgressBar>(R.id.missionProgressBar)

            progressBar.max = mission.target.coerceAtLeast(1)
            progressBar.progress =
                mission.progress.coerceIn(0, progressBar.max)

            if (mission.completed) {
                progressBar.progressTintList = ColorStateList.valueOf(
                    Color.rgb(82, 210, 139)
                )
            }

            val progressText =
                card.findViewById<TextView>(R.id.missionProgressText)

            progressText.text = when {
                mission.rewardClaimed -> "COMPLETED • REWARD RECEIVED"
                mission.readyToClaim -> "COMPLETED • CLAIM YOUR REWARD"
                mission.completed -> "COMPLETED"
                else ->
                    "${mission.progress.coerceAtMost(mission.target)} / ${mission.target}"
            }

            val claimButton =
                card.findViewById<Button>(R.id.missionClaimButton)

            when {
                mission.readyToClaim -> {
                    claimButton.visibility = View.VISIBLE
                    claimButton.isEnabled = true
                    claimButton.text = "CLAIM"
                    claimButton.backgroundTintList =
                        ColorStateList.valueOf(Color.rgb(40, 174, 103))

                    claimButton.setOnClickListener {
                        val claimed = system.claimReward(mission.id)
                        if (claimed) {
                            refreshScreen()
                        }
                    }
                }

                mission.rewardClaimed -> {
                    claimButton.visibility = View.VISIBLE
                    claimButton.isEnabled = false
                    claimButton.text = "CLAIMED"
                    claimButton.backgroundTintList =
                        ColorStateList.valueOf(Color.rgb(75, 91, 105))
                }

                else -> {
                    claimButton.visibility = View.GONE
                }
            }

            missionList.addView(card)
        }

        val pendingCount = system.getPendingClaimCount()
        val claimedCount = system.getMissions().count { it.rewardClaimed }

        missionStatus.text = when {
            pendingCount > 0 ->
                "$pendingCount reward(s) ready to claim"
            else ->
                "$claimedCount / ${missions.size} rewards collected"
        }
    }
    override fun onStart() {
        super.onStart()
        app.bgmManager.playLobby()
    }
}
