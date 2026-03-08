package com.craftworks.music.ui.screens.tv

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
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
    val tabs = listOf(
        stringResource(R.string.recently_added),
        stringResource(R.string.recently_played),
        stringResource(R.string.most_played),
        stringResource(R.string.Label_Sort_Starred),
    )
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val gridState = rememberLazyGridState()

    if (NavidromeManager.checkActiveServers()) {
        LaunchedEffect(albums.size) {
            if (albums.size % 50 != 0) return@LaunchedEffect
            if (albums.size < 50) return@LaunchedEffect

            snapshotFlow {
                val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow false
                val total = gridState.layoutInfo.totalItemsCount
                if (total < albums.size - 5) return@snapshotFlow false
                (total - lastVisible) <= 15
            }.filter { it }.collect {
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
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(20.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp),
    ) {
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
                                viewModel.setSorting(
                                    when (index) {
                                        0 -> SortOrder.NEWEST
                                        1 -> SortOrder.RECENT
                                        2 -> SortOrder.FREQUENT
                                        else -> SortOrder.STARRED
                                    }
                                )
                                      },
                        ) {
                            Text(
                                text = tab,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
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