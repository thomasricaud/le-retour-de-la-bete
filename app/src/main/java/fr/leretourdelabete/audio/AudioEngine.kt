package fr.leretourdelabete.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

data class PlaybackInfo(
    val available: Boolean,
    val durationMillis: Long,
)

@SuppressLint("DiscouragedApi")
class AudioEngine(
    private val context: Context,
    private val onFocusLost: () -> Unit,
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var foregroundPlayer: MediaPlayer? = null
    private var ambiencePlayer: MediaPlayer? = null
    private var ambienceResourceName: String? = null
    private var focusRequest: AudioFocusRequest? = null
    private var toneGenerator: ToneGenerator? = null

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (
            change == AudioManager.AUDIOFOCUS_LOSS ||
            change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
        ) {
            pause()
            onFocusLost()
        }
    }

    fun hasResource(resourceName: String): Boolean =
        context.resources.getIdentifier(resourceName, "raw", context.packageName) != 0

    fun playForeground(
        resourceName: String,
        looping: Boolean = false,
        seekMillis: Long = 0L,
    ): PlaybackInfo {
        stopForeground()
        return createPlayer(
            resourceName = resourceName,
            looping = looping,
            seekMillis = seekMillis,
            volume = FOREGROUND_VOLUME,
        ) { player ->
            foregroundPlayer = player
        }
    }

    fun playAmbience(resourceName: String): PlaybackInfo {
        val existingPlayer = ambiencePlayer
        if (ambienceResourceName == resourceName && existingPlayer != null) {
            requestAudioFocus()
            if (!existingPlayer.isPlaying) existingPlayer.start()
            return PlaybackInfo(true, existingPlayer.duration.toLong().coerceAtLeast(0L))
        }

        stopAmbience()
        return createPlayer(
            resourceName = resourceName,
            looping = true,
            seekMillis = 0L,
            volume = AMBIENCE_VOLUME,
        ) { player ->
            ambiencePlayer = player
            ambienceResourceName = resourceName
        }
    }

    private fun createPlayer(
        resourceName: String,
        looping: Boolean,
        seekMillis: Long,
        volume: Float,
        onCreated: (MediaPlayer) -> Unit,
    ): PlaybackInfo {
        val resourceId = context.resources.getIdentifier(
            resourceName,
            "raw",
            context.packageName,
        )
        if (resourceId == 0) return PlaybackInfo(false, 0L)

        requestAudioFocus()
        var candidatePlayer: MediaPlayer? = null
        return runCatching {
            context.resources.openRawResourceFd(resourceId).use { descriptor ->
                candidatePlayer = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(
                        descriptor.fileDescriptor,
                        descriptor.startOffset,
                        descriptor.length,
                    )
                    isLooping = looping
                    setVolume(volume, volume)
                    prepare()
                    if (seekMillis > 0L && duration > 0) {
                        seekTo(seekMillis.coerceAtMost(duration.toLong()).toInt())
                    }
                    start()
                }
            }
            val player = requireNotNull(candidatePlayer)
            onCreated(player)
            PlaybackInfo(true, player.duration.toLong().coerceAtLeast(0L))
        }.getOrElse {
            releasePlayer(candidatePlayer)
            abandonAudioFocusIfIdle()
            PlaybackInfo(false, 0L)
        }
    }

    fun pause() {
        foregroundPlayer?.takeIf { it.isPlaying }?.pause()
        ambiencePlayer?.takeIf { it.isPlaying }?.pause()
    }

    fun stopForeground() {
        releasePlayer(foregroundPlayer)
        foregroundPlayer = null
        abandonAudioFocusIfIdle()
    }

    fun stopAmbience() {
        releasePlayer(ambiencePlayer)
        ambiencePlayer = null
        ambienceResourceName = null
        abandonAudioFocusIfIdle()
    }

    fun stopAll() {
        releasePlayer(foregroundPlayer)
        releasePlayer(ambiencePlayer)
        foregroundPlayer = null
        ambiencePlayer = null
        ambienceResourceName = null
        abandonAudioFocus()
    }

    fun testSpeaker() {
        toneGenerator?.release()
        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80).also {
            it.startTone(ToneGenerator.TONE_PROP_BEEP2, 700)
        }
        handler.postDelayed({
            toneGenerator?.release()
            toneGenerator = null
        }, 800L)
    }

    fun release() {
        stopAll()
        toneGenerator?.release()
        toneGenerator = null
    }

    private fun requestAudioFocus() {
        if (focusRequest != null) return
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener(focusListener)
            .build()
            .also(audioManager::requestAudioFocus)
    }

    private fun abandonAudioFocus() {
        focusRequest?.let(audioManager::abandonAudioFocusRequest)
        focusRequest = null
    }

    private fun abandonAudioFocusIfIdle() {
        if (foregroundPlayer == null && ambiencePlayer == null) {
            abandonAudioFocus()
        }
    }

    private fun releasePlayer(player: MediaPlayer?) {
        player?.runCatching {
            stop()
            release()
        }
    }

    private companion object {
        const val FOREGROUND_VOLUME = 1.0f
        const val AMBIENCE_VOLUME = 0.28f
    }
}
