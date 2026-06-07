package com.example.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object SoundEngine {
    private const val SAMPLE_RATE = 22050
    private var isMuted = false
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.createAttributionContext("audio")
        } else {
            context.applicationContext
        }
    }

    fun setMute(muted: Boolean) {
        isMuted = muted
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        return isMuted
    }

    fun isMuted(): Boolean = isMuted

    // Generate tick audio buffer: a rapid frequency decay for a satisfying "woodblock click" feel
    private val tickBuffer: ShortArray by lazy {
        val durationMs = 25
        val numSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // Slide pitch down quickly from 1000Hz to 60Hz
            val progress = i.toDouble() / numSamples
            val freq = 1000.0 - (940.0 * progress)
            val envelope = kotlin.math.exp(-8.0 * progress)
            samples[i] = (sin(2.0 * Math.PI * freq * t) * 32767.0 * envelope * 0.9).toInt().toShort()
        }
        samples
    }

    // Generate ding audio buffer: bright playful arcade-like ascending bell chord
    private val dingBuffer: ShortArray by lazy {
        val durationMs = 450
        val numSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(numSamples)
        
        // Let's create a sweet, bright, cheerful chime sound blending major harmonics
        // Playful frequencies: E5 (659.25Hz), G#5 (830.61Hz), B5 (987.77Hz), E6 (1318.51Hz)
        val freqs = doubleArrayOf(659.25, 830.61, 987.77, 1318.51)
        val weights = doubleArrayOf(0.25, 0.25, 0.25, 0.25)
        
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples
            
            // Fast attack, slow decay envelope
            val envelope = if (progress < 0.05) {
                progress / 0.05
            } else {
                kotlin.math.exp(-5.0 * (progress - 0.05))
            }
            
            var mixedValue = 0.0
            for (fIdx in freqs.indices) {
                mixedValue += sin(2.0 * Math.PI * freqs[fIdx] * t) * weights[fIdx]
            }
            // Add a little dynamic vibrato
            val vibrato = 1.0 + 0.03 * sin(2.0 * Math.PI * 12.0 * t)
            
            samples[i] = (mixedValue * 32767.0 * envelope * 0.85 * vibrato).toInt().toShort()
        }
        samples
    }

    private val audioScope = CoroutineScope(Dispatchers.Default)

    fun playTick() {
        if (isMuted) return
        audioScope.launch {
            playRawPcm(tickBuffer)
        }
    }

    fun playDing() {
        if (isMuted) return
        audioScope.launch {
            playRawPcm(dingBuffer)
        }
    }

    private fun playRawPcm(pcmData: ShortArray) {
        var audioTrack: AudioTrack? = null
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufSize, pcmData.size * 2)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val builder = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)

            if (Build.VERSION.SDK_INT >= 34) {
                appContext?.let {
                    builder.setContext(it)
                }
            }
            audioTrack = builder.build()

            audioTrack.write(pcmData, 0, pcmData.size)
            audioTrack.play()
            
            val durationMs = (pcmData.size * 1000) / SAMPLE_RATE
            Thread.sleep(durationMs.toLong() + 30)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (ex: Exception) {
                // Ignore failure on cleanup
            }
        }
    }
}
