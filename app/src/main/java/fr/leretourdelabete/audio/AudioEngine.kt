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
    private var mediaPlayer: MediaPlayer? = null
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

    fun play(
        resourceName: String,
        looping: Boolean = false,
        seekMillis: Long = 0L,
    ): PlaybackInfo {
        stop()
        val resourceId = context.resources.getIdentifier(
            resourceName,
            "raw",
            context.packageName,
        )
        if (resourceId == 0) return PlaybackInfo(false, 0L)

        requestAudioFocus()
        return runCatching {
            val descriptor = context.resources.openRawResourceFd(resourceId)
            val player = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setDataSource(
                    descriptor.fileDescriptor,
                    descriptor.startOffset,
                    descriptor.length,
                )
                isLooping = looping
                prepare()
                if (seekMillis > 0L && duration > 0) {
                    seekTo(seekMillis.coerceAtMost(duration.toLong()).toInt())
                }
                start()
            }
            descriptor.close()
            mediaPlayer = player
            PlaybackInfo(true, player.duration.toLong().coerceAtLeast(0L))
        }.getOrElse {
            stop()
            PlaybackInfo(false, 0L)
        }
    }

    fun pause() {
        mediaPlayer?.takeIf { it.isPlaying }?.pause()
    }

    fun resume() {
        mediaPlayer?.start()
    }

    fun stop() {
        mediaPlayer?.runCatching {
            stop()
            release()
        }
        mediaPlayer = null
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
        stop()
        toneGenerator?.release()
        toneGenerator = null
    }

    private fun requestAudioFocus() {
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
}
