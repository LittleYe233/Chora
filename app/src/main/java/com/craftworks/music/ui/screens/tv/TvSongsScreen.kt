package com.craftworks.music.ui.screens.tv
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.tv.TvHorizontalSongCard
import com.craftworks.music.ui.viewmodels.SongsScreenViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSongsScreen(
    mediaController: MediaController? = null,
    viewModel: SongsScreenViewModel = hiltViewModel(),
) {
    val songs by viewModel.allSongs.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    if (NavidromeManager.checkActiveServers()) {
        LaunchedEffect(songs.size) {
            if (songs.size % 50 != 0) return@LaunchedEffect
            if (songs.size < 50) return@LaunchedEffect

            snapshotFlow {
                val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow false
                val total = gridState.layoutInfo.totalItemsCount
                if (total < songs.size - 5) return@snapshotFlow false
                (total - lastVisible) <= 15
            }.filter { it }.collect {
                viewModel.getMoreSongs(50)
            }
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(1),
        modifier = Modifier
            .fillMaxSize()
            .focusGroup()
            .focusRestorer(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(20.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp),
    ) {
        itemsIndexed(songs) { index, song ->
            TvHorizontalSongCard(
                song = song,
                onClick = {
                    coroutineScope.launch {
                        SongHelper.play(songs, index, mediaController)
                    }
                }
            )
        }
    }
}