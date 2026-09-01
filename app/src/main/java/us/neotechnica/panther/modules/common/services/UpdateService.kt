//
//  UpdateService.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 01/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.modules.common.extensions.isForcedUpdateRequired
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.common.services.MetadataService
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.shared.models.SharedState
import us.neotechnica.panther.translator.Translator
import java.util.Date

/**
 * Prompts the user to install app updates.
 *
 * The service compares the running build against the hosted App
 * Store build number and presents either a dismissible update alert
 * or the blocking forced-update modal.
 */
object UpdateService {
    // MARK: - Types

    /** The kind of update to prompt for. */
    enum class UpdateType {
        /** An update the user must install to continue using the app. */
        FORCED,

        /** An update the user may install or postpone. */
        NORMAL,
    }

    // MARK: - Properties

    private val observationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var observationJob: Job? = null

    // MARK: - Computed Properties

    private val hasUpdatedSinceLastForced: Boolean
        get() {
            val buildNumberWhenLastForcedToUpdate =
                Persistent.int(PersistentStorageKey.buildNumberWhenLastForcedToUpdate) ?: return true
            if (buildNumberWhenLastForcedToUpdate != Build.buildNumber) {
                Persistent.setInt(PersistentStorageKey.buildNumberWhenLastForcedToUpdate, null)
                return true
            }

            return false
        }

    // MARK: - Observe Forced Update Changes

    fun startObservingForcedUpdateChanges() {
        observationJob?.cancel()
        observationJob =
            observationScope.launch {
                Logger.log("Started observing forced update changes.")
                try {
                    Networking.config.databaseDelegate
                        .observe<Map<String, Any>>(NetworkPath.shared.rawValue, prependingEnvironment = false)
                        .collect { dictionary ->
                            val appStoreBuildNumber =
                                (dictionary[MetadataService.MetadataServiceKey.APP_STORE_BUILD_NUMBER.rawValue] as? Number)
                                    ?.toInt() ?: return@collect
                            val shouldForceUpdate =
                                dictionary[MetadataService.MetadataServiceKey.SHOULD_FORCE_UPDATE.rawValue] as? Boolean
                                    ?: return@collect

                            if (appStoreBuildNumber > Build.buildNumber && shouldForceUpdate) {
                                triggerForcedUpdateModal()
                            }
                        }
                } catch (exception: Exception) {
                    Logger.log(exception)
                }
            }
    }

    // MARK: - Check for Updates

    /**
     * Checks for an available update and prompts the user to
     * install it if needed.
     */
    suspend fun promptToUpdateIfNeeded() {
        when (checkForUpdates() ?: return) {
            UpdateType.FORCED -> triggerForcedUpdateModal()
            UpdateType.NORMAL -> presentUpdateCTA()
        }
    }

    // MARK: - Increment Relaunch Count

    /**
     * Increments the persisted relaunch count if an update has been
     * postponed. Call once per launch.
     */
    fun incrementRelaunchCountIfNeeded() {
        if (Persistent.long(PersistentStorageKey.firstPostponedUpdate) == null) return
        Persistent.setInt(
            PersistentStorageKey.relaunchesSinceLastPostponedUpdate,
            (Persistent.int(PersistentStorageKey.relaunchesSinceLastPostponedUpdate) ?: 0) + 1,
        )
    }

    // MARK: - Auxiliary

    private suspend fun checkForUpdates(): UpdateType? {
        // Revalidate first so the update decision is never made
        // against a stale, persisted build number.
        MetadataService.resolveValues()

        val appStoreBuildNumber = MetadataService.appStoreBuildNumber ?: return null
        val isUpdateAvailable = appStoreBuildNumber > Build.buildNumber
        val shouldPrompt = (Persistent.int(PersistentStorageKey.relaunchesSinceLastPostponedUpdate) ?: 0) >= RELAUNCH_PROMPT_THRESHOLD

        if (MetadataService.shouldForceUpdate) return if (isUpdateAvailable) UpdateType.FORCED else null
        if (!hasUpdatedSinceLastForced) return if (isUpdateAvailable) UpdateType.FORCED else null

        val firstPostponedUpdate =
            Persistent.long(PersistentStorageKey.firstPostponedUpdate)
                ?: return if (isUpdateAvailable) UpdateType.NORMAL else null

        val daysPassed = (Date().time - firstPostponedUpdate) / MILLIS_PER_DAY
        if (daysPassed < 0) {
            Persistent.setLong(PersistentStorageKey.firstPostponedUpdate, null)
            Persistent.setInt(PersistentStorageKey.relaunchesSinceLastPostponedUpdate, 0)
            Persistent.setInt(PersistentStorageKey.buildNumberWhenLastForcedToUpdate, null)
        }

        if (daysPassed < FORCE_UPDATE_POSTPONE_DAYS) {
            return if (isUpdateAvailable && shouldPrompt) UpdateType.NORMAL else null
        }

        return if (isUpdateAvailable) UpdateType.FORCED else null
    }

    private suspend fun presentUpdateCTA() {
        var appShareLink = MetadataService.appShareLink
        if (appShareLink == null) {
            MetadataService.resolveValues()
            appShareLink = MetadataService.appShareLink ?: return
        }

        val installURL = appShareLink
        Alert(
            title = "Update Available",
            message = "A new version of ${Build.finalName} is available. Would you like to update now?",
            actions =
                listOf(
                    Action("Update", style = ActionStyle.PREFERRED) {
                        openInstallURL(installURL)
                        Persistent.setLong(PersistentStorageKey.firstPostponedUpdate, null)
                        Persistent.setInt(PersistentStorageKey.relaunchesSinceLastPostponedUpdate, 0)
                    },
                    Action("Cancel", style = ActionStyle.CANCEL) {
                        if (Persistent.long(PersistentStorageKey.firstPostponedUpdate) == null) {
                            Persistent.setLong(PersistentStorageKey.firstPostponedUpdate, Date().time)
                        }
                        Persistent.setInt(PersistentStorageKey.relaunchesSinceLastPostponedUpdate, 0)
                    },
                ),
        ).present()
    }

    private fun triggerForcedUpdateModal() {
        Persistent.setLong(PersistentStorageKey.firstPostponedUpdate, null)
        Persistent.setInt(PersistentStorageKey.relaunchesSinceLastPostponedUpdate, 0)
        Persistent.setInt(PersistentStorageKey.buildNumberWhenLastForcedToUpdate, Build.buildNumber)
        SharedState { it.isForcedUpdateRequired }.wrappedValue = true
    }

    private fun openInstallURL(url: String) {
        val activity = Translator.config.currentActivityProvider?.invoke() ?: return
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

private const val FORCE_UPDATE_POSTPONE_DAYS = 10L
private const val MILLIS_PER_DAY = 86_400_000L
private const val RELAUNCH_PROMPT_THRESHOLD = 3
