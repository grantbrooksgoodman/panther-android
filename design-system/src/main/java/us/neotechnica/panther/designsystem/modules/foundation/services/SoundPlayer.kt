//
//  SoundPlayer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.services

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated

/**
 * Plays short interface sounds from raw resources.
 *
 * [initialize] must be called once with the application context
 * before any playback. Sounds load lazily: the first [play] of a
 * resource loads it and plays it when loading completes, and later
 * plays start immediately.
 */
object SoundPlayer {
    // MARK: - Properties

    private val loadedSoundIDs = LockIsolated(mapOf<Int, Int>())

    private val soundPool: SoundPool by lazy {
        SoundPool
            .Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            ).build()
            .also { pool ->
                pool.setOnLoadCompleteListener { completedPool, soundID, status ->
                    if (status == 0) completedPool.play(soundID, VOLUME, VOLUME, 1, 0, PLAYBACK_RATE)
                }
            }
    }

    @Volatile
    private var appContext: Context? = null

    // MARK: - Initialization

    /** Prepares the player with the application context. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    // MARK: - Methods

    /**
     * Plays the sound stored under the given raw resource.
     *
     * @param resourceID The raw resource identifier of the sound.
     */
    fun play(resourceID: Int) {
        val context = appContext ?: return
        val loadedSoundID = loadedSoundIDs.wrappedValue[resourceID]
        if (loadedSoundID != null) {
            soundPool.play(loadedSoundID, VOLUME, VOLUME, 1, 0, PLAYBACK_RATE)
            return
        }

        // The load-complete listener plays the sound once it loads.
        val soundID = soundPool.load(context, resourceID, 1)
        loadedSoundIDs.withValue { it.value = it.value + (resourceID to soundID) }
    }

    // MARK: - Companion

    private const val MAX_STREAMS = 2
    private const val PLAYBACK_RATE = 1f
    private const val VOLUME = 1f
}
