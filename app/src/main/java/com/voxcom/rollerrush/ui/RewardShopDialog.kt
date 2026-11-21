package com.voxcom.rollerrush.ui

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.voxcom.rollerrush.R
import com.voxcom.rollerrush.data.PlayerData
import java.util.concurrent.TimeUnit

class RewardShopDialog(
    private val activity: Activity,
    private val playerData: PlayerData,
    private val onBalancesChanged: () -> Unit
) {

    private val prefs = activity.getSharedPreferences(
        "reward_shop_cooldowns",
        Context.MODE_PRIVATE
    )

    private val handler = Handler(Looper.getMainLooper())

    private var shopDialog: Dialog? = null
    private var coinButton: Button? = null
    private var rubyButton: Button? = null
    private var ticker: Runnable? = null

    fun show() {
        if (activity.isFinishing || activity.isDestroyed) return

        val view = activity.layoutInflater.inflate(
            R.layout.dialog_reward_shop,
            null
        )

        coinButton = view.findViewById(R.id.rewardCoinsButton)
        rubyButton = view.findViewById(R.id.rewardRubyButton)

        shopDialog = Dialog(activity).apply {
            setContentView(view)
            setCancelable(true)
            window?.setBackgroundDrawableResource(
                android.R.color.transparent
            )
        }

        view.findViewById<TextView>(R.id.rewardShopClose)
            .setOnClickListener {
                shopDialog?.dismiss()
            }

        coinButton?.setOnClickListener {
            if (remaining(COIN_KEY) == 0L) {
                showConfirmation(false)
            }
        }

        rubyButton?.setOnClickListener {
            if (remaining(RUBY_KEY) == 0L) {
                showConfirmation(true)
            }
        }

        shopDialog?.setOnDismissListener {
            ticker?.let(handler::removeCallbacks)
            ticker = null
        }

        shopDialog?.show()

        setDialogWidth(shopDialog, 0.72f)

        refreshButtons()
        startTicker()
    }

    private fun showConfirmation(isRuby: Boolean) {
        if (activity.isFinishing || activity.isDestroyed) return

        val view = activity.layoutInflater.inflate(
            R.layout.dialog_reward_confirmation,
            null
        )

        val image = view.findViewById<ImageView>(
            R.id.rewardConfirmImage
        )

        val title = view.findViewById<TextView>(
            R.id.rewardConfirmTitle
        )

        val message = view.findViewById<TextView>(
            R.id.rewardConfirmMessage
        )

        val watch = view.findViewById<Button>(
            R.id.rewardConfirmWatchButton
        )

        image.setImageResource(
            if (isRuby) R.drawable.rush_ruby
            else R.drawable.rush_coin
        )

        title.text = if (isRuby) {
            "RUBY REWARD"
        } else {
            "COIN REWARD"
        }

        message.text = if (isRuby) {
            "Simulate watching a video to collect 1 Rush Ruby."
        } else {
            "Simulate watching a video to collect 500 Coins."
        }

        watch.text = "SIMULATE AD COMPLETION"

        watch.background = activity.getDrawable(
            if (isRuby) R.drawable.reward_shop_card2
            else R.drawable.reward_shop_card2
        )

        val confirmDialog = Dialog(activity).apply {
            setContentView(view)
            setCancelable(true)
            window?.setBackgroundDrawableResource(
                android.R.color.transparent
            )
        }

        view.findViewById<TextView>(R.id.rewardConfirmCancel)
            .setOnClickListener {
                confirmDialog.dismiss()
            }

        watch.setOnClickListener {

            // MOCK REWARD.
            // Later this will be replaced with AdMob's
            // onUserEarnedReward callback.

            if (isRuby) {
                playerData.addRushRubies(1)
                setCooldown(RUBY_KEY)
            } else {
                playerData.addCoins(500)
                setCooldown(COIN_KEY)
            }

            onBalancesChanged()

            confirmDialog.dismiss()
            refreshButtons()
        }

        confirmDialog.show()

        setDialogWidth(confirmDialog, 0.60f)
    }

    private fun setDialogWidth(
        dialog: Dialog?,
        widthFraction: Float
    ) {
        dialog?.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        dialog?.window?.setLayout(
            (activity.resources.displayMetrics.widthPixels *
                    widthFraction).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun refreshButtons() {
        val coinRemaining = remaining(COIN_KEY)
        val rubyRemaining = remaining(RUBY_KEY)

        coinButton?.apply {
            text = if (coinRemaining > 0) {
                "WAIT ${formatTime(coinRemaining)}"
            } else {
                "WATCH AD • 500 COINS"
            }

            isEnabled = coinRemaining == 0L
            alpha = if (isEnabled) 1f else 0.65f
        }

        rubyButton?.apply {
            text = if (rubyRemaining > 0) {
                "WAIT ${formatTime(rubyRemaining)}"
            } else {
                "WATCH AD • 1 RUBY"
            }

            isEnabled = rubyRemaining == 0L
            alpha = if (isEnabled) 1f else 0.65f
        }
    }

    private fun startTicker() {
        ticker?.let(handler::removeCallbacks)

        ticker = object : Runnable {
            override fun run() {
                if (shopDialog?.isShowing != true) return

                refreshButtons()
                handler.postDelayed(this, 1000L)
            }
        }

        handler.post(ticker!!)
    }

    private fun remaining(key: String): Long {
        val endTime = prefs.getLong(key, 0L)

        return (endTime - System.currentTimeMillis())
            .coerceAtLeast(0L)
    }

    private fun setCooldown(key: String) {
        prefs.edit()
            .putLong(
                key,
                System.currentTimeMillis() + COOLDOWN_MS
            )
            .apply()
    }

    private fun formatTime(ms: Long): String {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms)

        return String.format(
            "%02d:%02d",
            seconds / 60,
            seconds % 60
        )
    }

    companion object {
        private const val COIN_KEY =
            "coin_reward_cooldown_end"

        private const val RUBY_KEY =
            "ruby_reward_cooldown_end"

        private const val COOLDOWN_MS =
            3 * 60 * 1000L
    }
}