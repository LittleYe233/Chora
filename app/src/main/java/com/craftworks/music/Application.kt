package com.craftworks.music

import android.app.Application
import com.craftworks.music.managers.LocalProviderManager
import com.craftworks.music.managers.NavidromeAuthManager
import com.craftworks.music.managers.NavidromeManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ChoraApplication : Application(){
    @Inject lateinit var navidromeAuthManager: NavidromeAuthManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NavidromeManager.init(this)
        LocalProviderManager.init(this)

        // Best-effort background validation of the stored native-API token:
        // probe with a /api/keepalive/0 heartbeat, re-login on 401.
        applicationScope.launch {
            val server = NavidromeManager.getCurrentServer() ?: return@launch
            navidromeAuthManager.probeStoredToken(server)
        }
    }
}
