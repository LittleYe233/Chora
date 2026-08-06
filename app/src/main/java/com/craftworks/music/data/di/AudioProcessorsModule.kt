package com.craftworks.music.data.di

import com.craftworks.music.managers.audio.EqualizerAudioProcessor
import com.craftworks.music.managers.audio.PreampAudioProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing the audio processor singletons used by the playback chain.
 *
 * Both [PreampAudioProcessor] and [EqualizerAudioProcessor] are scoped to the
 * application so a single instance is shared between the player service and the
 * settings UI, and so they outlive any ExoPlayer recreation. The processors are
 * configured by the player (which also defines their order in the audio chain,
 * preamp before equalizer) and driven in real time by the settings UI without
 * recreating the player.
 */
@Module
@InstallIn(SingletonComponent::class)
object AudioProcessorsModule {

    @Singleton
    @Provides
    fun providePreampAudioProcessor(): PreampAudioProcessor {
        return PreampAudioProcessor()
    }

    @Singleton
    @Provides
    fun provideEqualizerAudioProcessor(): EqualizerAudioProcessor {
        return EqualizerAudioProcessor()
    }
}
