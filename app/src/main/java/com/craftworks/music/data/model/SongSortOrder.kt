package com.craftworks.music.data.model

/**
 * Sort modes for the Songs screen.
 *
 * [key] is the DataStore persistence key; [apiSort]/[apiOrder] map to the
 * Navidrome `/api/song` `_sort`/`_order` query parameters. `null` marks the
 * Default mode, which keeps the existing Subsonic `search3` code path.
 */
enum class SongSortOrder(val key: String, val apiSort: String?, val apiOrder: String?) {
    DEFAULT("default", null, null),
    TITLE("title", "title", "ASC"),
    ALBUM("album", "album", "ASC"),
    ARTIST("artist", "artist", "ASC"),
    ADDED_ASC("added_asc", "created_at", "ASC"),
    ADDED_DESC("added_desc", "created_at", "DESC")
}
