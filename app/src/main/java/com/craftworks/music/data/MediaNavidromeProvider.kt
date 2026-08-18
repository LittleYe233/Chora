package com.craftworks.music.data

import kotlinx.serialization.Serializable

@Serializable
data class NavidromeProvider (
    val id: String = "0",
    var url:String,
    var username:String,
    val password:String,
    val enabled:Boolean? = true,
    var allowSelfSignedCert: Boolean? = false,
    // Bearer token for Navidrome's native REST API (/auth/login). Nullable so
    // previously persisted servers without a token still deserialize.
    var jwtToken: String? = null,
    // List of library folders and if they're enabled or not.
    var libraryIds: List<Pair<NavidromeLibrary, Boolean>> = listOf(Pair(NavidromeLibrary(0, "Media Library"), true))
)

@Serializable
data class NavidromeLibrary (
    val id: Int = 0,
    var name:String,
)