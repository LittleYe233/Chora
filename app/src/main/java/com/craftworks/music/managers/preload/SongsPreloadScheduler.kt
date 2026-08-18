package com.craftworks.music.managers.preload

import android.util.Log
import androidx.media3.common.MediaItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Coroutine task pool for the all-songs list preloader.
 *
 * Every enqueued page is one atomic task launched immediately as its own
 * coroutine — there is no concurrency cap ("preload N pages, run N fetches").
 * Priority is realized through launch order: pages closer to the current
 * viewport page are launched first.
 *
 * [shutdown] cancels the whole scope: queued tasks vanish and in-flight
 * fetches are cancelled, so their pages simply revert to "missing" in the
 * owning [SongsPageStore]. A failed fetch is retried once, then abandoned
 * (the next window re-evaluation may pick it up again).
 */
class SongsPreloadScheduler(
    private val fetchPage: suspend (pageIndex: Int) -> List<MediaItem>,
    private val onPageLoaded: (pageIndex: Int, songs: List<MediaItem>) -> Unit,
    private val onPageFailed: (pageIndex: Int) -> Unit = {},
) {
    companion object {
        private const val TAG = "SongsPreload"
        private const val MAX_ATTEMPTS = 2
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()

    /** Pages currently queued or in flight. */
    private val active = ConcurrentHashMap.newKeySet<Int>()

    /** Pages this scheduler instance has successfully delivered. */
    private val completed = ConcurrentHashMap.newKeySet<Int>()

    private val attempts = ConcurrentHashMap<Int, Int>()

    @Volatile
    private var topPage: Int = 0

    /** Seeds the viewport anchor used for priority ordering. */
    fun start(initialTopPage: Int) {
        topPage = initialTopPage
    }

    /** Updates the viewport anchor; affects the launch order of later enqueues. */
    fun onViewportPageChanged(page: Int) {
        topPage = page
    }

    /**
     * Launches every not-yet-active page, nearest to the viewport first.
     * Deduplicates against in-flight and already-completed pages.
     */
    fun enqueue(pages: Collection<Int>) {
        val toStart = mutableSetOf<Int>()
        synchronized(lock) {
            pages.forEach { page ->
                if (page !in active && page !in completed) {
                    active.add(page)
                    toStart.add(page)
                }
            }
        }
        if (toStart.isEmpty()) return

        val ordered = toStart.sortedWith(compareBy({ kotlin.math.abs(it - topPage) }, { it }))
        Log.d(TAG, "Enqueueing pages $ordered (topPage=$topPage)")

        ordered.forEach { page -> scope.launch { runPage(page) } }
    }

    /** Cancels all queued and in-flight work. The instance is dead afterwards. */
    fun shutdown() {
        Log.d(TAG, "Scheduler shutdown")
        scope.cancel()
    }

    private suspend fun runPage(page: Int) {
        try {
            val songs = fetchPage(page)
            completed.add(page)
            active.remove(page)
            onPageLoaded(page, songs)
        } catch (e: CancellationException) {
            // Scope shut down mid-flight: the page stays missing by design.
            throw e
        } catch (e: Exception) {
            active.remove(page)
            val attempt = attempts.merge(page, 1, Int::plus) ?: 1
            if (attempt < MAX_ATTEMPTS) {
                Log.w(TAG, "Page $page fetch failed (attempt $attempt): ${e.message}; retrying")
                enqueue(listOf(page))
            } else {
                attempts.remove(page)
                Log.w(TAG, "Page $page fetch failed after $MAX_ATTEMPTS attempts; leaving it missing")
                onPageFailed(page)
            }
        }
    }
}
