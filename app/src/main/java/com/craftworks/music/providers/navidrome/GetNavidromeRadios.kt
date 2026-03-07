package com.craftworks.music.providers.navidrome

import android.util.Log
import com.craftworks.music.data.model.MediaData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("artists")
data class internetRadioStations(val internetRadioStation: List<MediaData.Radio>)

fun parseNavidromeRadioJSON(
    response: String
) : List<MediaData.Radio> {
    val subsonicResponse = parseSubsonicResponse(response)

    val mediaDataRadios = subsonicResponse.internetRadioStations?.internetRadioStation ?: emptyList()

    Log.d("NAVIDROME", "Added radios. Total: ${mediaDataRadios.size}")

    return mediaDataRadios
}