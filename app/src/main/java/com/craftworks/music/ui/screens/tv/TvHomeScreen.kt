package com.craftworks.music.ui.screens.tv

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.Button
import androidx.tv.material3.Carousel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.rememberCarouselState
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.toAlbum
import com.craftworks.music.formatMilliseconds
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.tv.TvAlbumCard
import com.craftworks.music.ui.screens.HomeItem
import com.craftworks.music.ui.viewmodels.HomeScreenViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvHomeScreen(
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = null,
    viewModel: HomeScreenViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val recentlyPlayedAlbums by viewModel.recentlyPlayedAlbums.collectAsStateWithLifecycle()
    val recentAlbums by viewModel.recentAlbums.collectAsStateWithLifecycle()
    val mostPlayedAlbums by viewModel.mostPlayedAlbums.collectAsStateWithLifecycle()
    val shuffledAlbums by viewModel.shuffledAlbums.collectAsStateWithLifecycle()

    val libraries by NavidromeManager.libraries.collectAsStateWithLifecycle()

    val orderedHomeItems = AppearanceSettingsManager(context).homeItemsItemsFlow.collectAsState(
        initial = listOf(
            HomeItem("recently_played", true),
            HomeItem("recently_added", true),
            HomeItem("most_played", true),
            HomeItem("random_songs", true)
        )
    ).value

    val titleMap = remember {
        mapOf(
            "recently_played" to R.string.recently_played,
            "recently_added" to R.string.recently_added,
            "most_played" to R.string.most_played,
            "random_songs" to R.string.random_songs
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .focusGroup()
        ) {
            val username = AppearanceSettingsManager(context)
                .usernameFlow.collectAsState("Username")

            Text(
                text = "${stringResource(R.string.welcome_text)}, ${username.value}!",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    navHostController.navigate(Screen.Setting.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }, modifier = Modifier
                    .padding(end = 12.dp)
                    .size(48.dp)
            ) {
                Icon(
                    ImageVector.vectorResource(R.drawable.rounded_settings_24),
                    contentDescription = "Settings",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (libraries.size > 1) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                libraries.forEach { (library, isSelected) ->
                    FilterChip(
                        onClick = {
                            NavidromeManager.currentServerId.value?.let { serverId ->
                                NavidromeManager.toggleServerLibraryEnabled(
                                    serverId, library.id, !isSelected
                                )
                            }
                        },
                        content = { Text(library.name) },
                        selected = isSelected,
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))


        val carouselState = rememberCarouselState()
        val coroutineScope = rememberCoroutineScope()

        Carousel(
            itemCount = shuffledAlbums.size,
            modifier = Modifier
                .height(320.dp)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(16.dp)),
            carouselState = carouselState,
            contentTransformEndToStart =
                fadeIn(tween(600)).togetherWith(fadeOut(tween(600))),
            contentTransformStartToEnd =
                fadeIn(tween(600)).togetherWith(fadeOut(tween(600)))
        ) { itemIndex ->
            val album = shuffledAlbums[itemIndex]
            CarouselItem(
                album = album,
                onPlay = {
                    coroutineScope.launch {
                        val mediaItems = viewModel.getAlbumSongs(
                            album.mediaMetadata.extras?.getString("navidromeID") ?: ""
                        )
                        if (mediaItems.size > 1)
                            SongHelper.play(
                                mediaItems = mediaItems.subList(1, mediaItems.size),
                                index = 0,
                                mediaController = mediaController
                            )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        orderedHomeItems.forEach { item ->
            if (!item.enabled) return@forEach

            val albums = when (item.key) {
                "recently_played" -> recentlyPlayedAlbums
                "recently_added" -> recentAlbums
                "most_played" -> mostPlayedAlbums
                "random_songs" -> shuffledAlbums
                else -> emptyList()
            }
            if (albums.isEmpty()) return@forEach

            TvHomeAlbumRow(
                title = stringResource(titleMap[item.key] ?: R.string.recently_played),
                albums = albums,
                navHostController = navHostController,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvHomeAlbumRow(
    title: String,
    albums: List<MediaItem>,
    navHostController: NavHostController,
) {
    Column(modifier = Modifier
        .padding(bottom = 24.dp)
        .focusGroup()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 12.dp, bottom = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(start = 12.dp, end = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.focusRestorer(),
        ) {
            items(albums) { album ->
                TvAlbumCard(
                    album = album,
                    modifier = Modifier.padding(end = 16.dp),
                    onClick = {
                        val albumEncoded = album.toAlbum()
                        val encodedImage = URLEncoder.encode(albumEncoded.coverArt, "UTF-8")
                        navHostController.navigate(Screen.AlbumDetails.route + "/${albumEncoded.navidromeID}/$encodedImage") {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CarouselItem(
    album: MediaItem,
    onPlay: () -> Unit
) {
    val title = album.mediaMetadata.title?.toString() ?: ""
    val artist = album.mediaMetadata.artist?.toString() ?: ""
    val genre = album.mediaMetadata.genre?.toString() ?: ""
    val duration = formatMilliseconds(
        album.mediaMetadata.durationMs?.div(1000)?.toInt() ?: 0
    )
    val subtitle = listOf(genre, artist, duration)
        .filter { it.isNotBlank() }
        .joinToString("  •  ")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(
                    album.mediaMetadata.artworkUri.toString()
                        .replace("&size=128", "&size=1024")
                )
                .diskCacheKey(album.mediaMetadata.extras?.getString("navidromeID"))
                .diskCachePolicy(CachePolicy.DISABLED)
                .crossfade(true)
                .build(),
            placeholder = painterResource(R.drawable.placeholder),
            fallback = painterResource(R.drawable.placeholder),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0.00f to Color.Black,
                            0.30f to Color.Black.copy(alpha = 0.55f),
                            0.55f to Color.Transparent,
                        ),
                        blendMode = BlendMode.DstOut
                    )
                }
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.60f)
                .background(
                    Brush.horizontalGradient(
                        0.00f to Color.Black.copy(alpha = 0.85f),
                        0.70f to Color.Black.copy(alpha = 0.50f),
                        1.00f to Color.Transparent
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.52f)
                .padding(start = 36.dp, bottom = 32.dp, end = 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Metadata pill: Genre • Artist • Duration
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
            }

            // Album title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(20.dp))

            // Buttons
            var playFocused by remember { mutableStateOf(false) }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPlay,
                    modifier = Modifier
                        .onFocusChanged { playFocused = it.isFocused }
                        .border(
                            width = 2.dp,
                            color = if (playFocused) MaterialTheme.colorScheme.primary
                            else Color.Transparent,
                            shape = RoundedCornerShape(50)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.Action_Play))
                }
            }
        }
    }
}