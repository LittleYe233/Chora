package com.craftworks.music.managers.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.StreamMetadata
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A 10-band peaking equalizer [androidx.media3.common.audio.AudioProcessor] built
 * from a cascade of RBJ biquad filters at the fixed ISO center frequencies in
 * [EQ_BAND_FREQUENCIES_HZ] (31 Hz ... 16 kHz).
 *
 * Target gains are set from any thread via [setBandGainDb] and [setEnabled]; the
 * audio thread eases each band's current gain toward its target every buffer, so
 * slider changes take effect in real time without clicks. Flushing snaps the gains
 * to their targets and clears all filter state, so seeks and track changes do not
 * produce transients.
 *
 * Inter-band crosstalk compensation: cascaded peaking biquads interfere with
 * their neighbors, so [setBandGainDb] first maps the user's requested gains
 * through the precomputed inverse coupling matrix
 * ([EqualizerCouplingMatrix.compensatedGains]). Each biquad then runs at a
 * slightly adjusted gain that cancels the leakage from adjacent bands, making
 * the measured response match the requested curve at every band center. The
 * matrix multiply runs only at control rate (slider changes); the audio-rate
 * path is unchanged and allocation-free.
 *
 * The processor accepts 16-bit PCM and float PCM. When disabled, [onConfigure]
 * returns [AudioFormat.NOT_SET] so the pipeline bypasses the processor entirely;
 * when enabled with every band at 0 dB the cascade is skipped and buffers are
 * copied straight through.
 */
@OptIn(UnstableApi::class)
class EqualizerAudioProcessor : BaseAudioProcessor() {

    private companion object {
        /**
         * Q of every peaking band. sqrt(2) (~1.414) matches the Q the coupling
         * matrix in [EqualizerCouplingMatrix] was computed for; do not change it
         * without regenerating that matrix.
         */
        val BAND_Q = sqrt(2f)

        /** Per-buffer gain smoothing factor; 4 buffers converges in ~<100 ms. */
        const val SMOOTHING_FACTOR = 0.25f

        /** Gain delta (dB) that forces the cached biquad coefficients to be recomputed. */
        const val COEFFICIENT_EPSILON_DB = 0.01f

        /** Number of EQ bands; must match [EQ_BAND_FREQUENCIES_HZ] length. */
        val BAND_COUNT = EQ_BAND_FREQUENCIES_HZ.size
    }

    /** Master switch; when false the processor passes audio through untouched. */
    @Volatile
    private var enabled = false

    /**
     * User-requested gains in dB per band, in [EQ_BAND_FREQUENCIES_HZ] order.
     * Written from any thread; the reference is swapped atomically.
     */
    @Volatile
    private var userTargetGainsDb = FloatArray(BAND_COUNT)

    /**
     * Crosstalk-compensated gains in dB per band, in [EQ_BAND_FREQUENCIES_HZ]
     * order: `INVERSE_COUPLING_MATRIX * userTargetGainsDb`. The array
     * reference is swapped atomically on write so the audio thread always reads
     * a consistent snapshot without synchronization.
     */
    @Volatile
    private var targetGainsDb = FloatArray(BAND_COUNT)

    /**
     * Per-band filter instances. This is audio-thread state: it is created in
     * [onConfigure] and only ever read or written from the audio thread.
     */
    private var bands = emptyArray<BandFilter>()

    /**
     * Enables or disables the equalizer.
     *
     * When disabled at configuration time the processor reports [AudioFormat.NOT_SET]
     * and the pipeline bypasses it; when disabled during playback the cascade is
     * skipped so output stays untouched.
     */
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    /**
     * Sets the target gain for one band, in decibels. The requested gain is
     * mapped through the inverse coupling matrix so the actual response at the
     * band centers matches the request despite inter-band crosstalk.
     *
     * @param index band index matching [EQ_BAND_FREQUENCIES_HZ] order (0 = 31 Hz,
     *   9 = 16 kHz)
     * @param gainDb target gain in dB (0 dB = unity)
     * @throws IllegalArgumentException if [index] is out of bounds
     */
    fun setBandGainDb(index: Int, gainDb: Float) {
        require(index in 0 until BAND_COUNT) {
            "Band index $index out of range 0..${BAND_COUNT - 1}"
        }
        val user = userTargetGainsDb.copyOf()
        user[index] = gainDb
        userTargetGainsDb = user
        val compensated = FloatArray(BAND_COUNT)
        EqualizerCouplingMatrix.compensatedGains(user, compensated)
        targetGainsDb = compensated
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        val encoding = inputAudioFormat.encoding
        if (encoding != C.ENCODING_PCM_16BIT && encoding != C.ENCODING_PCM_FLOAT) {
            throw UnhandledAudioFormatException(
                "Expected ENCODING_PCM_16BIT or ENCODING_PCM_FLOAT.",
                inputAudioFormat,
            )
        }
        if (!enabled) {
            return AudioFormat.NOT_SET
        }
        val targets = targetGainsDb
        bands = Array(BAND_COUNT) { index ->
            BandFilter(
                frequencyHz = EQ_BAND_FREQUENCIES_HZ[index],
                sampleRateHz = inputAudioFormat.sampleRate.toFloat(),
                channelCount = inputAudioFormat.channelCount,
                initialGainDb = targets[index],
            )
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        check(inputAudioFormat !== AudioFormat.NOT_SET) {
            "Audio processor must be configured and flushed before calling queueInput()."
        }
        if (!inputBuffer.hasRemaining()) {
            return
        }
        check(inputBuffer.remaining() % inputAudioFormat.bytesPerFrame == 0) {
            "Queued an incomplete frame."
        }

        easeBandGains()

        val outputBuffer = replaceOutputBuffer(inputBuffer.remaining())
        if (shouldBypassCascade()) {
            outputBuffer.put(inputBuffer)
        } else {
            processFrames(inputBuffer, outputBuffer)
        }
        outputBuffer.flip()
    }

    override fun onFlush(streamMetadata: StreamMetadata) {
        resetBands()
    }

    override fun onReset() {
        resetBands()
    }

    /** Eases every band's current gain one step toward its target. */
    private fun easeBandGains() {
        val targets = targetGainsDb
        val filterBands = bands
        if (filterBands.isEmpty()) {
            return
        }
        var bandIndex = 0
        while (bandIndex < BAND_COUNT) {
            filterBands[bandIndex].easeTowards(targets[bandIndex])
            bandIndex++
        }
    }

    /**
     * Whether the cascade can be skipped this buffer: either the equalizer is
     * disabled, or every band is exactly at 0 dB with a 0 dB target.
     */
    private fun shouldBypassCascade(): Boolean {
        if (!enabled) {
            return true
        }
        val targets = targetGainsDb
        val filterBands = bands
        if (filterBands.isEmpty()) {
            return true
        }
        var bandIndex = 0
        while (bandIndex < BAND_COUNT) {
            if (filterBands[bandIndex].currentGainDb != 0f || targets[bandIndex] != 0f) {
                return false
            }
            bandIndex++
        }
        return true
    }

    /** Runs every frame of [inputBuffer] through the 10-band cascade into [outputBuffer]. */
    private fun processFrames(inputBuffer: ByteBuffer, outputBuffer: ByteBuffer) {
        val encoding = inputAudioFormat.encoding
        val channelCount = inputAudioFormat.channelCount
        val filterBands = bands
        while (inputBuffer.hasRemaining()) {
            for (channel in 0 until channelCount) {
                var sample = when (encoding) {
                    C.ENCODING_PCM_FLOAT -> inputBuffer.float
                    else -> inputBuffer.short.toFloat()
                }
                var bandIndex = 0
                while (bandIndex < BAND_COUNT) {
                    sample = filterBands[bandIndex].process(channel, sample)
                    bandIndex++
                }
                when (encoding) {
                    C.ENCODING_PCM_FLOAT -> outputBuffer.putFloat(sample)
                    else -> outputBuffer.putShort(sample.toInt().toShort())
                }
            }
        }
    }

    /**
     * Snaps every band's gain to its target and clears all filter state.
     *
     * Safe to call before [onConfigure] has created the bands: the pipeline may
     * flush this processor even when it reported [AudioFormat.NOT_SET] (EQ
     * disabled), in which case [bands] is still empty.
     */
    private fun resetBands() {
        val targets = targetGainsDb
        val filterBands = bands
        if (filterBands.isEmpty()) {
            return
        }
        var bandIndex = 0
        while (bandIndex < BAND_COUNT) {
            filterBands[bandIndex].resetState(targets[bandIndex])
            bandIndex++
        }
    }

    /**
     * One peaking biquad band with per-channel direct-form 1 state. All state is
     * private to the audio thread: [currentGainDb] is eased toward the volatile
     * target, cached coefficients are recomputed when the smoothed gain moves by
     * more than [COEFFICIENT_EPSILON_DB], and [process] runs the DF1 difference
     * equation without allocating.
     */
    private class BandFilter(
        private val frequencyHz: Float,
        private val sampleRateHz: Float,
        channelCount: Int,
        initialGainDb: Float,
    ) {
        /** Smoothed gain in dB currently applied by this band. */
        var currentGainDb = initialGainDb
            private set

        /** Gain in dB the cached coefficients were last computed for. */
        private var coefficientGainDb = initialGainDb
        private var b0 = 0f
        private var b1 = 0f
        private var b2 = 0f
        private var a1 = 0f
        private var a2 = 0f
        private val x1 = FloatArray(channelCount)
        private val x2 = FloatArray(channelCount)
        private val y1 = FloatArray(channelCount)
        private val y2 = FloatArray(channelCount)

        init {
            updateCoefficients()
        }

        /** Eases the current gain one step toward [targetGainDb], recomputing if needed. */
        fun easeTowards(targetGainDb: Float) {
            currentGainDb += (targetGainDb - currentGainDb) * SMOOTHING_FACTOR
            if (abs(targetGainDb - currentGainDb) < COEFFICIENT_EPSILON_DB) {
                currentGainDb = targetGainDb
            }
            if (abs(currentGainDb - coefficientGainDb) > COEFFICIENT_EPSILON_DB) {
                updateCoefficients()
            }
        }

        /** Runs one sample of [channel] through the band and returns the filtered sample. */
        fun process(channel: Int, x: Float): Float {
            val y = b0 * x + b1 * x1[channel] + b2 * x2[channel] -
                a1 * y1[channel] - a2 * y2[channel]
            x2[channel] = x1[channel]
            x1[channel] = x
            y2[channel] = y1[channel]
            y1[channel] = y
            return y
        }

        /** Snaps the gain to [targetGainDb] and clears all per-channel DF1 state. */
        fun resetState(targetGainDb: Float) {
            currentGainDb = targetGainDb
            updateCoefficients()
            x1.fill(0f)
            x2.fill(0f)
            y1.fill(0f)
            y2.fill(0f)
        }

        /** Recomputes the cached coefficients for [currentGainDb] (not hot-path). */
        private fun updateCoefficients() {
            val coefficients = peakingCoefficients(frequencyHz, sampleRateHz, BAND_Q, currentGainDb)
            b0 = coefficients.b0
            b1 = coefficients.b1
            b2 = coefficients.b2
            a1 = coefficients.a1
            a2 = coefficients.a2
            coefficientGainDb = currentGainDb
        }
    }
}
