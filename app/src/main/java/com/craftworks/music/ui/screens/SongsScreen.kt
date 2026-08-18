package com.craftworks.music.ui.screens

import android.util.Log
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.StarRating
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.craftworks.music.R
import com.craftworks.music.data.model.SongSortOrder
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.RippleEffect
import com.craftworks.music.ui.elements.SongsHorizontalColumn
import com.craftworks.music.ui.elements.TopBarWithSearch
import com.craftworks.music.ui.elements.dialogs.AddSongToPlaylist
import com.craftworks.music.ui.elements.dialogs.RatingDialog
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

    var songToRate by remember { mutableStateOf<MediaItem?>(null) }


    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle()
    val songSortOrder by viewModel.songSortOrder.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }
    val sortMenuItems = listOf(
        SongSortOrder.DEFAULT to R.string.Label_Sort_Default,
        SongSortOrder.TITLE to R.string.Label_Sort_Song_Title,
        SongSortOrder.ALBUM to R.string.Label_Sort_Song_Album,
        SongSortOrder.ARTIST to R.string.Label_Sort_Song_Artist,
        SongSortOrder.ADDED_ASC to R.string.Label_Sort_Song_Added_Asc,
        SongSortOrder.ADDED_DESC to R.string.Label_Sort_Song_Added_Desc,
    )

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    PullToRefreshBox(
        state = state,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopBarWithSearch(
                    headerText = stringResource(R.string.songs),
                    scrollBehavior = scrollBehavior,
                    onSearch = { query -> viewModel.search(query) },
                    searchResults = {
                        SongsHorizontalColumn(
                            songList = searchResults,
                            onSongSelected = { songs, index ->
                                println("Starting song at index: $index")
                                coroutineScope.launch {
                                    SongHelper.play(songs, index, mediaController)
                                }
                            },
                            onAddToQueue = {
                                mediaController?.addMediaItem(it)
                            },
                            onSetRating = { songToRate = it },
                            isSearch = true,
                            showFavoritesOnly = false,
                            viewModel = viewModel
                        )
                    },
                    extraAction = {
                        Row {
                            Box {
                                IconButton (
                                    onClick = { viewModel.setShowFavoritesOnly(!showFavoritesOnly) }
                                ) {
                                    Icon (
                                        imageVector = ImageVector.vectorResource(if (showFavoritesOnly) androidx.media3.session.R.drawable.media3_icon_heart_filled else androidx.media3.session.R.drawable.media3_icon_heart_unfilled),
                                        contentDescription = stringResource(R.string.Label_Toggle_Favorites),
                                    )
                                }
                            }

                            Box {
                                IconButton(
                                    onClick = { showSortMenu = true }
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.rounded_sort_24),
                                        contentDescription = stringResource(R.string.Label_Sorting),
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    sortMenuItems.forEach { (order, labelRes) ->
                                        DropdownMenuItem(
                                            text = { Text(stringResource(labelRes)) },
                                            trailingIcon = {
                                                if (order == songSortOrder)
                                                    Icon(Icons.Rounded.Check, contentDescription = null)
                                            },
                                            onClick = {
                                                viewModel.setSortOrder(order)
                                                showSortMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
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
                    Button(
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
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
                        onAddToQueue = {
                            mediaController?.addMediaItem(it)
                        },
                        onSetRating = { songToRate = it },
                        isSearch = false,
                        showFavoritesOnly = showFavoritesOnly,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    if(showAddSongToPlaylistDialog.value)
        AddSongToPlaylist(setShowDialog =  { showAddSongToPlaylistDialog.value = it } )

    songToRate?.let { song ->
        RatingDialog(
            currentRating = (song.mediaMetadata.userRating as? StarRating)?.starRating?.toInt() ?: 0,
            onDismiss = { songToRate = null },
            onSetRating = { rating ->
                viewModel.setSongRating(song.mediaMetadata.extras?.getString("navidromeID") ?: "", rating)
                songToRate = null
            }
        )
    }

    RippleEffect(
        center = Offset(rippleXOffset.toFloat(), rippleYOffset.toFloat()),
        color = MaterialTheme.colorScheme.surfaceVariant,
        key = showRipple
    )
}