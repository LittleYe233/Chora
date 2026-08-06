package com.craftworks.music.ui.screens.tv.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.managers.settings.DEFAULT_PRESET_ID
import com.craftworks.music.managers.settings.PlaybackSettingsManager
import com.craftworks.music.ui.elements.dialogs.tv.EqualizerPresetDialog
import com.craftworks.music.ui.elements.dialogs.tv.presetLabelRes
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Formats a dB value as a signed string, e.g. "+3 dB", "0 dB", "-5 dB"
private fun formatDb(value: Float): String {
    val rounded = value.roundToInt()
    val sign = if (rounded > 0) "+" else ""
    return "$sign$rounded dB"
}

@Composable
@Preview(device = "id:tv_1080p", showSystemUi = true, showBackground = true)
fun TvS_EqualizerScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showPresetDialog by remember { mutableStateOf(false) }

    // Equalizer band labels in ascending frequency order (index 0 = 31 Hz ... index 9 = 16 kHz)
    val equalizerBandLabels = listOf(
        R.string.Setting_Equalizer_Band_31Hz,
        R.string.Setting_Equalizer_Band_63Hz,
        R.string.Setting_Equalizer_Band_125Hz,
        R.string.Setting_Equalizer_Band_250Hz,
        R.string.Setting_Equalizer_Band_500Hz,
        R.string.Setting_Equalizer_Band_1kHz,
        R.string.Setting_Equalizer_Band_2kHz,
        R.string.Setting_Equalizer_Band_4kHz,
        R.string.Setting_Equalizer_Band_8kHz,
        R.string.Setting_Equalizer_Band_16kHz
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp)
    ) {
        item {
            val eqEnabled by PlaybackSettingsManager(context).eqEnabledFlow.collectAsState(false)

            SettingsSwitchItem(
                title = stringResource(R.string.Setting_Equalizer_Enable),
                checked = eqEnabled,
                onCheckedChange = {
                    coroutineScope.launch {
                        PlaybackSettingsManager(context).setEqEnabled(it)
                    }
                }
            )
        }

        item {
            val presetId by PlaybackSettingsManager(context).eqPresetFlow.collectAsState(DEFAULT_PRESET_ID)

            SettingsButtonItem(
                title = stringResource(R.string.Setting_Equalizer_Preset),
                subtitle = context.getString(presetLabelRes(presetId)),
                icon = ImageVector.vectorResource(R.drawable.outline_line_weight_24),
                onClick = { showPresetDialog = true }
            )
        }

        item {
            val sliderValue by PlaybackSettingsManager(context).eqPreampDbFlow.collectAsState(0f)

            Surface(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .padding(vertical = 8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.Setting_Equalizer_Preamp),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = formatDb(sliderValue),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            coroutineScope.launch {
                                PlaybackSettingsManager(context).setEqPreampDb(it)
                            }
                        },
                        valueRange = -5f..5f,
                        steps = 9,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onKeyEvent { keyEvent ->
                                when (keyEvent.key) {
                                    Key.DirectionRight -> {
                                        coroutineScope.launch {
                                            PlaybackSettingsManager(context).setEqPreampDb((sliderValue + 1f).coerceIn(-5f, 5f))
                                        }
                                        true
                                    }

                                    Key.DirectionLeft -> {
                                        coroutineScope.launch {
                                            PlaybackSettingsManager(context).setEqPreampDb((sliderValue - 1f).coerceIn(-5f, 5f))
                                        }
                                        true
                                    }

                                    else -> false
                                }
                            }
                    )
                }
            }
        }

        items(equalizerBandLabels.size) { index ->
            val sliderValue by PlaybackSettingsManager(context).eqBandGainDbFlow(index).collectAsState(0f)

            Surface(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .padding(vertical = 8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(equalizerBandLabels[index]),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = formatDb(sliderValue),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            coroutineScope.launch {
                                PlaybackSettingsManager(context).setEqBandGainDb(index, it)
                            }
                        },
                        valueRange = -5f..5f,
                        steps = 9,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onKeyEvent { keyEvent ->
                                when (keyEvent.key) {
                                    Key.DirectionRight -> {
                                        coroutineScope.launch {
                                            PlaybackSettingsManager(context).setEqBandGainDb(index, (sliderValue + 1f).coerceIn(-5f, 5f))
                                        }
                                        true
                                    }

                                    Key.DirectionLeft -> {
                                        coroutineScope.launch {
                                            PlaybackSettingsManager(context).setEqBandGainDb(index, (sliderValue - 1f).coerceIn(-5f, 5f))
                                        }
                                        true
                                    }

                                    else -> false
                                }
                            }
                    )
                }
            }
        }
    }

    if (showPresetDialog) {
        EqualizerPresetDialog(setShowDialog = { showPresetDialog = it })
    }
}
