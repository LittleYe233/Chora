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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.craftworks.music.data.model.Screen
import com.craftworks.music.ui.elements.tv.TvArtistCard
import com.craftworks.music.ui.viewmodels.ArtistsScreenViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvArtistScreen(
    navHostController: NavHostController = rememberNavController(),
    viewModel: ArtistsScreenViewModel = hiltViewModel(),
) {
    val allArtistList by viewModel.allArtists.collectAsStateWithLifecycle()

    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier
            .fillMaxSize()
            .focusGroup()
            .focusRestorer(),
        contentPadding = PaddingValues(48.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        items(allArtistList) { artist ->
            TvArtistCard(
                artist = artist,
                onClick = {
                    viewModel.setSelectedArtist(artist)
                    navHostController.navigate(Screen.ArtistDetails.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}