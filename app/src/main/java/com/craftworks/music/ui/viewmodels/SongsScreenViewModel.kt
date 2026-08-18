package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.StarRating
import com.craftworks.music.data.datasource.navidrome.NavidromeDataSource
import com.craftworks.music.data.model.SongSortOrder
import com.craftworks.music.data.repository.SongRepository
import com.craftworks.music.managers.DataRefreshManager
import com.craftworks.music.managers.settings.LocalDataSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongsScreenViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val localDataSettingsManager: LocalDataSettingsManager
) : ViewModel() {

    private val _allSongs = MutableStateFlow<List<MediaItem>>(emptyList())
    val allSongs: StateFlow<List<MediaItem>> = _allSongs.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val searchResults: StateFlow<List<MediaItem>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    private val _songSortOrder = MutableStateFlow(SongSortOrder.DEFAULT)
    val songSortOrder: StateFlow<SongSortOrder> = _songSortOrder.asStateFlow()

    // Session cache: pages already fetched per (sort mode, favorites) pair.
    // Switching back to a previously used combination replays without network.
    private val sessionCache = mutableMapOf<Pair<SongSortOrder, Boolean>, MutableList<MediaItem>>()
    private val reachedEnd = mutableSetOf<Pair<SongSortOrder, Boolean>>()

    init {
        viewModelScope.launch {
            combine(
                localDataSettingsManager.songSortOrder,
                localDataSettingsManager.showFavoriteOnly
            ) { order, favorites -> order to favorites }
                .collect { (order, favorites) ->
                    _songSortOrder.value = order
                    _showFavoritesOnly.value = favorites
                    onDisplayConfigChanged()
                }
        }
        viewModelScope.launch {
            DataRefreshManager.dataSourceChangedEvent.collect {
                // Server/library selection changed: cached pages are stale.
                sessionCache.clear()
                reachedEnd.clear()
                getSongs()
            }
        }
    }

    private fun onDisplayConfigChanged() {
        val key = _songSortOrder.value to _showFavoritesOnly.value
        val cached = sessionCache[key]
        if (!cached.isNullOrEmpty())
            _allSongs.value = cached.toList()
        else
            getSongs()
    }

    fun getSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            coroutineScope {
                val key = _songSortOrder.value to _showFavoritesOnly.value
                val songs = songRepository.getSongs(
                    ignoreCachedResponse = true,
                    favoritesOnly = _showFavoritesOnly.value,
                    sortOrder = _songSortOrder.value
                )
                sessionCache[key] = songs.toMutableList()
                _allSongs.value = songs
            }
            _isLoading.value = false
        }
    }

    fun getMoreSongs(size: Int){
        viewModelScope.launch {
            val key = _songSortOrder.value to _showFavoritesOnly.value
            // /api/song signals the end with a short (or empty) page; once
            // reached, stop fetching for this combination. The Default
            // (search3) path keeps its original behavior.
            if (key.first != SongSortOrder.DEFAULT && key in reachedEnd) return@launch
            _isLoading.value = true
            coroutineScope {
                val songOffset = _allSongs.value.size
                val more = songRepository.getSongs(
                    songCount = size,
                    songOffset = songOffset,
                    favoritesOnly = _showFavoritesOnly.value,
                    sortOrder = _songSortOrder.value
                )
                sessionCache.getOrPut(key) { _allSongs.value.toMutableList() }.addAll(more)
                _allSongs.value += more
                if (key.first != SongSortOrder.DEFAULT && more.size < NavidromeDataSource.API_SONG_PAGE_SIZE)
                    reachedEnd.add(key)
            }
            _isLoading.value = false
        }
    }

    fun setSortOrder(sortOrder: SongSortOrder) {
        viewModelScope.launch {
            localDataSettingsManager.saveSongSortOrder(sortOrder)
        }
    }

    fun search(query: String){
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            coroutineScope {
                _searchResults.value = songRepository.searchSongs(query)
            }
            _isLoading.value = false
        }
    }
    fun setShowFavoritesOnly(showFavorites: Boolean) {
        viewModelScope.launch {
            localDataSettingsManager.saveShowFavoriteOnly(showFavorites)
        }
    }

    fun setSongRating(
        songId: String,
        rating: Int,
    ) {
        val song =_allSongs.value.firstOrNull {
            it.mediaMetadata.extras?.getString("navidromeID") == songId
        } ?: _searchResults.value.first {
            it.mediaMetadata.extras?.getString("navidromeID") == songId
        }

        val maxStars = (song.mediaMetadata.userRating as? StarRating)?.maxStars ?: 5

        val updatedSong = song.buildUpon().setMediaMetadata(
            song.mediaMetadata.buildUpon()
                .setUserRating(StarRating(maxStars, rating.toFloat()))
                .build()
        ).build()

        _allSongs.value = _allSongs.value.map { item ->
            if (item.mediaId == song.mediaId) updatedSong else item
        }
        _searchResults.value = _searchResults.value.map { item ->
            if (item.mediaId == song.mediaId) updatedSong else item
        }

        viewModelScope.launch {
            songRepository.setSongRating(songId, rating)
        }
    }
}
