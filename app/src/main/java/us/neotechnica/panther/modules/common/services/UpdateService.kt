//
//  UpdateService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
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
import us.neotechnica.panther.bundle.buildNumberWhenLastForcedToUpdate
import us.neotechnica.panther.bundle.firstPostponedUpdate
import us.neotechnica.panther.bundle.relaunchesSinceLastPostponedUpdate
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.modules.common.extensions.MetadataServiceStorageKey
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.shared.models.SharedState
import us.neotechnica.panther.subsystem.modules.shared.models.isForcedUpdateRequired
import us.neotechnica.panther.translator.Translator
import java.util.Calendar
import java.util.Date

/**
 * Prompts the user to install app updates.
 *
 * The service compares the running build against the hosted Play
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

    /**
     * The URL the forced-update modal's install button opens, or
     * `null` if it has not been resolved.
     */
    val installButtonRedirectURL: String?
        get() = MetadataService.playStoreShareLink

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
                            val playStoreBuildNumber =
                                (dictionary[MetadataServiceStorageKey.PLAY_STORE_BUILD_NUMBER.rawValue] as? Number)
                                    ?.toInt() ?: return@collect
                            val shouldForceUpdateAndroid =
                                dictionary[MetadataServiceStorageKey.SHOULD_FORCE_UPDATE_ANDROID.rawValue] as? Boolean
                                    ?: return@collect

                            if (playStoreBuildNumber > Build.buildNumber && shouldForceUpdateAndroid) {
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
     * Checks for an available update and prompts the user to install
     * it if needed.
     *
     * An update is available when the hosted Play Store build number
     * exceeds the running build's. Updates are forced when the hosted
     * force-update flag is set, when the app has not been updated
     * since a previous forced update, or when a postponed update is
     * at least 10 days old; a forced update presents the blocking
     * forced-update modal. Otherwise, an alert offers to open Google
     * Play, and canceling postpones the update – once postponed, the
     * alert reappears only after at least three subsequent relaunches.
     *
     * @throws Exception If resolving hosted metadata fails.
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

        val playStoreBuildNumber = MetadataService.playStoreBuildNumber ?: return null
        val overrideForceUpdate = MetadataService.shouldForceUpdateAndroid ?: return null

        val isUpdateAvailable = playStoreBuildNumber > Build.buildNumber
        val shouldPrompt =
            (Persistent.int(PersistentStorageKey.relaunchesSinceLastPostponedUpdate) ?: 0) >= RELAUNCH_PROMPT_THRESHOLD

        if (overrideForceUpdate) return if (isUpdateAvailable) UpdateType.FORCED else null
        if (!hasUpdatedSinceLastForced) return if (isUpdateAvailable) UpdateType.FORCED else null

        val firstPostponedUpdate =
            Persistent.long(PersistentStorageKey.firstPostponedUpdate)
                ?: return if (isUpdateAvailable) UpdateType.NORMAL else null

        val daysPassed = (startOfDay(Date()) - startOfDay(Date(firstPostponedUpdate))) / MILLIS_PER_DAY
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
        val installURL =
            installButtonRedirectURL ?: run {
                MetadataService.resolveValues()
                installButtonRedirectURL ?: return
            }

        val updateAction =
            Action("Update", style = ActionStyle.PREFERRED) {
                openInstallURL(installURL)
                Persistent.setLong(PersistentStorageKey.firstPostponedUpdate, null)
                Persistent.setInt(PersistentStorageKey.relaunchesSinceLastPostponedUpdate, 0)
            }

        Alert(
            title = "Update Available",
            message = "A new version of ⌘${Build.finalName}⌘ is available on ⌘Google Play⌘. Would you like to update now?",
            actions =
                listOf(
                    updateAction,
                    Action(LocalizedStringKey.Cancel.localized(), style = ActionStyle.CANCEL) {
                        if (Persistent.long(PersistentStorageKey.firstPostponedUpdate) == null) {
                            Persistent.setLong(PersistentStorageKey.firstPostponedUpdate, Date().time)
                        }
                        Persistent.setInt(PersistentStorageKey.relaunchesSinceLastPostponedUpdate, 0)
                    },
                ),
        ).present(
            translating =
                listOf(
                    Alert.TranslationOptionKey.Actions(listOf(updateAction)),
                    Alert.TranslationOptionKey.Message,
                    Alert.TranslationOptionKey.Title,
                ),
        )
    }

    private fun openInstallURL(url: String) {
        val activity = Translator.config.currentActivityProvider?.invoke() ?: return
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    // The start of the calendar day for the given date, so the
    // postponement interval is counted in whole calendar days.
    private fun startOfDay(date: Date): Long {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun triggerForcedUpdateModal() {
        Persistent.setLong(PersistentStorageKey.firstPostponedUpdate, null)
        Persistent.setInt(PersistentStorageKey.relaunchesSinceLastPostponedUpdate, 0)
        Persistent.setInt(PersistentStorageKey.buildNumberWhenLastForcedToUpdate, Build.buildNumber)
        SharedState { it.isForcedUpdateRequired }.wrappedValue = true
    }
}

private const val FORCE_UPDATE_POSTPONE_DAYS = 10L
private const val MILLIS_PER_DAY = 86_400_000L
private const val RELAUNCH_PROMPT_THRESHOLD = 3
