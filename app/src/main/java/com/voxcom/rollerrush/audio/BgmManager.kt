
package com.voxcom.rollerrush.audio

import android.content.Context
import android.media.MediaPlayer
import com.voxcom.rollerrush.R

class BgmManager(context: Context) {

    private val appContext = context.applicationContext

    private enum class Track {
        NONE,
        LOBBY,
        GAMEPLAY
    }

    private var lobbyPlayer: MediaPlayer? = null
    private var gameplayPlayer: MediaPlayer? = null
    private var currentTrack = Track.NONE

    @Synchronized
    private fun getPlayer(track: Track): MediaPlayer? {
        return when (track) {
            Track.LOBBY -> {
                if (lobbyPlayer == null) {
                    lobbyPlayer = createPlayer(R.raw.rollerrush_low_bgm)
                }
                lobbyPlayer
            }

            Track.GAMEPLAY -> {
                if (gameplayPlayer == null) {
                    gameplayPlayer = createPlayer(R.raw.rollerrush)
                }
                gameplayPlayer
            }

            Track.NONE -> null
        }
    }

    private fun createPlayer(resourceId: Int): MediaPlayer? {
        return MediaPlayer.create(appContext, resourceId)?.apply {
            isLooping = true
            setVolume(0.65f, 0.65f)
        }
    }

    @Synchronized
    private fun switchTo(track: Track) {
        if (currentTrack != track) {
            lobbyPlayer?.let {
                if (it.isPlaying) it.pause()
            }

            gameplayPlayer?.let {
                if (it.isPlaying) it.pause()
            }

            // Start a new track from its beginning when switching tracks.
            getPlayer(track)?.seekTo(0)
            currentTrack = track
        }

        getPlayer(track)?.let {
            if (!it.isPlaying) {
                it.start()
            }
        }
    }

    @Synchronized
    fun playLobby() {
        switchTo(Track.LOBBY)
    }

    @Synchronized
    fun playGameplay() {
        switchTo(Track.GAMEPLAY)
    }

    @Synchronized
    fun pause() {
        when (currentTrack) {
            Track.LOBBY -> lobbyPlayer
            Track.GAMEPLAY -> gameplayPlayer
            Track.NONE -> null
        }?.let {
            if (it.isPlaying) it.pause()
        }
    }

    @Synchronized
    fun resume() {
        when (currentTrack) {
            Track.LOBBY -> playLobby()
            Track.GAMEPLAY -> playGameplay()
            Track.NONE -> Unit
        }
    }

    @Synchronized
    fun release() {
        lobbyPlayer?.release()
        gameplayPlayer?.release()

        lobbyPlayer = null
        gameplayPlayer = null
        currentTrack = Track.NONE
    }
}
