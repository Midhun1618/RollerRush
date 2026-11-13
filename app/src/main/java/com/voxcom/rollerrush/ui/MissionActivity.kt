package com.voxcom.rollerrush.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
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

        findViewById<View>(R.id.backButton).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        refreshScreen()
    }

    private fun refreshScreen() {
        val player = app.playerData
        val careerScore = player.careerScore

        careerLabel.text = "CAREER SCORE: ${String.format("%,d", careerScore)}"
        levelLabel.text = "LEVEL ${player.level}"
        levelProgress.max = 1000
        levelProgress.progress = (player.levelProgress * 1000f).toInt()

        val nextLevel = player.scoreForNextLevel
        nextLevelLabel.text = if (nextLevel == null) {
            "MAX LEVEL REACHED"
        } else {
            "${String.format("%,d", careerScore)} / ${String.format("%,d", nextLevel)} XP"
        }

        val system = MissionSystem(app.prefs, player)
        val missions = system.getMissions().sortedWith(
            compareBy<MissionSystem.Mission>(
                { when {
                    it.readyToClaim -> 0
                    !it.locked && !it.completed -> 1
                    it.locked -> 2
                    else -> 3
                } },
                { it.unlockLevel },
                { it.category.ordinal },
                { it.title }
            )
        )

        missionList.removeAllViews()
        val inflater = LayoutInflater.from(this)

        missions.forEach { mission ->
            val card = inflater.inflate(R.layout.item_mission, missionList, false)
            val locked = mission.locked

            card.alpha = if (locked) 0.58f else 1f

            card.findViewById<TextView>(R.id.missionName).text = buildString {
                if (locked) append("🔒 ")
                append(mission.title)
            }

            card.findViewById<TextView>(R.id.missionDescription).text =
                if (locked) {
                    "Unlocks at Level ${mission.unlockLevel} • ${mission.category.label} • ${mission.description}"
                } else {
                    "${mission.category.label} • ${mission.description}"
                }

            val rewardText = when {
                mission.rewardCoins > 0 -> "+${mission.rewardCoins}"
                mission.rewardRubies > 0 -> "+${mission.rewardRubies}"
                else -> "REWARD"
            }

            card.findViewById<TextView>(R.id.missionReward).text = rewardText
            card.findViewById<ImageView>(R.id.missionRewardIcon).setImageResource(
                if (mission.rewardRubies > 0) R.drawable.rush_ruby else R.drawable.rush_coin
            )

            val progressBar = card.findViewById<ProgressBar>(R.id.missionProgressBar)
            progressBar.max = mission.target.coerceAtLeast(1)
            progressBar.progress = if (locked) 0 else mission.progress.coerceIn(0, progressBar.max)

            if (mission.completed) {
                progressBar.progressTintList = ColorStateList.valueOf(Color.rgb(82, 210, 139))
            } else if (locked) {
                progressBar.progressTintList = ColorStateList.valueOf(Color.rgb(105, 119, 137))
            }

            val progressText = card.findViewById<TextView>(R.id.missionProgressText)
            progressText.text = when {
                locked -> "LOCKED • REACH LEVEL ${mission.unlockLevel}"
                mission.rewardClaimed -> "COMPLETED • REWARD RECEIVED"
                mission.readyToClaim -> "COMPLETED • CLAIM YOUR REWARD"
                mission.completed -> "COMPLETED"
                else -> "${mission.progress.coerceAtMost(mission.target)} / ${mission.target}"
            }

            val claimButton = card.findViewById<Button>(R.id.missionClaimButton)
            when {
                mission.readyToClaim -> {
                    claimButton.visibility = View.VISIBLE
                    claimButton.isEnabled = true
                    claimButton.text = "CLAIM"
                    claimButton.backgroundTintList =
                        ColorStateList.valueOf(Color.rgb(40, 174, 103))
                    claimButton.setOnClickListener {
                        if (system.claimReward(mission.id)) refreshScreen()
                    }
                }

                mission.rewardClaimed -> {
                    claimButton.visibility = View.VISIBLE
                    claimButton.isEnabled = false
                    claimButton.text = "CLAIMED"
                    claimButton.backgroundTintList =
                        ColorStateList.valueOf(Color.rgb(75, 91, 105))
                }

                else -> claimButton.visibility = View.GONE
            }

            missionList.addView(card)
        }

        val pendingCount = system.getPendingClaimCount()
        val unlockedCount = system.getUnlockedMissionCount()
        val claimedCount = system.getMissions().count { it.rewardClaimed }

        missionStatus.text = when {
            pendingCount > 0 -> "$pendingCount reward(s) ready • $unlockedCount / 50 missions unlocked"
            else -> "$claimedCount rewards collected • $unlockedCount / 50 missions unlocked"
        }
    }

    override fun onStart() {
        super.onStart()
        app.bgmManager.playLobby()
    }
}
