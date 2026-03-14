@file:OptIn(UnstableApi::class)

package com.craftworks.music.ui.screens.tv

import androidx.annotation.OptIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.data.repository.LyricsState
import com.craftworks.music.player.ChoraMediaLibraryService
import com.craftworks.music.ui.playing.LyricsView
import com.gigamole.composefadingedges.marqueeHorizontalFadingEdges

@Preview(device = "id:tv_1080p", showBackground = true, showSystemUi = true)
@Composable
fun TvNowPlaying(
    mediaController: MediaController? = null,
    iconColor: Color = Color.Black,
    metadata: MediaMetadata? = null
){
    val focusRequester = remember { FocusRequester() }

    // use dark or light colors for icons and text based on the album art luminance.
    val iconTextColor by animateColorAsState(
        targetValue = iconColor,
        animationSpec = tween(1000, 0, FastOutSlowInEasing),
        label = "Animated text color"
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Row {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                //.widthIn(max = 640.dp)
                .padding(horizontal = 48.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            /* Image + Song Title + Artist */
            Row(
                modifier = Modifier.wrapContentHeight(),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(metadata?.artworkUri.toString().replace("size=128", "size=500"))
                        .diskCachePolicy(CachePolicy.DISABLED)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album Cover Art",
                    placeholder = painterResource(R.drawable.placeholder),
                    fallback = painterResource(R.drawable.placeholder),
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .height(256.dp)
                        .aspectRatio(1f)
                        .shadow(4.dp, RoundedCornerShape(12.dp), clip = true)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )

                Spacer(Modifier.width(24.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = metadata?.title.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = iconTextColor,
                        maxLines = 1, overflow = TextOverflow.Visible,
                        softWrap = false,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .wrapContentWidth(
                                align = Alignment.Start,
                                unbounded = true
                            )
                            //.fillMaxWidth()
                            //.marqueeHorizontalFadingEdges(marqueeProvider = { Modifier.basicMarquee() })
                    )

                    Text(
                        text = metadata?.albumTitle.toString()  + if (metadata?.recordingYear != 0) " • " + metadata?.recordingYear else "",
                        style = MaterialTheme.typography.titleLarge,
                        color = iconTextColor,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .wrapContentWidth(
                                align = Alignment.Start,
                                unbounded = true
                            )
                    )

                    Text(
                        text = metadata?.artist.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = iconTextColor,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .wrapContentWidth(
                                align = Alignment.Start,
                                unbounded = true
                            )
                            .marqueeHorizontalFadingEdges(marqueeProvider = { Modifier.basicMarquee() })
                    )
                }
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .focusGroup(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChoraMediaLibraryService.getInstance()?.player?.let {
                    ShuffleButton(
                        it,
                        Modifier.size(IconButtonDefaults.SmallButtonSize)
                    )

                    PreviousSongButton(
                        it,
                        Modifier.size(IconButtonDefaults.MediumButtonSize)
                    )

                    PlayPauseButton(
                        it,
                        Modifier.size(IconButtonDefaults.LargeButtonSize).focusRequester(focusRequester)
                    )

                    NextSongButton(
                        it,
                        Modifier.size(IconButtonDefaults.MediumButtonSize)
                    )

                    RepeatButton(
                        it,
                        Modifier.size(IconButtonDefaults.SmallButtonSize)
                    )
                }
            }

            if (metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                PlaybackProgressSlider(iconTextColor, mediaController)
        }

        val lyrics by LyricsState.lyrics.collectAsStateWithLifecycle()

        var topPadding = 0.dp
        if ((metadata?.title?.length ?: 0) > 25)
            topPadding = 32.dp
        if (((metadata?.albumTitle?.length ?: 0) + 4) > 20)
            topPadding = 64.dp
        if ((metadata?.artist?.length ?: 0) > 40)
            topPadding = 96.dp

        if (metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION
            && lyrics.isNotEmpty()
        ) {
            Box(Modifier.weight(0.75f)
                .fillMaxHeight()
                .padding(top = topPadding)
                .focusGroup()
                .focusRestorer()
            ){
                LyricsView(
                    iconTextColor,
                    true,
                    mediaController,
                    PaddingValues(24.dp)
                )
            }
        }
    }
}