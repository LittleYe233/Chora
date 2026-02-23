# Chora Development Progress

## Session: fix-song-duration-missing (2026-02-22 -> 2026-02-23)

### Current Working Status
- **Issue**: Progress bar loops (00:00 to ~00:05) when playing transcoded songs from Navidrome.
- **Root Cause**: ExoPlayer's `Timeline` reports `TIME_UNSET` for transcoded streams (e.g., Opus), overriding the valid metadata duration.
- **Status**: **RESOLVED**. The issue is fixed by implementing a custom `DurationForcingMediaSource` that overrides both `Window` (UI) and `Period` (internal seeking) to enforce metadata duration.
- **Build Status**: `assembleDebug` successful.

### Changes Made
1.  **`MusicService.kt`**:
    -   Initially attempted `ClippingConfiguration` on `MediaItem` (found to be insufficient).
    -   Removed `ClippingConfiguration` logic.
    -   Updated `initializePlayer` to use a custom `MediaSource.Factory` that wraps sources in `DurationForcingMediaSource` when duration metadata is available.
    -   Added debug logging to track duration availability.
2.  **`DurationForcingMediaSource.kt` (New File)**:
    -   Implemented `DurationForcingMediaSource` extending `CompositeMediaSource`.
    -   Created a `ForwardingTimeline` wrapper that overrides:
        *   `getWindow`: Sets `durationUs`, `isDynamic = false`, `isSeekable = true`, and `liveConfiguration = null` (effectively `isLive = false`).
        *   `getPeriod`: Sets `durationUs` to match the window duration (crucial for internal seeking logic).
3.  **`NowPlayingElements.kt`**:
    -   Updated `PlaybackProgressSlider` to fallback to `metadata.duration` if `player.duration` is invalid.
    -   Cleaned up debug logs.
4.  **`NowPlayingPortrait.kt` & `NowPlayingLandscape.kt`**:
    -   Updated to pass `metadata` object to `PlaybackProgressSlider`.

### Mistakes Made & Lessons Learned
1.  **Insufficient Fix with `ClippingConfiguration`**:
    *   **Error**: Applied `ClippingConfiguration` to `MediaItem`, hoping it would override the player's timeline duration. This didn't work because the underlying `MediaPeriod` still reported `TIME_UNSET`, causing the timeline to remain broken.
    *   **Correction**: Realized that `ClippingConfiguration` is not enough when the stream itself lacks duration metadata. Needed to intervene at the `Timeline` level.
    *   **Rule**: Fix the source of the data (Timeline/Period) rather than applying a clipping overlay that still depends on that data.

2.  **Incomplete Timeline Override**:
    *   **Error**: Implemented `ForwardingTimeline` overriding only `getWindow`. The progress bar showed the correct duration (maybe), but seeking still failed or looped because internal player logic relies on `getPeriod`.
    *   **Correction**: Overrode both `getWindow` (for UI) and `getPeriod` (for internal seeking) to ensure consistency.
    *   **Rule**: When customizing ExoPlayer behavior via Timeline, ensure you cover both the public API (`Window`) and the internal API (`Period`).

3.  **Read-Only Property Assignment**:
    *   **Error**: Attempted to set `window.isLive = false` directly, resulting in a compilation error (`val cannot be reassigned`).
    *   **Correction**: Investigated `Timeline.Window` properties and found `isLive` is a derived property. Set `window.liveConfiguration = null` instead to achieve the desired state.
    *   **Rule**: Consult the class definition (source/javadoc) when encountering assignment errors; look for backing fields or setters rather than assuming the property is mutable.

### Strict Caution for Future Work
**MUST DISTILL THE MISTAKES WRITTEN IN THIS PROGRESS FILE AND NEVER MAKE THEM AGAIN IN THE FUTURE.**
-   Do not rely on high-level APIs (`ClippingConfiguration`) to fix low-level data issues (`TIME_UNSET`). Intervene at the data source/timeline level.
-   Always synchronize `Window` and `Period` overrides when customizing ExoPlayer timelines.
-   Verify property mutability before attempting assignment; look for backing fields or configuration objects.

### Pending Tasks
-   None (User testing required).
