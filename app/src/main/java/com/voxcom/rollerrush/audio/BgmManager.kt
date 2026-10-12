package com.voxcom.rollerrush.audio

import android.content.Context
import android.media.MediaPlayer
import com.voxcom.rollerrush.R

class BgmManager(context: Context) {

    private val appContext = context.applicationContext
    private var mediaPlayer: MediaPlayer? = null
    private var shouldPlay = false

    fun start() {
        shouldPlay = true

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(
                appContext,
                R.raw.rollerrush
            )?.apply {
                isLooping = true
                setVolume(0.65f, 0.65f)
            }
        }

        if (shouldPlay && mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
        }
    }

    fun pause() {
        shouldPlay = false
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    fun resume() {
        if (mediaPlayer == null) {
            start()
            return
        }

        shouldPlay = true
        if (mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
        }
    }

    fun stop() {
        shouldPlay = false
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun release() {
        shouldPlay = false
        mediaPlayer?.release()
        mediaPlayer = null
    }
}