package com.sillymobile.rallyx.audio

import kotlin.math.pow

/** Equal-temperament note frequencies, relative to A4 = 440Hz. Used to write the original
 *  chiptune-style melodies without depending on any external audio assets. */
object Notes {
    const val REST = 0.0

    private fun f(semitonesFromA4: Int): Double = 440.0 * 2.0.pow(semitonesFromA4 / 12.0)

    // Octave 3
    val C3 = f(-21); val D3 = f(-19); val E3 = f(-17); val F3 = f(-16)
    val G3 = f(-14); val A3 = f(-12); val B3 = f(-10)

    // Octave 4
    val C4 = f(-9); val D4 = f(-7); val E4 = f(-5); val F4 = f(-4)
    val G4 = f(-2); val A4 = f(0); val B4 = f(2)

    // Octave 5
    val C5 = f(3); val D5 = f(5); val E5 = f(7); val F5 = f(8)
    val G5 = f(10); val A5 = f(12); val B5 = f(14)

    // Octave 6
    val C6 = f(15); val D6 = f(17); val E6 = f(19); val G6 = f(22)
}
