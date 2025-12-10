package com.craftworks.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.craftworks.music.R
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.RippleEffect
import com.craftworks.music.ui.elements.SongsHorizontalColumn
import com.craftworks.music.ui.elements.TopBarWithSearch
import com.craftworks.music.ui.elements.dialogs.AddSongToPlaylist
import com.craftworks.music.ui.elements.dialogs.showAddSongToPlaylistDialog
import com.craftworks.music.ui.playing.dpToPx
import com.craftworks.music.ui.viewmodels.SongsScreenViewModel
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SongsScreen(
    mediaController: MediaController? = null,
    viewModel: SongsScreenViewModel = hiltViewModel()
) {
    val allSongsList by viewModel.allSongs.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()

    val state = rememberPullToRefreshState()
    val isRefreshing by viewModel.isLoading.collectAsStateWithLifecycle()

    var showRipple by remember { mutableIntStateOf(0) }
    val rippleXOffset = LocalWindowInfo.current.containerSize.width / 2
    val rippleYOffset = dpToPx(12)

    val onRefresh: () -> Unit = {
        viewModel.getSongs()
        showRipple++
    }

    PullToRefreshBox(
        state = state,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh
    ) {
        Scaffold(
            topBar = {
                TopBarWithSearch(
                    headerIcon = ImageVector.vectorResource(R.drawable.round_music_note_24),
                    headerText = stringResource(R.string.songs),
                    onSearch = { query -> viewModel.search(query) },
                ) {
                    SongsHorizontalColumn(
                        songList = searchResults,
                        onSongSelected = { songs, index ->
                            println("Starting song at index: $index")
                            coroutineScope.launch {
                                SongHelper.play(songs, index, mediaController)
                            }
                        },
                        isSearch = true,
                        viewModel = viewModel
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .height(64.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play all
                    Button(
                        onClick = {
                            Log.d("SongsScreenPlayAll",
                                "Starting song at first index: 0. Song list size: ${allSongsList.size}")
                            coroutineScope.launch {
                                SongHelper.play(
                                    allSongsList,
                                    0,
                                    mediaController
                                )
                            }
                        },
                        modifier = Modifier
                            .widthIn(min = 128.dp, max = 320.dp)
                    ) {
                        Row (verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(24.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, "Play Songs")
                            Text(stringResource(R.string.Action_Play), maxLines = 1)
                        }
                    }
                    // Shuffle all
                    OutlinedButton(
                        onClick = {
                            mediaController?.shuffleModeEnabled = true
                            val random = allSongsList.subList(1, allSongsList.size).indices.random()
                            Log.d("SongsScreenShuffleAll",
                                "Starting song at random index: $random. Song list size: ${allSongsList.size}")
                            coroutineScope.launch {
                                SongHelper.play(
                                    allSongsList.subList(1, allSongsList.size),
                                    random,
                                    mediaController
                                )
                            }
                        },
                        modifier = Modifier
                            .widthIn(min = 128.dp, max = 320.dp)
                    ) {
                        Row (verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(24.dp)
                        ) {
                            Icon(ImageVector.vectorResource(R.drawable.round_shuffle_28), "Shuffle Songs")
                            Text(stringResource(R.string.Action_Shuffle), maxLines = 1)
                        }
                    }
                }
                // List
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    SongsHorizontalColumn(
                        songList = allSongsList,
                        onSongSelected = { songs, index ->
                            println("Starting song at index: $index")
                            coroutineScope.launch {
                                SongHelper.play(songs, index, mediaController)
                            }
                        },
                        isSearch = false,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    if(showAddSongToPlaylistDialog.value)
        AddSongToPlaylist(setShowDialog =  { showAddSongToPlaylistDialog.value = it } )

    RippleEffect(
        center = Offset(rippleXOffset.toFloat(), rippleYOffset.toFloat()),
        color = MaterialTheme.colorScheme.surfaceVariant,
        key = showRipple
    )
}