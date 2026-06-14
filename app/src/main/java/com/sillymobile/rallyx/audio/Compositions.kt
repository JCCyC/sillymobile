package com.sillymobile.rallyx.audio

import com.sillymobile.rallyx.audio.Notes.A3
import com.sillymobile.rallyx.audio.Notes.A4
import com.sillymobile.rallyx.audio.Notes.B4
import com.sillymobile.rallyx.audio.Notes.C3
import com.sillymobile.rallyx.audio.Notes.C4
import com.sillymobile.rallyx.audio.Notes.C5
import com.sillymobile.rallyx.audio.Notes.D4
import com.sillymobile.rallyx.audio.Notes.D5
import com.sillymobile.rallyx.audio.Notes.E4
import com.sillymobile.rallyx.audio.Notes.E5
import com.sillymobile.rallyx.audio.Notes.F3
import com.sillymobile.rallyx.audio.Notes.F4
import com.sillymobile.rallyx.audio.Notes.G3
import com.sillymobile.rallyx.audio.Notes.G4
import com.sillymobile.rallyx.audio.Notes.G5
import com.sillymobile.rallyx.audio.Notes.REST

/**
 * Two short, original looping chiptune compositions written for this project. They aim for the
 * bouncy, square-wave arcade feel of early-80s racing games without quoting any existing
 * copyrighted melody - all notes/rhythms here are new.
 */
object Compositions {

    /** Bouncy major-key theme for ordinary maze stages. */
    val NORMAL_THEME = ChiptuneSong(
        bpm = 132,
        tracks = listOf(
            // Lead: syncopated eighth-note riff over a I - vi - IV - V progression.
            Track(
                waveform = Waveform.SQUARE,
                volume = 0.9f,
                notes = eighths(
                    E4, G4, C5, G4, E4, D4, C4, D4,
                    E4, G4, A4, G4, E4, D4, C4, REST,
                    F4, A4, C5, A4, F4, E4, D4, E4,
                    G4, E4, D4, C4, D4, E4, G4, REST,
                ),
            ),
            // Bass: bouncing octave roots, one per beat.
            Track(
                waveform = Waveform.TRIANGLE,
                volume = 0.55f,
                notes = quarters(
                    C3, C4, C3, C4,
                    A3, A4, A3, A4,
                    F3, F4, F3, F4,
                    G3, G4, G3, G4,
                ),
            ),
            // Percussion: soft off-beat noise hits.
            Track(
                waveform = Waveform.NOISE,
                volume = 0.10f,
                notes = offbeatHits(count = 16),
            ),
        ),
    )

    /** Faster, driving minor-key theme for the bonus "challenge" stages. */
    val CHALLENGE_THEME = ChiptuneSong(
        bpm = 176,
        tracks = listOf(
            // Lead: rapid sixteenth-note arpeggios over i - VI - III - VII (Am F C G).
            Track(
                waveform = Waveform.SQUARE,
                volume = 0.9f,
                notes = sixteenths(
                    A4, C5, E5, C5, A4, C5, E5, C5, A4, C5, E5, C5, A4, C5, E5, C5,
                    F4, A4, C5, A4, F4, A4, C5, A4, F4, A4, C5, A4, F4, A4, C5, A4,
                    C5, E5, G5, E5, C5, E5, G5, E5, C5, E5, G5, E5, C5, E5, G5, E5,
                    G4, B4, D5, B4, G4, B4, D5, B4, G4, B4, D5, B4, G4, B4, D5, B4,
                ),
            ),
            // Bass: driving quarter-note roots.
            Track(
                waveform = Waveform.TRIANGLE,
                volume = 0.6f,
                notes = quarters(
                    A3, A4, A3, A4,
                    F3, F4, F3, F4,
                    C3, C4, C3, C4,
                    G3, G4, G3, G4,
                ),
            ),
            // Percussion: constant eighth-note drive.
            Track(
                waveform = Waveform.NOISE,
                volume = 0.12f,
                notes = offbeatHits(count = 32, everyBeat = true),
            ),
        ),
    )

    private fun quarters(vararg freqs: Double) = freqs.map { Note(it, 1.0) }
    private fun eighths(vararg freqs: Double) = freqs.map { Note(it, 0.5) }
    private fun sixteenths(vararg freqs: Double) = freqs.map { Note(it, 0.25) }

    /** A short noise tick on every off-beat eighth note, silence elsewhere. */
    private fun offbeatHits(count: Int, everyBeat: Boolean = false): List<Note> {
        val notes = mutableListOf<Note>()
        repeat(count) {
            if (everyBeat) {
                notes.add(Note(120.0, 0.5))
            } else {
                notes.add(Note(REST, 0.5))
                notes.add(Note(120.0, 0.5))
            }
        }
        return notes
    }
}
