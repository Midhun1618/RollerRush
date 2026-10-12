package com.voxcom.rollerrush.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.voxcom.rollerrush.R

class CoinSoundManager(context: Context) {

    private val soundPool: SoundPool
    private val coinSoundId: Int
    private var soundLoaded = false
    private var pendingPlays = 0

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(32)
            .setAudioAttributes(attributes)
            .build()

        coinSoundId = soundPool.load(
            context.applicationContext,
            R.raw.coin_toss,
            1
        )

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (sampleId == coinSoundId && status == 0) {
                soundLoaded = true

                repeat(pendingPlays) {
                    playLoadedSound()
                }

                pendingPlays = 0
            }
        }
    }

    @Synchronized
    fun playCoin() {
        if (soundLoaded) {
            playLoadedSound()
        } else {
            pendingPlays++
        }
    }

    private fun playLoadedSound() {
        soundPool.play(
            coinSoundId,
            1f,
            1f,
            1,
            0,
            1f
        )
    }

    fun release() {
        soundPool.release()
    }
}