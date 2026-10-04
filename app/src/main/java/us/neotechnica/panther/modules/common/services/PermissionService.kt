//
//  PermissionService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.translator.Translator
import kotlin.coroutines.resume
import android.os.Build as SystemBuild

/**
 * Checks, requests, and prompts for system permissions.
 *
 * The recording and transcription permissions are cut until audio
 * messages arrive, so only contacts and
 * notifications are modeled.
 */
object PermissionService {
    // MARK: - Types

    /** The authorization state of a system permission. */
    enum class PermissionStatus {
        /** The permission was denied or is restricted. */
        DENIED,

        /** The permission was granted. */
        GRANTED,

        /** The permission has not yet been determined. */
        UNKNOWN,
    }

    /** A system permission used by the app. */
    enum class PermissionType {
        /** Access to the user's contact list. */
        CONTACTS,

        /** Permission to display notifications, badges, and sounds. */
        NOTIFICATIONS,
    }

    // MARK: - Computed Properties

    /** The current status of the contacts permission. */
    val contactPermissionStatus: PermissionStatus
        get() = getPermissionStatus(Manifest.permission.READ_CONTACTS)

    /** The current status of the notifications permission. */
    val notificationPermissionStatus: PermissionStatus
        get() =
            if (SystemBuild.VERSION.SDK_INT >= SystemBuild.VERSION_CODES.TIRAMISU) {
                getPermissionStatus(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                val activity = currentActivity() ?: return PermissionStatus.UNKNOWN
                if (NotificationManagerCompat.from(activity).areNotificationsEnabled()) {
                    PermissionStatus.GRANTED
                } else {
                    PermissionStatus.DENIED
                }
            }

    // MARK: - Permissions Requesting

    /**
     * Requests the given permission from the user through the current
     * activity's result registry.
     *
     * @param type The permission to request.
     * @return The resulting permission status.
     * @throws Exception if no activity is foregrounded to host the request.
     */
    suspend fun requestPermission(type: PermissionType): PermissionStatus =
        when (type) {
            PermissionType.CONTACTS -> requestSystemPermission(Manifest.permission.READ_CONTACTS)
            PermissionType.NOTIFICATIONS ->
                if (SystemBuild.VERSION.SDK_INT >= SystemBuild.VERSION_CODES.TIRAMISU) {
                    requestSystemPermission(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // Pre-Tiramisu there is no notification runtime permission; report the current state.
                    notificationPermissionStatus
                }
        }

    private suspend fun requestSystemPermission(permission: String): PermissionStatus {
        val activity =
            currentActivity() as? ComponentActivity
                ?: throw Exception("No activity to request a permission from.", metadata = ExceptionMetadata(this))

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                var launcher: ActivityResultLauncher<String>? = null
                launcher =
                    activity.activityResultRegistry.register(
                        "${REGISTRY_KEY_PREFIX}_${System.nanoTime()}",
                        ActivityResultContracts.RequestPermission(),
                    ) { isGranted ->
                        launcher?.unregister()
                        continuation.resume(if (isGranted) PermissionStatus.GRANTED else PermissionStatus.DENIED)
                    }
                continuation.invokeOnCancellation { launcher?.unregister() }
                launcher.launch(permission)
            }
        }
    }

    // MARK: - Call to Action Methods

    /**
     * Presents an alert prompting the user to grant the given permission
     * in Settings.
     *
     * @param type The permission the alert describes.
     * @return `true` if the user canceled the alert; otherwise, `false`
     *   if they chose to open Settings.
     */
    suspend fun presentCTA(type: PermissionType): Boolean =
        when (type) {
            PermissionType.CONTACTS -> presentContactCTA()
            PermissionType.NOTIFICATIONS -> presentNotificationCTA()
        }

    private suspend fun presentContactCTA(): Boolean =
        presentCTA(
            "⌘${Build.finalName}⌘ has not been granted permission to access your contact list." +
                "\n\nYou can change this in Settings.",
        )

    private suspend fun presentNotificationCTA(): Boolean =
        presentCTA(
            "⌘${Build.finalName}⌘ has not been granted permission to send and receive notifications." +
                "\n\nYou can change this in Settings.",
        )

    private suspend fun presentCTA(message: String): Boolean {
        val cancelled = LockIsolated(true)
        val activity = currentActivity()
        val resolvedSettingsString = LocalizedStringKey.Settings.localized().replace("…", "...")

        val actions = mutableListOf(Action(LocalizedStringKey.Cancel.localized(), style = ActionStyle.CANCEL) {})
        if (activity != null) {
            actions.add(
                Action(resolvedSettingsString) {
                    cancelled.wrappedValue = false
                    activity.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", activity.packageName, null),
                        ),
                    )
                },
            )
        }

        Alert(message = message, actions = actions).present(translating = listOf(Alert.TranslationOptionKey.Message))
        return cancelled.wrappedValue
    }

    // MARK: - Auxiliary

    private fun getPermissionStatus(permission: String): PermissionStatus {
        val activity = currentActivity() ?: return PermissionStatus.UNKNOWN
        if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
            return PermissionStatus.GRANTED
        }
        // Android cannot distinguish "never requested" from "permanently denied" directly; the
        // rationale flag is the closest heuristic (true only after a denial the user can revisit).
        return if (activity.shouldShowRequestPermissionRationale(permission)) {
            PermissionStatus.DENIED
        } else {
            PermissionStatus.UNKNOWN
        }
    }

    private fun currentActivity() = Translator.config.currentActivityProvider?.invoke()

    // MARK: - Companion

    private const val REGISTRY_KEY_PREFIX = "PermissionService"
}
