package com.craftworks.music.managers.settings

/**
 * A named equalizer preset: preamp gain (dB) plus 10 band gains (dB) in ascending
 * ISO frequency order (31Hz..16kHz).
 */
data class EqualizerPreset(val id: String, val preampDb: Float, val bandGainsDb: FloatArray)

/** Fixed (built-in) presets. Band order: 31/63/125/250/500/1k/2k/4k/8k/16k Hz. */
val FIXED_EQUALIZER_PRESETS: List<EqualizerPreset> = listOf(
    EqualizerPreset("flat", 0f, floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    EqualizerPreset("pop", -4f, floatArrayOf(2f, 3f, 2f, -1f, 0f, 1f, 2f, 2f, 2f, 1f)),
    EqualizerPreset("rock", -5f, floatArrayOf(4f, 4f, 2f, -2f, -1f, 0f, 2f, 3f, 2f, 1f)),
    EqualizerPreset("electronic", -6f, floatArrayOf(6f, 5f, 3f, 0f, -2f, -1f, 0f, 2f, 3f, 4f)),
    EqualizerPreset("hiphop", -6f, floatArrayOf(5f, 6f, 3f, -1f, -2f, 0f, 2f, 2f, 1f, 0f)),
    EqualizerPreset("jazz", -3f, floatArrayOf(0f, 1f, 2f, 2f, 0f, 0f, 1f, 1f, 2f, 1f)),
    EqualizerPreset("classical", -3f, floatArrayOf(1f, 1f, 0f, -1f, 0f, 0f, 1f, 2f, 3f, 2f)),
    EqualizerPreset("vocal", -3f, floatArrayOf(-2f, -1f, 0f, -1f, 0f, 2f, 3f, 2f, 1f, 0f)),
    EqualizerPreset("lofi", -3f, floatArrayOf(2f, 3f, 3f, 2f, 1f, -1f, -2f, -2f, -1f, -2f)),
)

/** Ids of the three user-customizable slots. */
val CUSTOM_PRESET_IDS: List<String> = listOf("custom1", "custom2", "custom3")

/** Id used when the user manually edits parameters after choosing a fixed preset. Never shown in the picker. */
const val CUSTOM_EDITED_PRESET_ID = "custom"

/** The default preset id when nothing was chosen yet. */
const val DEFAULT_PRESET_ID = "flat"

/** All selectable preset ids: fixed presets first, then the three custom slots. */
val ALL_PRESET_IDS: List<String> = FIXED_EQUALIZER_PRESETS.map { it.id } + CUSTOM_PRESET_IDS

/** Flat/zero preset used as the fallback for empty custom slots. */
val ZERO_PRESET = EqualizerPreset("flat", 0f, FloatArray(10) { 0f })

/** Encodes [preampDb] and [bandGainsDb] into the persistence string: "preamp;b0;b1;...;b9". */
fun encodePresetValues(preampDb: Float, bandGainsDb: FloatArray): String =
    buildString {
        append(preampDb)
        for (g in bandGainsDb) { append(';'); append(g) }
    }

/** Decodes a string produced by [encodePresetValues] into (preampDb, FloatArray(10)); returns null on malformed input. */
fun decodePresetValues(encoded: String?): Pair<Float, FloatArray>? {
    if (encoded == null) return null
    val parts = encoded.split(';')
    if (parts.size != 11) return null
    return try {
        parts[0].toFloat() to FloatArray(10) { i -> parts[i + 1].toFloat() }
    } catch (_: NumberFormatException) {
        null
    }
}
