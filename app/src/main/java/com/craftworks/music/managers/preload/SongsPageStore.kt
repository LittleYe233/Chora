package com.craftworks.music.managers.preload

import androidx.media3.common.MediaItem
import com.craftworks.music.data.datasource.navidrome.NavidromeDataSource

/**
 * Sparse per-page song storage backing the all-songs list preloader.
 *
 * Pages may be stored out of order (concurrent fetches complete arbitrarily);
 * the published list is always [localPrefix] plus the contiguous run of pages
 * starting at 0, so the UI never shows holes. A page returning fewer than
 * [pageSize] songs marks the library end — pages at or beyond that index are
 * never fetched again.
 *
 * Not thread-safe by itself: mutations are confined to the ViewModel's
 * main-thread scope.
 */
class SongsPageStore(val pageSize: Int = NavidromeDataSource.API_SONG_PAGE_SIZE) {

    /** Page index -> songs of that page (short/empty page = library end marker). */
    val pages = mutableMapOf<Int, List<MediaItem>>()

    /** Local songs rendered before page 0; they never participate in paging. */
    var localPrefix: List<MediaItem> = emptyList()

    /** Smallest page index known to be the (short) final page, null while unknown. */
    var endPageIndex: Int? = null
        private set

    /**
     * Server-reported library size (remote songs, non-favorites combinations
     * only). Lets [fullyLoaded] trigger without fetching a guaranteed-empty
     * page when the total is an exact multiple of [pageSize].
     */
    var expectedTotal: Int? = null

    /** Replaces the store with a fresh first-page load (pull-to-refresh / initial). */
    fun reset(localPrefix: List<MediaItem>, firstRemotePage: List<MediaItem>) {
        this.pages.clear()
        this.endPageIndex = null
        this.localPrefix = localPrefix
        storePage(0, firstRemotePage)
    }

    /**
     * Replaces the store with a contiguous chunk list (e.g. seeding from the
     * legacy session cache when preloading is toggled on mid-session). The list
     * is assumed complete iff [complete] — used for the getStarred path which
     * always delivers every favorite song in one response.
     */
    fun seedChunked(localPrefix: List<MediaItem>, remote: List<MediaItem>, complete: Boolean = false) {
        this.pages.clear()
        this.endPageIndex = null
        this.localPrefix = localPrefix
        if (remote.isEmpty()) {
            storePage(0, emptyList())
            return
        }
        remote.chunked(pageSize).forEachIndexed { index, chunk ->
            storePage(index, chunk)
            if (complete && index == (remote.size + pageSize - 1) / pageSize - 1)
                markEnd(index)
        }
    }

    /** Stores a fetched page; a short page (including empty) marks the library end. */
    fun storePage(index: Int, songs: List<MediaItem>) {
        pages[index] = songs
        if (songs.size < pageSize) markEnd(index)
    }

    /** [localPrefix] plus pages 0..k where every page 0..k is present. */
    fun contiguousPrefix(): List<MediaItem> {
        val out = ArrayList<MediaItem>(localPrefix)
        var index = 0
        while (true) {
            val page = pages[index] ?: break
            out.addAll(page)
            index++
        }
        return out
    }

    /** First page index not present in the store. */
    fun contiguousEndPage(): Int {
        var index = 0
        while (pages.containsKey(index)) index++
        return index
    }

    /** Pages in [window] that are neither stored nor beyond the known end. */
    fun missingPages(window: IntRange): List<Int> {
        val end = endPageIndex
        return window.filter { it !in pages && (end == null || it < end) }.toList()
    }

    /** Sum of all stored remote page sizes (partial while loading). */
    val totalRemoteCount: Int
        get() = pages.values.sumOf { it.size }

    /** True once the contiguous prefix covers the known final page. */
    val fullyLoaded: Boolean
        get() = (endPageIndex != null && contiguousEndPage() > endPageIndex!!) ||
                (expectedTotal != null && contiguousEndPage() * pageSize >= expectedTotal!!)

    private fun markEnd(index: Int) {
        if (endPageIndex == null || index < endPageIndex!!)
            endPageIndex = index
    }
}
