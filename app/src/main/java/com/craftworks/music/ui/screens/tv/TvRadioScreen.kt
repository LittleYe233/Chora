package com.craftworks.music.ui.screens.tv

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.tv.TvRadioCard
import com.craftworks.music.ui.viewmodels.RadioScreenViewModel
import kotlinx.coroutines.launch

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TvRadioScreen(
    mediaController: MediaController? = null,
    viewModel: RadioScreenViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()

    var showRadioModifyDialog by remember { mutableStateOf(false) }
    var showRadioAddDialog by remember { mutableStateOf(false) }

    val radios by viewModel.radioStations.collectAsStateWithLifecycle()

    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier
            .fillMaxSize()
            .focusGroup()
            .focusRestorer(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        items(radios) { radio ->
            TvRadioCard (
                radio = radio,
                onClick = {
                    coroutineScope.launch {
                        SongHelper.play(
                            listOf(radio),
                            0,
                            mediaController
                        )
                    }
                }
            )
        }
    }
}
