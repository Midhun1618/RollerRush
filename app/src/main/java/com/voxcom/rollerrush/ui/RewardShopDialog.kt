
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
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
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

    // Google test rewarded ad unit.
    // Replace with production ID only after testing.
    private val rewardedAdUnitId = "ca-app-pub-5608962073439724/8749680876"

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
            "Watch a video to collect 1 Rush Ruby."
        } else {
            "Watch a video to collect 500 Coins."
        }

        watch.text = "WATCH AD"

        watch.background = activity.getDrawable(
            R.drawable.reward_shop_card2
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

            if (activity.isFinishing || activity.isDestroyed) {
                return@setOnClickListener
            }

            watch.isEnabled = false
            watch.text = "LOADING AD..."

            loadRewardedAd(
                isRuby = isRuby,
                confirmDialog = confirmDialog,
                watchButton = watch
            )
        }

        confirmDialog.show()

        setDialogWidth(confirmDialog, 0.60f)
    }

    private fun loadRewardedAd(
        isRuby: Boolean,
        confirmDialog: Dialog,
        watchButton: Button
    ) {

        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            activity,
            rewardedAdUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {

                override fun onAdLoaded(ad: RewardedAd) {

                    if (activity.isFinishing ||
                        activity.isDestroyed ||
                        !confirmDialog.isShowing
                    ) {
                        return
                    }

                    watchButton.isEnabled = true
                    watchButton.text = "WATCH AD"

                    var rewardEarned = false

                    ad.fullScreenContentCallback =
                        object : FullScreenContentCallback() {

                            override fun onAdDismissedFullScreenContent() {

                                confirmDialog.dismiss()

                                if (rewardEarned) {

                                    shopDialog?.dismiss()

                                    handler.postDelayed({

                                        if (!activity.isFinishing &&
                                            !activity.isDestroyed
                                        ) {
                                            RollerRushNotification.show(
                                                activity,
                                                if (isRuby)
                                                    "+1 Rush Ruby earned!"
                                                else
                                                    "+500 Rush Coins earned!",
                                                RollerRushNotification.Type.SUCCESS
                                            )
                                        }

                                    }, 500L)
                                }
                            }

                            override fun onAdFailedToShowFullScreenContent(
                                adError: AdError
                            ) {

                                confirmDialog.dismiss()

                                if (!activity.isFinishing &&
                                    !activity.isDestroyed
                                ) {
                                    RollerRushNotification.show(
                                        activity,
                                        "Unable to show ad. Try again.",
                                        RollerRushNotification.Type.WARNING
                                    )
                                }
                            }
                        }

                    ad.show(activity) {

                        // Google confirms that the user earned the reward.
                        rewardEarned = true

                        if (isRuby) {
                            playerData.addRushRubies(1)
                            setCooldown(RUBY_KEY)
                        } else {
                            playerData.addCoins(500)
                            setCooldown(COIN_KEY)
                        }

                        onBalancesChanged()
                    }
                }

                override fun onAdFailedToLoad(
                    adError: LoadAdError
                ) {

                    if (!activity.isFinishing &&
                        !activity.isDestroyed
                    ) {

                        watchButton.isEnabled = true
                        watchButton.text = "TRY AGAIN"

                        RollerRushNotification.show(
                            activity,
                            "Ad unavailable. Please try again.",
                            RollerRushNotification.Type.WARNING
                        )
                    }
                }
            }
        )
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
