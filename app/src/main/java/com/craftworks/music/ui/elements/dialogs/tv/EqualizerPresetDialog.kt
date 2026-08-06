package com.craftworks.music.ui.elements.dialogs.tv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.craftworks.music.R
import com.craftworks.music.managers.settings.ALL_PRESET_IDS
import com.craftworks.music.managers.settings.PlaybackSettingsManager
import kotlinx.coroutines.launch

/**
 * Maps an equalizer preset id to the string resource for its display name.
 * Covers every id in [ALL_PRESET_IDS] plus the "custom" edited state
 * ([CUSTOM_EDITED_PRESET_ID]), which is never shown in the picker but is
 * displayed in the settings subtitle once the user manually edits a preset.
 */
fun presetLabelRes(presetId: String): Int = when (presetId) {
    "flat" -> R.string.Setting_Equalizer_Preset_Flat
    "pop" -> R.string.Setting_Equalizer_Preset_Pop
    "rock" -> R.string.Setting_Equalizer_Preset_RockMetal
    "electronic" -> R.string.Setting_Equalizer_Preset_Electronic
    "hiphop" -> R.string.Setting_Equalizer_Preset_HipHop
    "jazz" -> R.string.Setting_Equalizer_Preset_JazzBlues
    "classical" -> R.string.Setting_Equalizer_Preset_Classical
    "vocal" -> R.string.Setting_Equalizer_Preset_Vocal
    "lofi" -> R.string.Setting_Equalizer_Preset_Lofi
    "custom1" -> R.string.Setting_Equalizer_Preset_Custom1
    "custom2" -> R.string.Setting_Equalizer_Preset_Custom2
    "custom3" -> R.string.Setting_Equalizer_Preset_Custom3
    "custom" -> R.string.Setting_Equalizer_Preset_Custom
    else -> R.string.Setting_Equalizer_Preset_Flat
}

@Composable
fun EqualizerPresetDialog(setShowDialog: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = PlaybackSettingsManager(context)

    // "custom" (edited state) has no row in ALL_PRESET_IDS; no row is highlighted then.
    val currentPreset by manager.eqPresetFlow.collectAsState("")

    GenericListDialog(
        setShowDialog = setShowDialog,
        titleRes = R.string.Setting_Equalizer_Preset,
        options = ALL_PRESET_IDS,
        selectedOption = currentPreset,
        label = { id -> context.getString(presetLabelRes(id)) },
        onOptionSelected = { id ->
            scope.launch { manager.applyPreset(id) }
        }
    )
}
