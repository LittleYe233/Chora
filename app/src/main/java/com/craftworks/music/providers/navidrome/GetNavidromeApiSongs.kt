package com.craftworks.music.providers.navidrome

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.craftworks.music.data.datasource.navidrome.NavidromeDataSource
import com.craftworks.music.data.model.Artists
import com.craftworks.music.data.model.Genre
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.data.model.toMediaItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.math.roundToInt

// DTOs for Navidrome's native /api/song endpoint. Only fields consumed by the
// app are declared; everything else is skipped via ignoreUnknownKeys.
@Serializable
data class NavidromeApiSong(
    val id: String = "",
    val title: String = "",
    val album: String? = null,
    val albumId: String? = null,
    val artist: String? = null,
    val artistId: String? = null,
    val trackNumber: Int? = 0,
    val discNumber: Int? = 0,
    val year: Int? = 0,
    val genre: String? = "",
    val genres: List<ApiGenre>? = null,
    val suffix: String? = "",
    val duration: Double? = 0.0,
    val bitRate: Int? = 0,
    val size: Long? = 0,
    val path: String? = "",
    val createdAt: String? = "",
    val hasCoverArt: Boolean? = false,
    val starred: JsonPrimitive? = null,
    val participants: ApiParticipants? = null
)

@Serializable
data class ApiGenre(val id: String? = "", val name: String? = "")

@Serializable
data class ApiParticipants(val artist: List<ApiParticipant>? = null)

@Serializable
data class ApiParticipant(val id: String? = "", val name: String? = "")

private val apiJson = Json { ignoreUnknownKeys = true }

/**
 * Parses a `/api/song` JSON array into [MediaItem]s. Server-provided order is
 * preserved as-is: the caller relies on `_sort`/`_order` for ordering, so no
 * client-side re-sorting happens here.
 */
@OptIn(UnstableApi::class)
fun parseNavidromeApiSongsJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String,
): List<MediaItem> {
    val apiSongs = apiJson.decodeFromString<List<NavidromeApiSong>>(response)

    // Playback and artwork still go through the Subsonic endpoints, which
    // authenticate with a per-request salt + MD5 token (same as search3 lists).
    val passwordSalt = NavidromeDataSource.generateSalt(8)
    val passwordHash = NavidromeDataSource.md5Hash(navidromePassword + passwordSalt)

    return apiSongs.map { dto ->
        val media =
            "$navidromeUrl/rest/stream.view?&id=${dto.id}&u=$navidromeUsername&t=$passwordHash&s=$passwordSalt&v=1.12.0&c=Chora"
        val coverArt =
            if (dto.hasCoverArt == true)
                "$navidromeUrl/rest/getCoverArt.view?&id=${dto.id}&u=$navidromeUsername&t=$passwordHash&s=$passwordSalt&v=1.16.1&c=Chora&size=128"
            else
                null

        MediaData.Song(
            navidromeID = dto.id,
            parent = "",
            title = dto.title,
            album = dto.album ?: "",
            artist = dto.artist ?: "",
            artists = dto.participants?.artist?.map { Artists(it.id, it.name) } ?: listOf(),
            track = dto.trackNumber ?: 0,
            year = dto.year ?: 0,
            genre = dto.genre ?: "",
            imageUrl = coverArt,
            size = dto.size?.toInt() ?: 0,
            format = dto.suffix ?: "",
            duration = (dto.duration ?: 0.0).roundToInt(),
            bitrate = dto.bitRate ?: 0,
            path = dto.path ?: "",
            dateAdded = dto.createdAt ?: "",
            albumId = dto.albumId ?: "",
            artistId = dto.artistId ?: "",
            discNumber = dto.discNumber ?: 0,
            genres = dto.genres?.map { Genre(it.name) },
            starred = dto.starred?.contentOrNull,
            media = media
        ).toMediaItem()
    }
}
