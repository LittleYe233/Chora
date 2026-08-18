package com.craftworks.music.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.StarRating
import com.craftworks.music.data.datasource.navidrome.NavidromeDataSource
import com.craftworks.music.data.model.SongSortOrder
import com.craftworks.music.data.repository.SongRepository
import com.craftworks.music.managers.DataRefreshManager
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.managers.preload.SongsPageStore
import com.craftworks.music.managers.preload.SongsPreloadScheduler
import com.craftworks.music.managers.settings.LocalDataSettingsManager
import com.craftworks.music.managers.settings.PlaybackSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongsScreenViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val localDataSettingsManager: LocalDataSettingsManager,
    private val playbackSettingsManager: PlaybackSettingsManager
) : ViewModel() {

    companion object {
        private const val TAG = "SongsPreload"
    }

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

    // True when every song of the active combination is loaded (footer + snackbar).
    private val _allSongsLoaded = MutableStateFlow(false)
    val allSongsLoaded: StateFlow<Boolean> = _allSongsLoaded.asStateFlow()

    private val _totalSongCount = MutableStateFlow(0)
    val totalSongCount: StateFlow<Int> = _totalSongCount.asStateFlow()

    // One-shot (server count, loaded count) pair for the "library fully loaded" snackbar.
    private val _libraryLoadedEvent = Channel<Pair<Int?, Int>>(Channel.BUFFERED)
    val libraryLoadedEvent = _libraryLoadedEvent.receiveAsFlow()

    // Session cache: pages already fetched per (sort mode, favorites) pair.
    // Switching back to a previously used combination replays without network.
    private val sessionCache = mutableMapOf<Pair<SongSortOrder, Boolean>, MutableList<MediaItem>>()
    private val reachedEnd = mutableSetOf<Pair<SongSortOrder, Boolean>>()

    // Preloader state.
    private val pageStores = mutableMapOf<Pair<SongSortOrder, Boolean>, SongsPageStore>()
    private var scheduler: SongsPreloadScheduler? = null
    private var screenEntered = false
    private var currentTopPage = 0
    private var preloadEnabled = false
    private var preloadPages = 10

    // Server-reported library size (getScanStatus); null while unknown.
    private var librarySongCount: Int? = null

    // Keys whose completion snackbar already fired this load cycle.
    private val announcedKeys = mutableSetOf<Pair<SongSortOrder, Boolean>>()

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
            combine(
                playbackSettingsManager.songsPreloadEnabled,
                playbackSettingsManager.songsPreloadPages
            ) { enabled, pages -> enabled to pages }
                .collect { (enabled, pages) -> onPreloadSettingsChanged(enabled, pages) }
        }
        viewModelScope.launch {
            // Server-reported library size clamps the preload page range.
            // Arriving late (startup fetch) re-evaluates the open window.
            NavidromeManager.librarySongCount.collect { count ->
                val wasNull = librarySongCount == null
                librarySongCount = count
                if (count != null) {
                    // Backfill stores created before the count arrived
                    // (non-favorites combinations only — the count describes
                    // the whole library, not the starred subset).
                    pageStores.forEach { (key, store) ->
                        if (!key.second && store.expectedTotal == null) {
                            store.expectedTotal = count
                            republishFromStore(key, store)
                        }
                    }
                }
                if (wasNull && count != null) evaluatePreloadWindow()
            }
        }
        viewModelScope.launch {
            DataRefreshManager.dataSourceChangedEvent.collect {
                // Server/library selection changed: cached pages are stale.
                shutdownScheduler()
                pageStores.clear()
                announcedKeys.clear()
                _allSongsLoaded.value = false
                sessionCache.clear()
                reachedEnd.clear()
                getSongs()
            }
        }
    }

    private fun currentKey(): Pair<SongSortOrder, Boolean> = _songSortOrder.value to _showFavoritesOnly.value

    // getStarred.view always returns the complete favorite set in one call,
    // so there is nothing to page or preload.
    private fun isPagerEligible(key: Pair<SongSortOrder, Boolean>): Boolean =
        NavidromeManager.checkActiveServers() && key != SongSortOrder.DEFAULT to true

    private fun isLocalSong(song: MediaItem): Boolean =
        song.mediaMetadata.extras?.getString("navidromeID")?.startsWith("Local_") == true

    //region Preload lifecycle

    /** Called by the screen when it enters composition (navigated to). */
    fun onScreenEntered() {
        screenEntered = true
        val key = currentKey()
        val store = pageStores[key] ?: return
        maybeStartScheduler(key, store)
    }

    /** Called by the screen when it leaves composition (navigated away). */
    fun onScreenExited() {
        screenEntered = false
        shutdownScheduler()
    }

    /** Called by the list whenever the topmost visible remote song moves to another page. */
    fun onViewportTopPageChanged(page: Int) {
        if (page == currentTopPage) return
        currentTopPage = page
        scheduler?.onViewportPageChanged(page)
        evaluatePreloadWindow()
    }

    private fun onPreloadSettingsChanged(enabled: Boolean, pages: Int) {
        val wasEnabled = preloadEnabled
        preloadEnabled = enabled
        preloadPages = pages
        if (enabled == wasEnabled) {
            if (enabled) evaluatePreloadWindow()   // window size changed
            return
        }
        if (enabled) {
            // Off -> on mid-session: seed a page store from the legacy cache
            // so already-loaded songs are not re-fetched.
            val key = currentKey()
            val store = pageStores.getOrPut(key) { SongsPageStore() }
            if (!key.second) store.expectedTotal = librarySongCount
            if (store.pages.isEmpty()) {
                val cached = sessionCache[key]
                if (!cached.isNullOrEmpty()) {
                    if (key == SongSortOrder.DEFAULT to true)
                        store.seedChunked(cached.filter(::isLocalSong), cached.filterNot(::isLocalSong), complete = true)
                    else
                        store.seedChunked(cached.filter(::isLocalSong), cached.filterNot(::isLocalSong))
                    republishFromStore(key, store)
                }
            }
            maybeStartScheduler(key, store)
        } else {
            shutdownScheduler()
            _allSongsLoaded.value = false
        }
    }

    private fun maybeStartScheduler(key: Pair<SongSortOrder, Boolean>, store: SongsPageStore) {
        if (!preloadEnabled || !screenEntered || !isPagerEligible(key)) return
        if (store.fullyLoaded) {
            shutdownScheduler()
            return
        }
        // Never recreate a live scheduler: that would cancel in-flight page
        // fetches and re-issue duplicate requests.
        if (scheduler == null) startScheduler(key, store)
        evaluatePreloadWindow()
    }

    private fun startScheduler(key: Pair<SongSortOrder, Boolean>, store: SongsPageStore) {
        shutdownScheduler()
        val pageSize = NavidromeDataSource.API_SONG_PAGE_SIZE
        scheduler = SongsPreloadScheduler(
            fetchPage = { page ->
                songRepository.getSongs(
                    songCount = pageSize,
                    songOffset = page * pageSize,
                    favoritesOnly = key.second,
                    sortOrder = key.first
                )
            },
            onPageLoaded = { page, songs ->
                viewModelScope.launch { handlePageLoaded(key, store, page, songs) }
            },
            onPageFailed = { page ->
                Log.w(TAG, "Page $page failed permanently; next window evaluation will retry")
            },
        ).also { it.start(currentTopPage) }
        Log.d(TAG, "Scheduler started (key=$key, topPage=$currentTopPage)")
    }

    private fun shutdownScheduler() {
        scheduler?.shutdown()
        scheduler = null
    }

    /** Enqueues the missing pages of [topPage, topPage + preloadPages), clamped by the library size. */
    private fun evaluatePreloadWindow() {
        if (!preloadEnabled || !screenEntered) return
        val key = currentKey()
        if (!isPagerEligible(key)) return
        val store = pageStores[key] ?: return
        if (store.pages.isEmpty() || store.fullyLoaded) return
        val activeScheduler = scheduler ?: return

        // With a known library count N (non-favorites only — favorites lists
        // are far smaller and end via the short-page marker), pages beyond
        // (N-1)/pageSize are guaranteed empty: never request them.
        val lastValidPage = if (!key.second)
            librarySongCount?.let { n -> if (n <= 0) -1 else (n - 1) / store.pageSize }
        else
            null
        val window = currentTopPage until currentTopPage + preloadPages
        val missing = store.missingPages(window)
            .filter { lastValidPage == null || it <= lastValidPage }
        if (missing.isNotEmpty()) activeScheduler.enqueue(missing)
    }
    /** Scheduler callback, hopped to the main thread. Stale (post key-switch) results are dropped. */
    private fun handlePageLoaded(key: Pair<SongSortOrder, Boolean>, store: SongsPageStore, page: Int, songs: List<MediaItem>) {
        if (pageStores[key] !== store) return
        store.storePage(page, songs)
        Log.d(TAG, "Page $page loaded: ${songs.size} songs (isLast=${songs.size < store.pageSize})")
        republishFromStore(key, store)
    }

    private fun republishFromStore(key: Pair<SongSortOrder, Boolean>, store: SongsPageStore) {
        val list = store.contiguousPrefix()
        sessionCache[key] = list.toMutableList()
        _allSongs.value = list
        _totalSongCount.value = store.localPrefix.size + store.totalRemoteCount
        if (store.fullyLoaded) markFullyLoaded(key, store)
    }

    private fun markFullyLoaded(key: Pair<SongSortOrder, Boolean>, store: SongsPageStore) {
        val total = store.localPrefix.size + store.totalRemoteCount
        _totalSongCount.value = total
        if (_allSongsLoaded.value) return
        _allSongsLoaded.value = true
        // M = remote songs actually loaded; compare against N (server count)
        // to reveal server-side filtering (missing=false) or gaps.
        val loadedRemote = store.totalRemoteCount
        Log.d(
            TAG,
            "All songs loaded: $loadedRemote songs across ${store.pages.size} pages " +
                "(key=$key, serverCount=$librarySongCount)"
        )
        if (key !in announcedKeys) {
            announcedKeys.add(key)
            _libraryLoadedEvent.trySend(librarySongCount to loadedRemote)
        }
    }

    //endregion

    private fun onDisplayConfigChanged() {
        val key = currentKey()
        shutdownScheduler()
        _allSongsLoaded.value = false
        _totalSongCount.value = 0
        val cached = sessionCache[key]
        if (!cached.isNullOrEmpty()) {
            _allSongs.value = cached.toList()
            if (preloadEnabled) {
                val store = pageStores.getOrPut(key) { SongsPageStore() }
                if (!key.second) store.expectedTotal = librarySongCount
                if (store.pages.isEmpty()) {
                    if (key == SongSortOrder.DEFAULT to true)
                        store.seedChunked(cached.filter(::isLocalSong), cached.filterNot(::isLocalSong), complete = true)
                    else
                        store.seedChunked(cached.filter(::isLocalSong), cached.filterNot(::isLocalSong))
                }
                republishFromStore(key, store)
                maybeStartScheduler(key, store)
            }
        } else {
            getSongs()
        }
    }

    fun getSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            val key = currentKey()
            val songs = songRepository.getSongs(
                ignoreCachedResponse = true,
                favoritesOnly = key.second,
                sortOrder = key.first
            )
            if (preloadEnabled) {
                // Pull-to-refresh / initial load: cancel in-flight pages first —
                // they belong to the previous (possibly stale) library state.
                shutdownScheduler()
                val local = songs.filter(::isLocalSong)
                val remote = songs.filterNot(::isLocalSong)
                val store = pageStores.getOrPut(key) { SongsPageStore() }
                if (!key.second) store.expectedTotal = librarySongCount
                if (key == SongSortOrder.DEFAULT to true)
                    store.seedChunked(local, remote, complete = true)
                else
                    store.reset(local, remote)
                _allSongsLoaded.value = false
                announcedKeys.remove(key)
                republishFromStore(key, store)
                maybeStartScheduler(key, store)
            } else {
                sessionCache[key] = songs.toMutableList()
                _allSongs.value = songs
            }
            _isLoading.value = false
        }
    }

    fun getMoreSongs(size: Int) {
        if (preloadEnabled) {
            // Pager mode: the near-bottom trigger just boosts the next
            // contiguous page to the front of the pool.
            val key = currentKey()
            if (!isPagerEligible(key)) return
            val store = pageStores[key] ?: return
            if (store.fullyLoaded) return
            maybeStartScheduler(key, store)
            scheduler?.enqueue(listOf(store.contiguousEndPage()))
            return
        }

        viewModelScope.launch {
            val key = currentKey()
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
                    favoritesOnly = key.second,
                    sortOrder = key.first
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

    override fun onCleared() {
        shutdownScheduler()
        super.onCleared()
    }
}
