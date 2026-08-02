package com.direader.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

/**
 * Wrapper around [AudioTrack] for playing TTS audio.
 */
class AudioPlayer {
    private var audioTrack: AudioTrack? = null
    private val sampleRate = 24000
    private var _isPlaying = false
    
    val isPlaying: Boolean
        get() = _isPlaying

    var onPlaybackComplete: (() -> Unit)? = null

    /**
     * Initializes the audio player.
     */
    fun initialize() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        val bufferSize = minBufferSize * 2

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    /**
     * Plays the given PCM data. Blocking operation until data is written.
     */
    fun play(pcmData: ShortArray) {
        val track = audioTrack ?: throw IllegalStateException("AudioPlayer is not initialized")
        
        if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
            track.play()
            _isPlaying = true
        }
        
        track.write(pcmData, 0, pcmData.size)
        
        // Simple callback trigger.
        onPlaybackComplete?.invoke()
    }

    /**
     * Pauses playback.
     */
    fun pause() {
        audioTrack?.pause()
        _isPlaying = false
    }

    /**
     * Resumes playback.
     */
    fun resume() {
        audioTrack?.play()
        _isPlaying = true
    }

    /**
     * Stops playback and flushes the buffer.
     */
    fun stop() {
        audioTrack?.let {
            if (it.playState != AudioTrack.PLAYSTATE_STOPPED) {
                it.stop()
            }
            it.flush()
        }
        _isPlaying = false
    }

    /**
     * Releases resources used by the player.
     */
    fun release() {
        stop()
        audioTrack?.release()
        audioTrack = null
    }
}
