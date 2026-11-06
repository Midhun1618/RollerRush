package com.voxcom.rollerrush.ui

import android.app.Activity
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import android.content.res.ColorStateList
import com.voxcom.rollerrush.R
import kotlin.math.roundToInt

object RollerRushNotification {

    enum class Type {
        SUCCESS,
        WARNING,
        INFO
    }

    private const val DISPLAY_DURATION = 2600L

    private val handler = Handler(Looper.getMainLooper())

    private var currentView: View? = null
    private var dismissRunnable: Runnable? = null

    fun show(
        activity: Activity,
        message: String,
        type: Type = Type.INFO
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        val root = activity.findViewById<ViewGroup>(android.R.id.content)
            ?: return

        // Cancel and remove the previous notification.
        dismissRunnable?.let {
            handler.removeCallbacks(it)
        }

        currentView?.animate()?.cancel()
        (currentView?.parent as? ViewGroup)?.removeView(currentView)
        currentView = null

        // Inflate the XML notification layout.
        val notification = LayoutInflater.from(activity).inflate(
            R.layout.view_roller_rush_notification,
            root,
            false
        )

        val icon = notification.findViewById<TextView>(
            R.id.notificationIcon
        )

        val messageText = notification.findViewById<TextView>(
            R.id.notificationMessage
        )

        // Configure notification type.
        val (symbol, accentColor) = when (type) {
            Type.SUCCESS -> Pair(
                "✓",
                Color.rgb(65, 220, 145)
            )

            Type.WARNING -> Pair(
                "!",
                Color.rgb(255, 190, 65)
            )

            Type.INFO -> Pair(
                "i",
                Color.rgb(75, 175, 255)
            )
        }

        icon.text = symbol
        icon.backgroundTintList = ColorStateList.valueOf(accentColor)

        messageText.text = message

        // Initial animation position.
        notification.alpha = 0f
        notification.translationY = -dp(activity, 110).toFloat()
        notification.elevation = dp(activity, 12).toFloat()

        notification.tag = "roller_rush_notification"

        // Position at the top of the screen.
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(activity, 92),
            Gravity.TOP
        ).apply {
            leftMargin = dp(activity, 12)
            rightMargin = dp(activity, 12)
            topMargin = dp(activity, 12)
        }

        root.addView(notification, params)

        currentView = notification

        // Slide down and fade in.
        notification.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Automatically dismiss.
        val dismiss = Runnable {
            if (notification.parent != null) {
                notification.animate()
                    .alpha(0f)
                    .translationY(-dp(activity, 110).toFloat())
                    .setDuration(300)
                    .withEndAction {
                        (notification.parent as? ViewGroup)
                            ?.removeView(notification)

                        if (currentView === notification) {
                            currentView = null
                        }
                    }
                    .start()
            }
        }

        dismissRunnable = dismiss

        handler.postDelayed(
            dismiss,
            DISPLAY_DURATION
        )
    }

    private fun dp(activity: Activity, value: Int): Int {
        return (
                value * activity.resources.displayMetrics.density
                ).roundToInt()
    }
}