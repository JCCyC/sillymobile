package com.sillymobile.rallyx.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Synthesizes short one-shot sound effects on the fly (no audio assets), played via
 * [AudioTrack] in static mode. Each effect is rendered once and cached.
 */
class SfxPlayer {
    private val sampleRate = 22050

    private val flagPickup by lazy { renderTones(listOf(Notes.C5 to 0.07, Notes.E5 to 0.09), Waveform.SQUARE) }
    private val specialFlag by lazy {
        renderTones(listOf(Notes.C5 to 0.06, Notes.E5 to 0.06, Notes.G5 to 0.06, Notes.C6 to 0.12), Waveform.SQUARE)
    }
    private val smoke by lazy { renderSweep(900.0, 180.0, 0.3, Waveform.TRIANGLE) }
    private val crash by lazy { renderNoiseBurst(0.45) }
    private val stun by lazy { renderSweep(220.0, 880.0, 0.18, Waveform.SQUARE) + renderSweep(880.0, 220.0, 0.18, Waveform.SQUARE) }
    private val levelComplete by lazy {
        renderTones(listOf(Notes.C5 to 0.12, Notes.E5 to 0.12, Notes.G5 to 0.12, Notes.C6 to 0.25), Waveform.SQUARE)
    }
    private val gameOver by lazy {
        renderTones(listOf(Notes.G4 to 0.18, Notes.E4 to 0.18, Notes.C4 to 0.4), Waveform.TRIANGLE)
    }

    fun playFlagPickup() = play(flagPickup)
    fun playSpecialFlag() = play(specialFlag)
    fun playSmoke() = play(smoke)
    fun playCrash() = play(crash)
    fun playStun() = play(stun)
    fun playLevelComplete() = play(levelComplete)
    fun playGameOver() = play(gameOver)

    private fun play(samples: ShortArray) {
        Thread {
            val track = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
                samples.size * 2,
                AudioTrack.MODE_STATIC,
                android.media.AudioManager.AUDIO_SESSION_ID_GENERATE,
            )
            track.write(samples, 0, samples.size)
            track.play()
            Thread.sleep((samples.size * 1000L / sampleRate) + 50)
            track.stop()
            track.release()
        }.apply { isDaemon = true; start() }
    }

    private fun renderTones(notes: List<Pair<Double, Double>>, wave: Waveform): ShortArray {
        var result = ShortArray(0)
        for ((freq, dur) in notes) {
            result += renderTone(freq, dur, wave)
        }
        return result
    }

    private fun renderTone(freq: Double, durationSec: Double, wave: Waveform): ShortArray {
        val n = (durationSec * sampleRate).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val env = envelope(i, n)
            val raw = when (wave) {
                Waveform.SQUARE -> if (sin(phase) >= 0.0) 1.0 else -1.0
                Waveform.TRIANGLE -> (2.0 / PI) * kotlin.math.asin(sin(phase))
                Waveform.NOISE -> Random.nextDouble(-1.0, 1.0)
            }
            phase += 2.0 * PI * freq / sampleRate
            out[i] = ((raw * env) * Short.MAX_VALUE * 0.6).toInt().toShort()
        }
        return out
    }

    private fun renderSweep(startFreq: Double, endFreq: Double, durationSec: Double, wave: Waveform): ShortArray {
        val n = (durationSec * sampleRate).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / n
            val freq = startFreq + (endFreq - startFreq) * t
            val env = envelope(i, n)
            val raw = when (wave) {
                Waveform.SQUARE -> if (sin(phase) >= 0.0) 1.0 else -1.0
                Waveform.TRIANGLE -> (2.0 / PI) * kotlin.math.asin(sin(phase))
                Waveform.NOISE -> Random.nextDouble(-1.0, 1.0)
            }
            phase += 2.0 * PI * freq / sampleRate
            out[i] = ((raw * env) * Short.MAX_VALUE * 0.6).toInt().toShort()
        }
        return out
    }

    private fun renderNoiseBurst(durationSec: Double): ShortArray {
        val n = (durationSec * sampleRate).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val env = envelope(i, n)
            out[i] = ((Random.nextDouble(-1.0, 1.0) * env) * Short.MAX_VALUE * 0.6).toInt().toShort()
        }
        return out
    }

    private fun envelope(i: Int, n: Int): Double {
        val attack = (n / 20).coerceAtMost(100)
        val release = (n / 3).coerceAtMost(4000)
        return when {
            i < attack -> i.toDouble() / attack
            i > n - release -> (n - i).toDouble() / release
            else -> 1.0
        }.coerceIn(0.0, 1.0)
    }

    private operator fun ShortArray.plus(other: ShortArray): ShortArray {
        val combined = ShortArray(size + other.size)
        System.arraycopy(this, 0, combined, 0, size)
        System.arraycopy(other, 0, combined, size, other.size)
        return combined
    }
}
