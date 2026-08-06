package com.craftworks.music.ui.elements.dialogs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.craftworks.music.R
import com.craftworks.music.managers.settings.ALL_PRESET_IDS
import com.craftworks.music.managers.settings.PlaybackSettingsManager
import com.craftworks.music.ui.elements.bounceClick
import kotlinx.coroutines.runBlocking


//region PREVIEWS
@Preview(showBackground = true)
@Composable
fun PreviewEqualizerPresetDialog() {
    EqualizerPresetDialog(setShowDialog = { })
}
//endregion

/**
 * Returns the string resource holding the display name of the equalizer preset with [presetId].
 * Unknown ids (e.g. [com.craftworks.music.managers.settings.CUSTOM_EDITED_PRESET_ID]) fall back
 * to the generic "Custom" label.
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
    else -> R.string.Setting_Equalizer_Preset_Custom
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun EqualizerPresetDialog(
    setShowDialog: (Boolean) -> Unit
) {
    val context = LocalContext.current

    val currentPreset by PlaybackSettingsManager(context).eqPresetFlow.collectAsState("")

    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Column(
            modifier = Modifier
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
                .dialogFocusable()
                .selectableGroup()
        ) {
            Text(
                text = stringResource(R.string.Setting_Equalizer_Preset),
                fontWeight = FontWeight.SemiBold,
                fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            for (id in ALL_PRESET_IDS) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .selectable(
                            selected = id == currentPreset,
                            onClick = {
                                runBlocking {
                                    PlaybackSettingsManager(context).applyPreset(id)
                                }
                                setShowDialog(false)
                            },
                            role = Role.RadioButton
                        ),
                ) {
                    RadioButton(
                        selected = id == currentPreset,
                        onClick = {
                            runBlocking {
                                PlaybackSettingsManager(context).applyPreset(id)
                            }
                            setShowDialog(false)
                        },
                        modifier = Modifier.bounceClick()
                    )
                    Text(
                        text = context.getString(presetLabelRes(id)),
                        fontWeight = FontWeight.Normal,
                        fontSize = MaterialTheme.typography.titleMedium.fontSize,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
