package com.craftworks.music.ui.screens.tv

import android.os.Bundle
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.ui.elements.tv.TvPlaylistCard
import com.craftworks.music.ui.viewmodels.PlaylistScreenViewModel

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TvPlaylistScreen(
    navHostController: NavHostController = rememberNavController(),
    viewModel: PlaylistScreenViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()

    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()

    val tabs = listOf(
        stringResource(R.string.playlists),
        stringResource(R.string.Label_Sort_Starred),
    )
    var selectedTabIndex by remember { mutableIntStateOf(0) }

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
        /*
        item(span = { GridItemSpan(5) }) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier
                    .focusGroup()
                    .focusRestorer()
                    .fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, tab ->
                    key(index) {
                        Tab(
                            selected = index == selectedTabIndex,
                            onFocus = {
                                selectedTabIndex = index

                            },
                        ) {
                            Text(
                                text = tab
                            )
                        }
                    }
                }
            }
        }
        */
        item {
            val favouritesPlaylist = MediaItem.Builder()
                .setMediaId("favourites")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Favourites")
                        .setIsPlayable(false)
                        .setIsBrowsable(true)
                        .setArtworkUri(("android.resource://com.craftworks.music/" + R.drawable.favourites).toUri())
                        .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                        .setExtras(Bundle().apply {
                            putString("navidromeID", "favourites")
                        })
                        .build()
                )
                .build()

            TvPlaylistCard (
                playlist = favouritesPlaylist,
                onClick = {
                    viewModel.setCurrentPlaylist(favouritesPlaylist)
                    navHostController.navigate(Screen.PlaylistDetails.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        items(playlists) { playlist ->
            TvPlaylistCard (
                playlist = playlist,
                onClick = {
                    viewModel.setCurrentPlaylist(playlist)
                    navHostController.navigate(Screen.PlaylistDetails.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        /*
        if (selectedTabIndex == 0) {
            items(playlists) { playlist ->
                TvPlaylistCard (
                    playlist = playlist,
                    onClick = {
                        viewModel.setCurrentPlaylist(playlist)
                        navHostController.navigate(Screen.PlaylistDetails.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
        else {
            item {

            }
        }
        */
    }
}
