package com.craftworks.music.data.model

import android.util.Log
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

// Universal Lyric object
@Stable
data class Lyric(
    val timestamp: Int,
    val content: List<String>,
)

fun List<Lyric>.groupLyrics(): List<Lyric> {
    return this.groupBy { it.timestamp }
        .map { (timestamp, lyrics) ->
            Lyric(
                timestamp = timestamp,
                content = lyrics.flatMap { it.content }
            )
        }
        .sortedBy { it.timestamp }
}


// LRCLIB Lyrics
@Serializable
data class LrcLibLyrics(
    val id: Int,
    val instrumental: Boolean,
    val plainLyrics: String? = "",
    val syncedLyrics: String? = ""
)

//region Convert proprietary lyric format to app format.

fun MediaData.PlainLyrics.toLyric(): Lyric {
    return Lyric(
        timestamp = -1,
        content = if (value.isBlank()) emptyList() else listOf(value)
    )
}

fun MediaData.StructuredLyrics.toLyrics(): List<Lyric> {
    return line.map { syncedLyric ->
        Lyric(
            // If not synced lyrics, set timestamp to -1
            timestamp = if (synced) syncedLyric.start + (offset ?: 0) else -1,
            content = listOf(syncedLyric.value)
        )
    }
}

fun LrcLibLyrics.toLyrics(): List<Lyric> {
    if (instrumental) return listOf()

    else if (syncedLyrics.toString() != "null") {
        val result = mutableListOf<Lyric>()

        syncedLyrics?.lines()?.forEach { lyric ->
            val timeStampsRaw = getTimeStamps(lyric)
            val lyricText: String = lyric.replace(Regex("\\[(.*?)]"), "").trim()

            timeStampsRaw.forEach { timestamp ->
                val time = mmssToMilliseconds(timestamp)
                result.add(Lyric(time.toInt(), listOf(lyricText)))
            }
        }
        
        // Sort by timestamp to handle multiple timestamps per line correctly
        result.sortBy { it.timestamp }

        Log.d("LYRICS", "Got LRCLIB synced lyrics: $result")
        return result
    }
    else if (plainLyrics.toString() != "null") {
        Log.d("LYRICS", "Got LRCLIB plain lyrics: $plainLyrics")
        return listOf(Lyric(-1, listOf(plainLyrics.toString())))
    }
    else
        return listOf()
}

//endregion

fun mmssToMilliseconds(mmss: String): Long {
    val parts = mmss.split(":", ".")
    if (parts.size == 3) {
        try {
            val minutes = parts[0].toLong()
            val seconds = parts[1].toLong()
            val ms = parts[2].toLong()
            return (minutes * 60 + seconds) * 1000 + ms * 10
        } catch (e: NumberFormatException) {
            // Handle the case where the input is not in the expected format
            e.printStackTrace()
        }
    }
    return 0L
}

fun getTimeStamps(input: String): List<String> {
    val regex = Regex("\\[(.*?)]")
    val matches = regex.findAll(input)

    val result = mutableListOf<String>()
    for (match in matches) {
        result.add(match.groupValues[1])
    }

    return result
}