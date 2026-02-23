package com.craftworks.music.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Timeline
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.source.CompositeMediaSource
import androidx.media3.exoplayer.source.ForwardingTimeline
import androidx.media3.exoplayer.source.MediaPeriod
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.Allocator
import androidx.media3.common.util.UnstableApi

@UnstableApi
class DurationForcingMediaSource(
    private val mediaSource: MediaSource,
    private var forcedDurationUs: Long,
    private val onRelease: () -> Unit = {}
) : CompositeMediaSource<Void?>() {

    private var lastTimeline: Timeline? = null

    /**
     * Updates the forced duration and refreshes the source info if a timeline is available.
     */
    fun updateDuration(newDurationUs: Long) {
        this.forcedDurationUs = newDurationUs
        
        lastTimeline?.let { timeline ->
            // Re-process the last known timeline with the new duration
            onChildSourceInfoRefreshed(null, mediaSource, timeline)
        }
    }

    override fun getMediaItem(): MediaItem {
        return mediaSource.mediaItem
    }

    override fun prepareSourceInternal(mediaTransferListener: TransferListener?) {
        super.prepareSourceInternal(mediaTransferListener)
        prepareChildSource(null, mediaSource)
    }

    override fun onChildSourceInfoRefreshed(
        childSourceId: Void?,
        mediaSource: MediaSource,
        newTimeline: Timeline
    ) {
        lastTimeline = newTimeline

        val forwardedTimeline = object : ForwardingTimeline(newTimeline) {
            override fun getWindow(
                windowIndex: Int,
                window: Window,
                defaultPositionProjectionUs: Long
            ): Window {
                val originalWindow = super.getWindow(windowIndex, window, defaultPositionProjectionUs)

                originalWindow.durationUs = forcedDurationUs
                originalWindow.isDynamic = false
                originalWindow.isSeekable = true

                // In Media3, isLive() is derived from liveConfiguration != null.
                // To set isLive = false, we must clear the liveConfiguration.
                if (originalWindow.liveConfiguration != null) {
                    originalWindow.liveConfiguration = null
                }
                return originalWindow
            }

            override fun getPeriod(periodIndex: Int, period: Period, setIds: Boolean): Period {
                val originalPeriod = super.getPeriod(periodIndex, period, setIds)
                originalPeriod.durationUs = forcedDurationUs
                return originalPeriod
            }
        }
        refreshSourceInfo(forwardedTimeline)
    }

    override fun createPeriod(
        id: MediaSource.MediaPeriodId,
        allocator: Allocator,
        startPositionUs: Long
    ): MediaPeriod {
        return mediaSource.createPeriod(id, allocator, startPositionUs)
    }

    override fun releasePeriod(mediaPeriod: MediaPeriod) {
        mediaSource.releasePeriod(mediaPeriod)
    }

    override fun releaseSourceInternal() {
        super.releaseSourceInternal()
        onRelease()
    }
}
