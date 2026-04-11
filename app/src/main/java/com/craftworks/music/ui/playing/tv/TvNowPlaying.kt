@file:OptIn(UnstableApi::class)

package com.craftworks.music.ui.playing.tv

import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.StandardCardContainer
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.data.repository.LyricsState
import com.craftworks.music.player.ChoraMediaLibraryService
import com.craftworks.music.ui.playing.LyricsView
import com.craftworks.music.ui.playing.dpToPx
import com.craftworks.music.ui.screens.tv.requestFocusOnFirstGainingVisibility
import com.gigamole.composefadingedges.marqueeHorizontalFadingEdges
import kotlinx.coroutines.delay

@Preview(device = "id:tv_1080p", showBackground = true, showSystemUi = true)
@Composable
fun TvNowPlaying(
    mediaController: MediaController? = null,
    iconColor: Color = Color.Black,
    metadata: MediaMetadata? = null
){
    var controlsVisible by remember { mutableStateOf(false) }
    val lyrics by LyricsState.lyrics.collectAsStateWithLifecycle()

    // Auto-hide after 5 seconds of visibility
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(5000)
            controlsVisible = false
        }
    }

    val iconTextColor by animateColorAsState(
        targetValue = iconColor,
        animationSpec = tween(1000, 0, FastOutSlowInEasing),
        label = "Animated text color"
    )

    val bottomPadding by animateIntAsState(
        targetValue = if (controlsVisible) dpToPx(64) else 0,
        animationSpec = tween(1000, 0, FastOutSlowInEasing),
        label = "Move content up with controls visible"
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 48.dp, vertical = 24.dp)
            .focusable(true)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && !controlsVisible) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            controlsVisible = true
                            mediaController?.pause()
                        }

                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            controlsVisible = true
                        }

                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            mediaController?.seekBack()
                        }

                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            mediaController?.seekForward()
                        }
                    }
                    return@onKeyEvent true
                }
                false
            },
        contentAlignment = Alignment.Center
    ) {
        Row (
            Modifier.offset { IntOffset(0, -bottomPadding) }
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StandardCardContainer(
                    imageCard = {
                        Box (
                            Modifier
                                .height(320.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
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
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .shadow(4.dp, RoundedCornerShape(12.dp), clip = true)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            )
                        }
                    },
                    title = {
                        Text(
                            text = metadata?.title.toString(),
                            color = iconTextColor,
                            maxLines = 1,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .marqueeHorizontalFadingEdges(marqueeProvider = { Modifier.basicMarquee() })
                        )
                    },
                    subtitle = {
                        Text(
                            text = metadata?.albumTitle.toString()  + if (metadata?.recordingYear != 0) " • " + metadata?.recordingYear else "",
                            color = iconTextColor,
                            maxLines = 1,
                            modifier = Modifier
                                .marqueeHorizontalFadingEdges(marqueeProvider = { Modifier.basicMarquee() })
                        )
                    },
                    description = {
                        Text(
                            text = metadata?.artist.toString(),
                            color = iconTextColor,
                            maxLines = 1,
                            modifier = Modifier
                                .marqueeHorizontalFadingEdges(marqueeProvider = { Modifier.basicMarquee() })
                        )
                    }
                )
            }

            AnimatedVisibility(
                visible = metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION && lyrics.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Box(Modifier
                    .fillMaxHeight()
                    .focusable(false)
                    .focusProperties {
                        canFocus = false
                    }
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

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(
                animationSpec = tween(durationMillis = 400)
            ) + slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + scaleIn(
                initialScale = 0.9f,
                transformOrigin = TransformOrigin(0.5f, 1f),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 300)
            ) + slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + scaleOut(
                targetScale = 0.8f,
                transformOrigin = TransformOrigin(0.5f, 1f)
            ),
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .align(Alignment.BottomCenter)
                .focusGroup()
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusGroup(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChoraMediaLibraryService.getInstance()?.player?.let {
                        ShuffleButton(
                            it,
                            Modifier
                                .size(IconButtonDefaults.SmallButtonSize)
                                .focusProperties {
                                    up = FocusRequester.Cancel
                                }
                        )

                        PreviousSongButton(
                            it,
                            Modifier
                                .size(IconButtonDefaults.MediumButtonSize)
                                .focusProperties {
                                    up = FocusRequester.Cancel
                                }
                        )

                        PlayPauseButton(
                            it,
                            Modifier
                                .size(IconButtonDefaults.LargeButtonSize)
                                .requestFocusOnFirstGainingVisibility()
                                .focusProperties {
                                    up = FocusRequester.Cancel
                                }
                        )

                        NextSongButton(
                            it,
                            Modifier
                                .size(IconButtonDefaults.MediumButtonSize)
                                .focusProperties {
                                    up = FocusRequester.Cancel
                                }
                        )

                        RepeatButton(
                            it,
                            Modifier
                                .size(IconButtonDefaults.SmallButtonSize)
                                .focusProperties {
                                    up = FocusRequester.Cancel
                                }
                        )
                    }
                }

                if (metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                    PlaybackProgressSlider(iconTextColor, mediaController)
            }
        }
    }
}