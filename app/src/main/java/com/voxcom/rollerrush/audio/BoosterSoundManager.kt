
package com.voxcom.rollerrush.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.voxcom.rollerrush.R

class BoosterSoundManager(context: Context) {

    private val soundPool: SoundPool
    private val soundBySkate = mutableMapOf<String, Int>()
    private val loadedSounds = mutableSetOf<Int>()
    private val pendingPlays = mutableMapOf<Int, Int>()

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                synchronized(this) {
                    loadedSounds.add(sampleId)

                    val pending = pendingPlays.remove(sampleId) ?: 0
                    repeat(pending) {
                        playLoadedSound(sampleId)
                    }
                }
            }
        }

        val appContext = context.applicationContext

        soundBySkate["skate_basic"] =
            soundPool.load(appContext, R.raw.booster_inferno, 1)

        soundBySkate["skate_ice"] =
            soundPool.load(appContext, R.raw.booster_inferno, 1)

        soundBySkate["skate_aero"] =
            soundPool.load(appContext, R.raw.booster_inferno, 1)

        soundBySkate["skate_inferno"] =
            soundPool.load(appContext, R.raw.booster_inferno, 1)

        soundBySkate["skate_volt"] =
            soundPool.load(appContext, R.raw.booster_volt, 1)
    }

    @Synchronized
    fun playForSkate(skateId: String) {
        val soundId = soundBySkate[skateId]
            ?: soundBySkate["skate_basic"]
            ?: return

        if (soundId in loadedSounds) {
            playLoadedSound(soundId)
        } else {
            pendingPlays[soundId] =
                (pendingPlays[soundId] ?: 0) + 1
        }
    }

    private fun playLoadedSound(soundId: Int) {
        soundPool.play(
            soundId,
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
