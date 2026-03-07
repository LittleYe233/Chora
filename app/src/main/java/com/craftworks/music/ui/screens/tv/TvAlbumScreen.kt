package com.craftworks.music.ui.screens.tv

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.data.model.toAlbum
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.ui.elements.tv.TvAlbumCard
import com.craftworks.music.ui.viewmodels.AlbumScreenViewModel
import kotlinx.coroutines.flow.filter
import java.net.URLEncoder

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvAlbumScreen(
    navHostController: NavHostController = rememberNavController(),
    viewModel: AlbumScreenViewModel = hiltViewModel(),
) {
    val albums by viewModel.allAlbums.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = 48.dp, top = 24.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.rounded_sort_24),
                        contentDescription = stringResource(R.string.Label_Sorting),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { androidx.compose.material3.Text(stringResource(R.string.Label_Sort_Alphabetical)) },
                        onClick = {
                            viewModel.setSorting(SortOrder.ALPHABETICAL); showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { androidx.compose.material3.Text(stringResource(R.string.recently_added)) },
                        onClick = { viewModel.setSorting(SortOrder.NEWEST); showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { androidx.compose.material3.Text(stringResource(R.string.recently_played)) },
                        onClick = { viewModel.setSorting(SortOrder.RECENT); showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { androidx.compose.material3.Text(stringResource(R.string.most_played)) },
                        onClick = { viewModel.setSorting(SortOrder.FREQUENT); showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { androidx.compose.material3.Text(stringResource(R.string.Label_Sort_Starred)) },
                        onClick = { viewModel.setSorting(SortOrder.STARRED); showSortMenu = false }
                    )
                }
            }
        }
        val gridState = rememberLazyGridState()

        if (NavidromeManager.checkActiveServers()) {
            LaunchedEffect(gridState) {
                if (albums.size % 50 != 0) return@LaunchedEffect

                snapshotFlow {
                    val lastVisibleItemIndex =
                        gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                    val totalItemsCount = gridState.layoutInfo.totalItemsCount

                    lastVisibleItemIndex != null && totalItemsCount > 0 &&
                            (totalItemsCount - lastVisibleItemIndex) <= 10
                }
                    .filter { it }
                    .collect {
                        viewModel.getMoreAlbums(50)
                    }
            }
        }

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .fillMaxSize()
                .focusGroup()
                .focusRestorer(),
            contentPadding = PaddingValues(
                start = 48.dp,
                end = 48.dp,
                bottom = 48.dp
            ),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(20.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp),
        ) {
            items(albums) { album ->
                TvAlbumCard(
                    album = album,
                    onClick = {
                        val albumEncoded = album.toAlbum()
                        val encodedImage = URLEncoder.encode(albumEncoded.coverArt, "UTF-8")
                        navHostController.navigate(Screen.AlbumDetails.route + "/${albumEncoded.navidromeID}/$encodedImage") {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}