package com.craftworks.music.managers.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.craftworks.music.dataStore
import com.craftworks.music.managers.audio.EQ_BAND_FREQUENCIES_HZ
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackSettingsManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        const val MIN_PRELOAD_PAGES = 1
        const val MAX_PRELOAD_PAGES = 50

        private val TRANSCODING_BITRATE_WIFI_KEY = stringPreferencesKey("transcoding_bitrate_wifi")
        private val TRANSCODING_BITRATE_DATA_KEY = stringPreferencesKey("transcoding_bitrate_data")
        private val TRANSCODING_FORMAT_KEY = stringPreferencesKey("transcoding_format")

        private val AUTOPLAY_SONGS = booleanPreferencesKey("autoplay")

        private val SONGS_PRELOAD_ENABLED = booleanPreferencesKey("songs_preload_enabled")
        private val SONGS_PRELOAD_PAGES = intPreferencesKey("songs_preload_pages")

        private val SCROBBLE_PERCENT_KEY = intPreferencesKey("scrobble_percent")

        private val EQ_ENABLED_KEY = booleanPreferencesKey("eq_enabled")
        private val EQ_PREAMP_DB_KEY = floatPreferencesKey("eq_preamp_db")
        private val EQ_BAND_GAIN_DB_KEYS = EQ_BAND_FREQUENCIES_HZ.mapIndexed { index, _ ->
            floatPreferencesKey("eq_band_gain_$index")
        }
        private val EQ_PRESET_KEY = stringPreferencesKey("eq_preset")
        private val EQ_CUSTOM_PRESET_KEYS = mapOf(
            "custom1" to stringPreferencesKey("eq_custom_preset_1"),
            "custom2" to stringPreferencesKey("eq_custom_preset_2"),
            "custom3" to stringPreferencesKey("eq_custom_preset_3"),
        )
    }

    val wifiTranscodingBitrateFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TRANSCODING_BITRATE_WIFI_KEY] ?: "No Transcoding"
    }

    suspend fun setWifiTranscodingBitrate(bitrate: String) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[TRANSCODING_BITRATE_WIFI_KEY] = bitrate
            }
        }
    }

    val mobileDataTranscodingBitrateFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TRANSCODING_BITRATE_DATA_KEY] ?: "No Transcoding"
    }

    suspend fun setMobileDataTranscodingBitrate(bitrate: String) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[TRANSCODING_BITRATE_DATA_KEY] = bitrate
            }
        }
    }

    val transcodingFormatFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TRANSCODING_FORMAT_KEY] ?: "opus"
    }

    suspend fun setTranscodingFormat(format: String) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[TRANSCODING_FORMAT_KEY] = format
            }
        }
    }

    val scrobblePercentFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SCROBBLE_PERCENT_KEY] ?: 7
    }

    suspend fun setScrobblePercent(scrobblePercent: Int) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[SCROBBLE_PERCENT_KEY] = scrobblePercent
            }
        }
    }

    val autoPlayFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTOPLAY_SONGS] ?: false
    }

    suspend fun setAutoPlay(autoPlay: Boolean) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[AUTOPLAY_SONGS] = autoPlay
            }
        }
    }

    /**
     * Whether the all-songs list prefetches pages ahead of the viewport. Defaults to `false`.
     */
    val songsPreloadEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SONGS_PRELOAD_ENABLED] ?: false
    }

    /**
     * Sets whether the all-songs list preloading is enabled.
     */
    suspend fun setSongsPreloadEnabled(enabled: Boolean) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[SONGS_PRELOAD_ENABLED] = enabled
            }
        }
    }

    /**
     * Number of pages preloaded ahead of the current viewport page. Defaults to `10`,
     * always clamped to 1..50.
     */
    val songsPreloadPages: Flow<Int> = context.dataStore.data.map { preferences ->
        (preferences[SONGS_PRELOAD_PAGES] ?: 10).coerceIn(MIN_PRELOAD_PAGES, MAX_PRELOAD_PAGES)
    }

    /**
     * Sets the preload page count, clamped to [MIN_PRELOAD_PAGES]..[MAX_PRELOAD_PAGES].
     */
    suspend fun setSongsPreloadPages(pages: Int) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[SONGS_PRELOAD_PAGES] = pages.coerceIn(MIN_PRELOAD_PAGES, MAX_PRELOAD_PAGES)
            }
        }
    }

    /**
     * Whether the 10-band equalizer is enabled. Defaults to `false` (full passthrough).
     */
    val eqEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[EQ_ENABLED_KEY] ?: false
    }

    /**
     * Sets whether the 10-band equalizer is enabled.
     */
    suspend fun setEqEnabled(enabled: Boolean) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[EQ_ENABLED_KEY] = enabled
            }
        }
    }

    /**
     * Preamp gain in dB, applied before the equalizer. Defaults to `0.0f`.
     */
    val eqPreampDbFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[EQ_PREAMP_DB_KEY] ?: 0.0f
    }

    /**
     * Sets the preamp gain in dB, applied before the equalizer.
     */
    suspend fun setEqPreampDb(db: Float) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[EQ_PREAMP_DB_KEY] = db
                val currentPreset = preferences[EQ_PRESET_KEY] ?: DEFAULT_PRESET_ID
                if (currentPreset in CUSTOM_PRESET_IDS) {
                    preferences[EQ_CUSTOM_PRESET_KEYS.getValue(currentPreset)] = encodePresetValues(
                        db,
                        FloatArray(10) { i -> preferences[EQ_BAND_GAIN_DB_KEYS[i]] ?: 0f },
                    )
                } else {
                    preferences[EQ_PRESET_KEY] = CUSTOM_EDITED_PRESET_ID
                }
            }
        }
    }

    /**
     * Gain in dB for the equalizer band at [index], in ascending frequency order.
     * Defaults to `0.0f`. Valid indices are `0..EQ_BAND_FREQUENCIES_HZ.lastIndex`.
     */
    fun eqBandGainDbFlow(index: Int): Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[EQ_BAND_GAIN_DB_KEYS[index]] ?: 0.0f
    }

    /**
     * Sets the gain in dB for the equalizer band at [index], in ascending frequency order.
     */
    suspend fun setEqBandGainDb(index: Int, db: Float) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[EQ_BAND_GAIN_DB_KEYS[index]] = db
                val currentPreset = preferences[EQ_PRESET_KEY] ?: DEFAULT_PRESET_ID
                if (currentPreset in CUSTOM_PRESET_IDS) {
                    val preampDb = preferences[EQ_PREAMP_DB_KEY] ?: 0f
                    preferences[EQ_CUSTOM_PRESET_KEYS.getValue(currentPreset)] = encodePresetValues(
                        preampDb,
                        FloatArray(10) { i -> preferences[EQ_BAND_GAIN_DB_KEYS[i]] ?: 0f },
                    )
                } else {
                    preferences[EQ_PRESET_KEY] = CUSTOM_EDITED_PRESET_ID
                }
            }
        }
    }

    /**
     * Currently selected equalizer preset id. Defaults to [DEFAULT_PRESET_ID] ("flat").
     */
    val eqPresetFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[EQ_PRESET_KEY] ?: DEFAULT_PRESET_ID
    }

    /**
     * Applies the preset with [id] in a single transaction: stores the preset id and writes
     * its preamp and band gains. Fixed presets come from [FIXED_EQUALIZER_PRESETS]; custom
     * slots are decoded from their persisted values, falling back to [ZERO_PRESET] when empty
     * or malformed.
     */
    suspend fun applyPreset(id: String) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                val preset = FIXED_EQUALIZER_PRESETS.firstOrNull { it.id == id }
                    ?: if (id in CUSTOM_PRESET_IDS) {
                        decodePresetValues(preferences[EQ_CUSTOM_PRESET_KEYS.getValue(id)])?.let { (preamp, gains) ->
                            EqualizerPreset(id, preamp, gains)
                        } ?: ZERO_PRESET
                    } else {
                        ZERO_PRESET
                    }
                preferences[EQ_PRESET_KEY] = id
                preferences[EQ_PREAMP_DB_KEY] = preset.preampDb
                EQ_BAND_GAIN_DB_KEYS.forEachIndexed { i, key ->
                    preferences[key] = preset.bandGainsDb[i]
                }
            }
        }
    }
}