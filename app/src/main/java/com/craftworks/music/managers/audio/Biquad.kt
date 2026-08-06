package com.craftworks.music.managers.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin

/**
 * Pure DSP math for biquad filters, based on the RBJ Audio EQ Cookbook
 * (https://www.w3.org/TR/audio-eq-cookbook/).
 *
 * This file has NO Android or Media3 dependencies so it can be unit-tested
 * on a plain JVM.
 */

/**
 * The 10 fixed ISO center frequencies (in Hz) of the equalizer, in ascending
 * order. This list is the single source of truth shared by the EQ audio
 * processor, the settings manager, and the settings UI — they all iterate
 * over it rather than duplicating the frequencies.
 *
 * Callers must treat this array as immutable; it is exposed as a [FloatArray]
 * so it can be iterated cheaply on the audio thread.
 */
val EQ_BAND_FREQUENCIES_HZ = floatArrayOf(
    31f, 63f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f
)

/**
 * Normalized (a0-removed) direct-form 1 biquad coefficients for a single
 * filter stage. The difference equation is:
 *
 * y[n] = b0*x[n] + b1*x[n-1] + b2*x[n-2] - a1*y[n-1] - a2*y[n-2]
 *
 * @property b0 feed-forward coefficient for x[n]
 * @property b1 feed-forward coefficient for x[n-1]
 * @property b2 feed-forward coefficient for x[n-2]
 * @property a1 feedback coefficient for y[n-1] (sign per the equation above)
 * @property a2 feedback coefficient for y[n-2] (sign per the equation above)
 */
data class BiquadCoefficients(
    val b0: Float,
    val b1: Float,
    val b2: Float,
    val a1: Float,
    val a2: Float,
)

/**
 * Computes the peaking (bell) filter coefficients for one equalizer band
 * using the RBJ Audio EQ Cookbook formulas.
 *
 * @param frequencyHz center frequency of the band in Hz
 * @param sampleRateHz sample rate of the audio stream in Hz
 * @param q filter quality factor (bandwidth); 0.707 (1 octave) is typical
 * @param gainDb boost/cut in decibels; 0dB produces exact passthrough
 * @return normalized [BiquadCoefficients] ready to apply to a sample stream
 */
fun peakingCoefficients(
    frequencyHz: Float,
    sampleRateHz: Float,
    q: Float,
    gainDb: Float,
): BiquadCoefficients {
    val a = 10f.pow(gainDb / 40f)
    val w0 = 2f * PI.toFloat() * frequencyHz / sampleRateHz
    val alpha = sin(w0) / (2f * q)
    val cosw = cos(w0)

    val b0 = 1f + alpha * a
    val b1 = -2f * cosw
    val b2 = 1f - alpha * a
    val a0 = 1f + alpha / a
    val a1 = -2f * cosw
    val a2 = 1f - alpha / a

    return BiquadCoefficients(
        b0 = b0 / a0,
        b1 = b1 / a0,
        b2 = b2 / a0,
        a1 = a1 / a0,
        a2 = a2 / a0,
    )
}

/**
 * Converts a gain in decibels to a linear amplitude multiplier.
 *
 * @param dB gain in decibels (0dB maps to 1.0)
 * @return linear gain 10^(dB/20)
 */
fun dBToLinear(dB: Float): Float = 10f.pow(dB / 20f)

/**
 * Converts a linear amplitude multiplier to a gain in decibels.
 *
 * @param linear linear gain (1.0 maps to 0dB); must be positive
 * @return gain in decibels 20*log10(linear)
 */
fun linearToDb(linear: Float): Float = 20f * log10(linear)
