
package com.voxcom.rollerrush.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.voxcom.rollerrush.R

class GameSoundEffects(context: Context) {

    private val soundPool: SoundPool

    private val rubySoundId: Int
    private val purchaseSoundId: Int
    private val equipSoundId: Int
    private val hitSoundId: Int
    private val gotBoosterId: Int
    private val mysteryBubbleSoundId: Int

    private val soundBySkate = mutableMapOf<String, Int>()
    private val loadedSounds = mutableSetOf<Int>()
    private val pendingPlays = mutableMapOf<Int, Int>()

    private val lock = Any()

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
                val pendingCount = synchronized(lock) {
                    loadedSounds.add(sampleId)
                    pendingPlays.remove(sampleId) ?: 0
                }

                repeat(pendingCount) {
                    playLoadedSound(sampleId)
                }
            }
        }

        val appContext = context.applicationContext

        rubySoundId = soundPool.load(appContext, R.raw.rush_ruby, 1)
        purchaseSoundId = soundPool.load(appContext, R.raw.purchased, 1)
        equipSoundId = soundPool.load(appContext, R.raw.skate_equip, 1)
        hitSoundId = soundPool.load(appContext, R.raw.player_hit, 1)
        gotBoosterId = soundPool.load(appContext, R.raw.got_booster, 1)

        // Add app/src/main/res/raw/mystery_bubble.mp3 (or .wav/.ogg).
        mysteryBubbleSoundId =
            soundPool.load(appContext, R.raw.mystery_bubble, 1)

        // Booster activation sounds, previously managed separately.
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

    private fun play(soundId: Int) {
        val shouldPlayNow = synchronized(lock) {
            if (soundId in loadedSounds) {
                true
            } else {
                pendingPlays[soundId] =
                    (pendingPlays[soundId] ?: 0) + 1
                false
            }
        }

        if (shouldPlayNow) {
            playLoadedSound(soundId)
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

    fun playRubyCollected() = play(rubySoundId)

    fun playSkatePurchased() = play(purchaseSoundId)

    fun playObstacleHit() = play(hitSoundId)

    fun playSkateEquipped() = play(equipSoundId)

    fun playGotBooster() = play(gotBoosterId)

    fun playMysteryBubbleCollected() = play(mysteryBubbleSoundId)

    fun playBoosterForSkate(skateId: String) {
        val soundId = soundBySkate[skateId]
            ?: soundBySkate["skate_basic"]
            ?: return

        play(soundId)
    }

    fun release() {
        soundPool.setOnLoadCompleteListener(null)
        soundPool.release()

        synchronized(lock) {
            loadedSounds.clear()
            pendingPlays.clear()
            soundBySkate.clear()
        }
    }
}
