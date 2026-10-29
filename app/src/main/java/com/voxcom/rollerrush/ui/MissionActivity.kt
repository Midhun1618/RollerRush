package com.voxcom.rollerrush.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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

        careerLabel.text = "CAREER SCORE: ${careerScore}"
        levelLabel.text = "LEVEL ${player.level}"

        levelProgress.progress = (player.levelProgress * 1000f).toInt()

        val nextLevel = player.scoreForNextLevel
        nextLevelLabel.text = if (nextLevel == null) {
            "MAX LEVEL REACHED"
        } else {
            "$careerScore / $nextLevel XP"
        }

        val missionSystem = MissionSystem(app.prefs, player)
        val missions = missionSystem.getMissions()

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
                    append("+${mission.rewardCoins} coins")
                }
                if (mission.rewardRubies > 0) {
                    if (isNotEmpty()) append("  ")
                    append("+${mission.rewardRubies} rubies")
                }
            }

            card.findViewById<TextView>(R.id.missionReward).text =
                if (mission.rewardGranted) "REWARDED" else rewardText

            val progressBar =
                card.findViewById<ProgressBar>(R.id.missionProgressBar)

            progressBar.max = mission.target.coerceAtLeast(1)
            progressBar.progress =
                mission.progress.coerceIn(0, progressBar.max)

            val progressText =
                card.findViewById<TextView>(R.id.missionProgressText)

            progressText.text = when {
                mission.rewardGranted ->
                    "COMPLETED • REWARD RECEIVED"
                mission.completed ->
                    "COMPLETED"
                else ->
                    "${mission.progress.coerceAtMost(mission.target)} / ${mission.target}"
            }

            if (mission.completed) {
                progressBar.progressTintList =
                    android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.rgb(82, 210, 139)
                    )
            }

            missionList.addView(card)
        }

        missionStatus.text =
            "${missions.count { it.rewardGranted }} / ${missions.size} rewards earned"
    }
}
