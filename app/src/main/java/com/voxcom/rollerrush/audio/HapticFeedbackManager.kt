
package com.voxcom.rollerrush.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Handler
import android.os.Looper
import kotlin.random.Random

class HapticFeedbackManager(context: Context) {

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private val handler = Handler(Looper.getMainLooper())
    private var boosterActive = false

    fun obstacleHit() {
        stopBoosterPulse()
        vibrate(longArrayOf(0, 75, 35, 120))
    }

    fun boosterActivated() {
        stopBoosterPulse()
        val deviceVibrator = vibrator ?: return
        if (!deviceVibrator.hasVibrator()) return

        boosterActive = true
        val endTime = System.currentTimeMillis() + 2000L

        fun pulse() {
            if (!boosterActive || System.currentTimeMillis() >= endTime) {
                boosterActive = false
                return
            }

            // Randomized short pulses with varied intensity.
            val duration = Random.nextLong(18L, 46L)
            val amplitude = Random.nextInt(100, 256)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                deviceVibrator.vibrate(
                    VibrationEffect.createOneShot(
                        duration,
                        amplitude
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                deviceVibrator.vibrate(duration)
            }

            // Irregular rhythm: a changing pause between each pulse.
            val interval = Random.nextLong(65L, 190L)
            handler.postDelayed({ pulse() }, interval)
        }

        pulse()
    }

    fun stopBoosterPulse() {
        boosterActive = false
        handler.removeCallbacksAndMessages(null)
        vibrator?.cancel()
    }

    private fun vibrate(pattern: LongArray) {
        val deviceVibrator = vibrator ?: return
        if (!deviceVibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            deviceVibrator.vibrate(
                VibrationEffect.createWaveform(pattern, -1)
            )
        } else {
            @Suppress("DEPRECATION")
            deviceVibrator.vibrate(pattern, -1)
        }
    }
}
