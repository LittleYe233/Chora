package com.craftworks.music.providers.navidrome

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class SubsonicScanStatusResponse(
    val status: String,
    val scanStatus: ScanStatus? = null
)

@Serializable
data class ScanStatus(
    val scanning: Boolean = false,
    val count: Int? = null,
    val folderCount: Int? = null
)

/**
 * Extracts the total library song count from a `getScanStatus` response.
 * Returns a single-element list holding `scanStatus.count`, or an empty list
 * when the response is an error / carries no count.
 */
fun parseNavidromeScanStatusJSON(response: String): List<Int> {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicScanStatusResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

    if (subsonicResponse.status != "ok") {
        Log.w("NAVIDROME", "getScanStatus returned status=${subsonicResponse.status}")
        return emptyList()
    }

    val count = subsonicResponse.scanStatus?.count ?: run {
        Log.w("NAVIDROME", "getScanStatus response has no scanStatus.count")
        return emptyList()
    }
    return listOf(count)
}
