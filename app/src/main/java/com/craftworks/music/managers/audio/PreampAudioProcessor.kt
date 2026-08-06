package com.craftworks.music.managers.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

/**
 * Easing coefficient applied to the current gain once per audio frame when easing it
 * toward the target gain. At a 48kHz sample rate this converges in roughly 5-10ms,
 * which is fast enough to be inaudible yet slow enough to prevent clicks from abrupt
 * gain steps.
 */
private const val SMOOTHING_COEFFICIENT = 0.001f

/**
 * A preamp gain stage implemented as an [AudioProcessor].
 *
 * The processor is always active: [onConfigure] accepts every supported PCM format and
 * returns it unchanged, so a unity gain costs only a buffer copy. The target gain is set
 * in decibels from any thread via [setTargetGainDb] (typically the UI thread) and applied
 * per sample on the audio thread, with exponential smoothing toward the target to avoid
 * clicks. On flush/reset the current gain snaps to the target (not to unity) so seeking
 * or track changes do not cause a volume jump.
 *
 * The preamp can be disabled via [setEnabled]; this is intended to stay in sync with the
 * equalizer's master switch (same-on-same-off). While disabled the effective target gain
 * is unity, so toggling the switch eases the current gain to a zero-cost pass-through
 * instead of clicking.
 *
 * This class is intentionally parameterless so it can be provided as a Hilt singleton and
 * shared by the player and the settings UI.
 */
@OptIn(UnstableApi::class)
class PreampAudioProcessor : BaseAudioProcessor() {

    /** Target linear gain multiplier, precomputed from dB on the caller thread. */
    @Volatile
    private var targetGain: Float = 1f

    /** Current linear gain multiplier, only touched on the audio thread. */
    private var currentGain: Float = 1f

    /** Master switch; when false the effective target gain is unity (pass-through). */
    @Volatile
    private var enabled: Boolean = true

    /**
     * Enables or disables the preamp.
     *
     * When disabled the effective target gain becomes unity and the current gain eases
     * back to 1.0, re-engaging the zero-cost copy path once it arrives.
     *
     * @param enabled true to apply [setTargetGainDb], false to pass audio through untouched
     */
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    /**
     * Sets the target preamp gain in decibels. The conversion to a linear multiplier is
     * done on the calling (UI) thread so the audio thread never performs math beyond the
     * smoothing step. A gain of 0dB (1.0 linear) enables the zero-cost copy path.
     *
     * @param gainDb target gain in decibels
     */
    fun setTargetGainDb(gainDb: Float) {
        targetGain = dBToLinear(gainDb)
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val encoding = inputAudioFormat.encoding
        if (encoding != C.ENCODING_PCM_16BIT && encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        // Always active: returning the input format makes this processor a pass-through
        // that is applied to every buffer (cost-free while the gain stays at unity).
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val outputBuffer = replaceOutputBuffer(inputBuffer.remaining())
        // Effective target: unity while disabled, so toggling the master switch eases
        // to/from unity via the same smoothing path instead of clicking.
        val effectiveTarget = if (enabled) targetGain else 1f
        if (effectiveTarget == 1f && currentGain == 1f) {
            // Unity gain: copy the frames unchanged for zero cost.
            outputBuffer.put(inputBuffer)
        } else {
            val encoding = inputAudioFormat.encoding
            val channelCount = inputAudioFormat.channelCount
            while (inputBuffer.hasRemaining()) {
                currentGain += (effectiveTarget - currentGain) * SMOOTHING_COEFFICIENT
                val gain = currentGain
                (0 until channelCount).forEach { _ ->
                    when (encoding) {
                        C.ENCODING_PCM_16BIT ->
                            outputBuffer.putShort((inputBuffer.getShort() * gain).toInt().toShort())
                        C.ENCODING_PCM_FLOAT ->
                            outputBuffer.putFloat(inputBuffer.getFloat() * gain)
                        else -> throw IllegalStateException("Unhandled PCM encoding: $encoding")
                    }
                }
            }
        }
        outputBuffer.flip()
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        // Snap to the target gain instead of resetting to unity so a seek or track
        // change does not cause a volume jump.
        currentGain = targetGain
    }

    override fun onReset() {
        currentGain = targetGain
    }
}
