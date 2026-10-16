package com.ghostreborn.akira.features.home.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.ghostreborn.akira.features.home.models.MusicTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AudioPlaybackState(
    val currentTrack: MusicTrack? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val errorMessage: String? = null
)

object AudioPlayerManager {
    private const val TAG = "AudioPlayerManager"

    private var mediaPlayer: MediaPlayer? = null
    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    fun playTrack(context: Context, track: MusicTrack) {
        val url = track.audioUrl
        if (url.isNullOrBlank()) {
            _playbackState.value = _playbackState.value.copy(
                errorMessage = "Track audio stream unavailable"
            )
            return
        }

        // If same track is already playing, toggle pause
        if (_playbackState.value.currentTrack?.id == track.id && mediaPlayer != null) {
            if (mediaPlayer?.isPlaying == true) {
                pause()
            } else {
                resume()
            }
            return
        }

        releasePlayer()

        _playbackState.value = AudioPlaybackState(
            currentTrack = track,
            isPlaying = false,
            isLoading = true,
            currentPositionMs = 0,
            durationMs = track.durationSeconds * 1000
        )

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { mp ->
                    mp.start()
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = true,
                        isLoading = false,
                        durationMs = mp.duration
                    )
                }
                setOnCompletionListener {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        currentPositionMs = 0
                    )
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Audio play error: what=$what, extra=$extra")
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        isLoading = false,
                        errorMessage = "Playback failed"
                    )
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start player: ${e.message}")
            _playbackState.value = _playbackState.value.copy(
                isPlaying = false,
                isLoading = false,
                errorMessage = e.message
            )
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        currentPositionMs = it.currentPosition
                    )
                }
            }
        } catch (_: Exception) {}
    }

    fun resume() {
        try {
            mediaPlayer?.let {
                it.start()
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = true
                )
            }
        } catch (_: Exception) {}
    }

    fun stop() {
        releasePlayer()
        _playbackState.value = AudioPlaybackState()
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
            _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
        } catch (_: Exception) {}
    }

    private fun releasePlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }
}
