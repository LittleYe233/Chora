package com.craftworks.music.ui.elements.tv

import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Checkbox
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.ListItemScale
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.data.NavidromeProvider
import com.craftworks.music.data.repository.LyricsState
import com.craftworks.music.managers.LocalProviderManager
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import kotlinx.coroutines.launch

@Composable
private fun ProviderItem(
    icon: Int,
    title: String,
    subtitle: String,
    trailingContent: @Composable () -> Unit = { },
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = { }
) {
    ListItem(
        selected = enabled,
        scale = ListItemScale.None,
        leadingContent = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(ListItemDefaults.IconSize)
            )
        },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        trailingContent = {
            Row {
                trailingContent()

                Checkbox(
                    checked = enabled,
                    onCheckedChange = { }
                )
            }
        },
        onClick = onClick,
        onLongClick = onLongClick
    )
}

@Preview
@Composable
fun NavidromeProviderCard(
    server: NavidromeProvider = NavidromeProvider(
        "0",
        "https://demo.navidrome.org",
        "CraftWorks",
        "demo",
        enabled = true,
        allowSelfSignedCert = true
    )
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val currentServerId by NavidromeManager.currentServerId.collectAsStateWithLifecycle()

    val checked by remember { derivedStateOf { server.id == currentServerId } }

    ProviderItem(
        icon = R.drawable.s_m_navidrome,
        title = server.username,
        subtitle = server.url,
        enabled = checked,
        /*
        trailingContent = {
            IconButton(
                onClick = {
                    NavidromeManager.removeServer(server.id)
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null
                )
            }
        },
        */
        onLongClick = {
            NavidromeManager.removeServer(server.id)
        },
        onClick = {
            coroutineScope.launch {
                if (!checked && NavidromeManager.getAllServers().size == 1)
                    NavidromeManager.setCurrentServer(null)
                else
                    NavidromeManager.setCurrentServer(server.id)
                AppearanceSettingsManager(context).setUsername(server.username)
            }
            Log.d("NAVIDROME", "Navidrome Current Server: ${server.id}")
        },
    )
}

@Preview
@Composable
fun LocalProviderCard(
    folder: String = ""
) = ProviderItem(
    icon = R.drawable.s_m_navidrome,
    title = "Local",
    subtitle = folder,
    enabled = LocalProviderManager.getAllFolders().contains(folder),
    onClick = { },
)

@Preview
@Composable
fun LrcLibProviderCard() = ProviderItem(
    icon = R.drawable.lrclib_logo,
    title = "LRCLIB",
    subtitle = "",
    enabled = LyricsState.useLrcLib,
    onClick = { LyricsState.useLrcLib = !LyricsState.useLrcLib },
)

@Preview
@Composable
fun NetEaseProviderCard() = ProviderItem(
    icon = R.drawable.netease_cloud_music,
    title = "NetEase",
    subtitle = "Lyrics",
    enabled = LyricsState.useNetEase,
    onClick = { LyricsState.useNetEase = !LyricsState.useNetEase },
)