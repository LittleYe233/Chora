package com.craftworks.music.player

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Timeline
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.source.CompositeMediaSource
import androidx.media3.exoplayer.source.ForwardingTimeline
import androidx.media3.exoplayer.source.MediaPeriod
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.Allocator

class DurationForcingMediaSource(
    private val mediaSource: MediaSource,
    private val forcedDurationUs: Long
) : CompositeMediaSource<Void?>() {

    init {
        Log.d("DurationForcing", "Initialized for media ${mediaSource.mediaItem.mediaId} with duration $forcedDurationUs")
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
        Log.d("DurationForcing", "Refreshed timeline for ${mediaSource.mediaItem.mediaId}")
        val forwardedTimeline = object : ForwardingTimeline(newTimeline) {
            override fun getWindow(
                windowIndex: Int,
                window: Timeline.Window,
                defaultPositionProjectionUs: Long
            ): Timeline.Window {
                val originalWindow = super.getWindow(windowIndex, window, defaultPositionProjectionUs)
                Log.d("DurationForcing", "Forcing duration: $forcedDurationUs (was ${originalWindow.durationUs})")
                originalWindow.durationUs = forcedDurationUs
                originalWindow.isDynamic = false
                originalWindow.isSeekable = true
                // In Media3, isLive() is derived from liveConfiguration != null.
                // To set isLive = false, we must clear the liveConfiguration.
                if (originalWindow.liveConfiguration != null) {
                    // Create a non-live configuration if needed, or null to indicate not live.
                    // Setting it to null makes isLive() return false.
                    originalWindow.liveConfiguration = null
                }
                return originalWindow
            }

            override fun getPeriod(periodIndex: Int, period: Timeline.Period, setIds: Boolean): Timeline.Period {
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
}
