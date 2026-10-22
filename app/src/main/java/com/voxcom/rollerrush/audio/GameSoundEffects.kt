
package com.voxcom.rollerrush.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.voxcom.rollerrush.R

class GameSoundEffects(context: Context) {

    private val soundPool: SoundPool

    private val rubySoundId: Int
    private val purchaseSoundId: Int
    private val equipSoundId: Int
    private val hitSoundId: Int
    private val gotBoosterId: Int


    private val loadedSounds = mutableSetOf<Int>()

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                synchronized(loadedSounds) {
                    loadedSounds.add(sampleId)
                }
            }
        }

        rubySoundId = soundPool.load(context, R.raw.rush_ruby, 1)
        purchaseSoundId = soundPool.load(context, R.raw.purchased, 1)
        equipSoundId = soundPool.load(context, R.raw.skate_equip, 1)
        hitSoundId = soundPool.load(context, R.raw.player_hit, 1)
        gotBoosterId = soundPool.load(context, R.raw.got_booster, 1)
    }

    private fun play(soundId: Int) {
        val ready = synchronized(loadedSounds) {
            soundId in loadedSounds
        }

        if (ready) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun playRubyCollected() = play(rubySoundId)

    fun playSkatePurchased() = play(purchaseSoundId)

    fun playObstacleHit() = play(hitSoundId)

    fun playSkateEquipped() = play(equipSoundId)

    fun playGotBooster() = play(gotBoosterId)


    fun release() {
        soundPool.setOnLoadCompleteListener(null)
        soundPool.release()

        synchronized(loadedSounds) {
            loadedSounds.clear()
        }
    }
}
