package com.craftworks.music.ui.screens.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.managers.settings.DEFAULT_PRESET_ID
import com.craftworks.music.managers.settings.PlaybackSettingsManager
import com.craftworks.music.ui.elements.dialogs.EqualizerPresetDialog
import com.craftworks.music.ui.elements.dialogs.dialogFocusable
import com.craftworks.music.ui.elements.dialogs.presetLabelRes
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

@Preview(showSystemUi = false, showBackground = true)
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun S_EqualizerScreen(navHostController: NavHostController = rememberNavController()) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    var showPresetDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.Settings_Header_Equalizer)) },
                actions = {
                    IconButton(
                        onClick = {
                            navHostController.navigate(Screen.S_Playback.route) {
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.size(56.dp, 70.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = "Previous Song",
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
    ) { innerPadding ->
        Box (
            modifier = Modifier
                .padding(
                    top = innerPadding.calculateTopPadding()
                )
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .dialogFocusable()
        ) {
            Column(
                Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Equalizer
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val eqEnabled =
                        PlaybackSettingsManager(context).eqEnabledFlow.collectAsState(false).value

                    SettingsSwitch(
                        selected = eqEnabled,
                        settingsName = stringResource(R.string.Setting_Equalizer_Enable),
                        settingsIcon = ImageVector.vectorResource(R.drawable.outline_line_weight_24),
                        toggleEvent = {
                            coroutineScope.launch {
                                PlaybackSettingsManager(context).setEqEnabled(!eqEnabled)
                            }
                        }
                    )

                    val presetId =
                        PlaybackSettingsManager(context).eqPresetFlow.collectAsState(DEFAULT_PRESET_ID).value

                    SettingsDialogButton(
                        settingsName = stringResource(R.string.Setting_Equalizer_Preset),
                        settingsSubtitle = stringResource(presetLabelRes(presetId)),
                        settingsIcon = ImageVector.vectorResource(R.drawable.outline_line_weight_24),
                        toggleEvent = { showPresetDialog = true }
                    )

                    val preampDb =
                        PlaybackSettingsManager(context).eqPreampDbFlow.collectAsState(0f).value

                    SettingsSlider(
                        settingsName = stringResource(R.string.Setting_Equalizer_Preamp),
                        steps = 11,
                        value = preampDb,
                        minValue = -6f, maxValue = 6f,
                        valueText = formatDb(preampDb),
                        onValueChange = {
                            runBlocking {
                                PlaybackSettingsManager(context).setEqPreampDb(it)
                            }
                        }
                    )

                    val eqBandLabelIds = intArrayOf(
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

                    eqBandLabelIds.forEachIndexed { index, labelId ->
                        val bandGain =
                            PlaybackSettingsManager(context).eqBandGainDbFlow(index).collectAsState(0f).value

                        SettingsSlider(
                            settingsName = stringResource(labelId),
                            steps = 11,
                            value = bandGain,
                            minValue = -6f, maxValue = 6f,
                            valueText = formatDb(bandGain),
                            onValueChange = {
                                runBlocking {
                                    PlaybackSettingsManager(context).setEqBandGainDb(index, it)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showPresetDialog) EqualizerPresetDialog(setShowDialog = { showPresetDialog = it })
}

private fun formatDb(value: Float): String {
    val rounded = value.roundToInt()
    return if (rounded > 0) "+$rounded dB" else "$rounded dB"
}
