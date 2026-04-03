package com.craftworks.music.ui.screens.tv

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component1
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component2
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.toAlbum
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.dialogs.tv.SongDialog
import com.craftworks.music.ui.elements.tv.TvAlbumCard
import com.craftworks.music.ui.elements.tv.TvArtistCard
import com.craftworks.music.ui.elements.tv.TvHorizontalSongCard
import com.craftworks.music.ui.viewmodels.AlbumScreenViewModel
import com.craftworks.music.ui.viewmodels.ArtistsScreenViewModel
import com.craftworks.music.ui.viewmodels.SongsScreenViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder

@Composable
fun TvSearchScreen(
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController?,
    albumsViewModel: AlbumScreenViewModel = hiltViewModel(),
    songsViewModel: SongsScreenViewModel = hiltViewModel(),
    artistsViewModel: ArtistsScreenViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()

    val albums by albumsViewModel.searchResults.collectAsStateWithLifecycle()
    val songs by songsViewModel.searchResults.collectAsStateWithLifecycle()
    val artists by artistsViewModel.searchResults.collectAsStateWithLifecycle()

    var searchQuery by rememberSaveable { mutableStateOf("") }

    var selectedSong by remember { mutableStateOf(MediaItem.EMPTY) }
    var showSongDialog by remember { mutableStateOf(false) }

    val tabs = listOf(
        stringResource(R.string.Albums),
        stringResource(R.string.songs),
        stringResource(R.string.Artists)
    )
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val (searchFocusRequester, focusRequester) = remember { FocusRequester.createRefs() }
    val searchInteractionSource = remember { MutableInteractionSource() }
    val isSearchFocused by searchInteractionSource.collectIsFocusedAsState()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier
            .fillMaxSize()
            .focusGroup()
            .focusRequester(focusRequester)
            .focusRestorer(focusRequester),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item(
            span = { GridItemSpan(5) }
        ) {
            Surface(
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.inverseOnSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.inverseOnSurface,
                    pressedContainerColor = MaterialTheme.colorScheme.inverseOnSurface,
                    focusedContentColor = MaterialTheme.colorScheme.onSurface,
                    pressedContentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = ClickableSurfaceDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(
                            width = if (isSearchFocused) 2.dp else 1.dp,
                            color = animateColorAsState(
                                targetValue = if (isSearchFocused) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.border,
                                label = ""
                            ).value
                        )
                    )
                ),
                tonalElevation = 2.dp,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(top = 8.dp),
                onClick = { searchFocusRequester.requestFocus() }
            ) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { updatedQuery -> searchQuery = updatedQuery },
                    decorationBox = {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 16.dp)
                                .padding(start = 20.dp),
                        ) {
                            it()
                            if (searchQuery.isEmpty()) {
                                Text(
                                    modifier = Modifier.graphicsLayer { alpha = 0.6f },
                                    text = "Anything",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 4.dp,
                            horizontal = 8.dp
                        )
                        .focusRequester(searchFocusRequester),
                    cursorBrush = Brush.verticalGradient(
                        colors = listOf(
                            LocalContentColor.current,
                            LocalContentColor.current,
                        )
                    ),
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            albumsViewModel.search(searchQuery)
                            songsViewModel.search(searchQuery)
                            artistsViewModel.onSearchQueryChange(searchQuery)
                        }
                    ),
                    maxLines = 1,
                    textStyle = MaterialTheme.typography.titleSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        item(
            key = "search_tab_row",
            span = { GridItemSpan(5) }
        ) {
            TabRow(
                modifier = Modifier.fillMaxWidth().focusGroup().focusRestorer(),
                selectedTabIndex = selectedTabIndex
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onFocus = { selectedTabIndex = index },
                        onClick = { selectedTabIndex = index },
                    ) {
                        Text(
                            text = title,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }

        when (selectedTabIndex) {
            0 -> {
                items(
                    items = albums,
                    key = { album -> album.mediaMetadata.extras?.getString("navidromeID") ?: album.mediaId },
                ) {
                    TvAlbumCard(
                        album = it,
                        modifier = Modifier.onFocusChanged {
                            focusRequester.saveFocusedChild()
                        },
                        onClick = {
                            val albumEncoded = it.toAlbum()
                            val encodedImage = URLEncoder.encode(albumEncoded.coverArt, "UTF-8")
                            navHostController.navigate(Screen.AlbumDetails.route + "/${albumEncoded.navidromeID}/$encodedImage") {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
            1 -> {
                itemsIndexed(
                    items = songs,
                    key = { _, song -> song.mediaMetadata.extras?.getString("navidromeID") ?: song.mediaId },
                    span = { _, _ -> GridItemSpan(5) }
                ) { index, song ->
                    TvHorizontalSongCard(
                        song = song,
                        modifier = Modifier.onFocusChanged {
                            focusRequester.saveFocusedChild()
                        },
                        onClick = {
                            coroutineScope.launch {
                                SongHelper.play(songs, index, mediaController)
                                navHostController.navigate(Screen.NowPlayingLandscape.route) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        onLongClick = {
                            selectedSong = song
                            showSongDialog = true
                        }
                    )
                }
            }
            2 -> {
                items(
                    items = artists,
                    key = { artist -> artist.navidromeID }
                ) {
                    TvArtistCard(
                        artist = it,
                        modifier = Modifier.onFocusChanged {
                            focusRequester.saveFocusedChild()
                        },
                        onClick = {
                            focusRequester.saveFocusedChild()
                            artistsViewModel.setSelectedArtist(it)
                            navHostController.navigate(Screen.ArtistDetails.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    }

    if (showSongDialog)
        SongDialog(
            song = selectedSong,
            setShowDialog = { showSongDialog = it }
        )
}
