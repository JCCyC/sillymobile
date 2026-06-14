package com.sillymobile.rallyx.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

enum class Waveform { SQUARE, TRIANGLE, NOISE }

/** A single melodic event. [freq] of [Notes.REST] is silence. */
data class Note(val freq: Double, val beats: Double)

/** One instrument line: a looping sequence of [Note]s rendered with [waveform] at [volume]. */
data class Track(val notes: List<Note>, val waveform: Waveform, val volume: Float)

/** A complete looping background tune, original composition in a retro arcade chiptune style. */
data class ChiptuneSong(val bpm: Int, val tracks: List<Track>)

/**
 * Generates and streams square/triangle/noise-wave chiptune music on a background thread using
 * [AudioTrack] in PCM streaming mode. No audio files are bundled - every note is synthesized
 * at runtime, so the melodies below are original compositions rather than recordings.
 */
class ChiptuneEngine {
    private val sampleRate = 22050
    private var thread: Thread? = null
    @Volatile private var running = false
    @Volatile private var currentSong: ChiptuneSong? = null
    @Volatile private var masterVolume = 0.35f

    fun setVolume(volume: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
    }

    /** Starts looping [song], replacing whatever was playing. No-op if already playing it. */
    fun play(song: ChiptuneSong) {
        if (currentSong === song && running) return
        stop()
        currentSong = song
        running = true
        thread = Thread { renderLoop(song) }.apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running = false
        thread?.interrupt()
        thread = null
        currentSong = null
    }

    private fun renderLoop(song: ChiptuneSong) {
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            minBuf * 2,
            AudioTrack.MODE_STREAM,
            android.media.AudioManager.AUDIO_SESSION_ID_GENERATE,
        )

        val secondsPerBeat = 60.0 / song.bpm
        val cursors = song.tracks.map { TrackCursor(it) }
        val bufferFrames = 1024
        val buffer = ShortArray(bufferFrames)

        audioTrack.play()
        try {
            while (running) {
                for (i in 0 until bufferFrames) {
                    var sample = 0.0
                    for (cursor in cursors) {
                        sample += cursor.nextSample(sampleRate, secondsPerBeat) * cursor.track.volume
                    }
                    sample *= masterVolume
                    val clamped = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                    buffer[i] = clamped
                }
                audioTrack.write(buffer, 0, bufferFrames)
            }
        } finally {
            audioTrack.stop()
            audioTrack.release()
        }
    }

    /** Walks one track's note list, looping forever, producing one waveform sample at a time. */
    private class TrackCursor(val track: Track) {
        private var noteIndex = 0
        private var sampleInNote = 0
        private var phase = 0.0
        private val noiseRandom = kotlin.random.Random(12345)

        fun nextSample(sampleRate: Int, secondsPerBeat: Double): Double {
            if (track.notes.isEmpty()) return 0.0
            var note = track.notes[noteIndex]
            val noteDurationSamples = (note.beats * secondsPerBeat * sampleRate).toInt().coerceAtLeast(1)

            if (sampleInNote >= noteDurationSamples) {
                sampleInNote = 0
                noteIndex = (noteIndex + 1) % track.notes.size
                note = track.notes[noteIndex]
                phase = 0.0
            }

            val value = if (note.freq <= 0.0) {
                0.0
            } else {
                val out = when (track.waveform) {
                    Waveform.SQUARE -> if (sin(phase) >= 0.0) 1.0 else -1.0
                    Waveform.TRIANGLE -> (2.0 / PI) * kotlin.math.asin(sin(phase))
                    Waveform.NOISE -> noiseRandom.nextDouble(-1.0, 1.0)
                }
                phase += 2.0 * PI * note.freq / sampleRate
                if (phase > 2.0 * PI) phase -= 2.0 * PI
                out * envelope(sampleInNote, noteDurationSamples)
            }

            sampleInNote++
            return value
        }

        /** Short attack/decay so notes don't click. */
        private fun envelope(sampleIndex: Int, totalSamples: Int): Double {
            val attack = (totalSamples / 12).coerceAtMost(200)
            val release = (totalSamples / 6).coerceAtMost(400)
            return when {
                sampleIndex < attack -> sampleIndex.toDouble() / attack
                sampleIndex > totalSamples - release -> (totalSamples - sampleIndex).toDouble() / release
                else -> 1.0
            }.coerceIn(0.0, 1.0)
        }
    }
}
