//
//  AnalyticsService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.services

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkEnvironment
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.extensions.currentUserID
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.Milestone
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.Build as AndroidBuild

/**
 * Reports usage events to the analytics backend.
 *
 * Events are reported with a standard set of metadata describing
 * the build, device, language, and current user.
 */
object AnalyticsService {
    // MARK: - Types

    /** A usage event that can be reported to the analytics backend. */
    enum class AnalyticsEvent(
        val eventName: String,
    ) {
        ACCESS_CHAT("access_chat"),
        ACCESS_NEW_CHAT_PAGE("access_new_chat_page"),

        CLEAR_CACHES("clear_caches"),
        CLOSE_APP("close_app"),
        CREATE_NEW_CONVERSATION("create_new_conversation"),

        DELETE_ACCOUNT("delete_account"),
        DELETE_CONVERSATION("delete_conversation"),
        DISMISS_NEW_CHAT_PAGE("dismiss_new_chat_page"),

        INVITE("invite"),

        LOG_IN("log_in"),
        LOG_OUT("log_out"),

        OPEN_APP("open_app"),

        SEND_AUDIO_MESSAGE("send_audio_message"),
        SEND_MEDIA_MESSAGE("send_media_message"),
        SEND_TEXT_MESSAGE("send_text_message"),
        SIGN_UP("sign_up"),

        TERMINATE_APP("terminate_app"),
        TOUCH_UI_ELEMENT("touch_ui_element"),

        VIEW_ALTERNATE("view_alternate"),
    }

    // MARK: - Properties

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // MARK: - Computed Properties

    /**
     * A Boolean value that indicates whether analytics data
     * collection is enabled.
     *
     * Data collection is enabled on general-release builds in the
     * production environment.
     */
    val shouldEnableDataCollection: Boolean
        get() =
            Networking.config.environment == NetworkEnvironment.PRODUCTION &&
                Build.milestone == Milestone.GENERAL_RELEASE

    // MARK: - Log Event

    /**
     * Reports the given event to the analytics backend.
     *
     * The event is reported asynchronously with the standard
     * metadata set; this method returns immediately. Parameter
     * values longer than 40 characters are truncated. If
     * [shouldEnableDataCollection] is `false`, the event is
     * discarded.
     *
     * @param event The event to report.
     * @param additionalUserInfo Additional parameters to attach to
     *   the event, overriding standard metadata values with matching
     *   keys.
     */
    fun logEvent(
        event: AnalyticsEvent,
        additionalUserInfo: Map<String, String>? = null,
    ) {
        scope.launch {
            if (!shouldEnableDataCollection) return@launch

            val parameters = userInfo().toMutableMap()
            additionalUserInfo?.let { parameters.putAll(it) }

            for ((key, value) in parameters) {
                if (value.length > MAX_PARAMETER_LENGTH) {
                    parameters[key] = value.take(MAX_PARAMETER_LENGTH)
                }
            }

            Logger.log("Logging analytics event \"${event.eventName}\".", domain = LoggerDomain.analytics)

            val bundle = Bundle()
            parameters.forEach { (key, value) -> bundle.putString(key, value) }
            FirebaseAnalytics.getInstance(Networking.requireContext()).logEvent(event.eventName, bundle)
        }
    }

    // MARK: - Auxiliary

    private fun userInfo(): Map<String, String> {
        val parameters =
            mutableMapOf(
                "build_sku" to Build.buildSKU,
                "bundle_revision" to "${Build.bundleRevision} (${Build.revisionBuildNumber})",
                "bundle_version" to "${Build.bundleVersion} (${Build.buildNumber}${Build.milestone.shortString})",
                "connection_status" to if (ConnectionStatusService.isOnline) "online" else "offline",
                "device_model" to "${AndroidBuild.MODEL} (${AndroidBuild.DEVICE.lowercase()})",
                "language_code" to RuntimeStorage.languageCode,
                "os_version" to AndroidBuild.VERSION.RELEASE.lowercase(),
                "timestamp" to SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(Date()),
            )

        User.currentUserID?.let { parameters["current_user_id"] = it }

        return parameters
    }
}

private const val MAX_PARAMETER_LENGTH = 40
private const val TIMESTAMP_FORMAT = "H:mm:ss.SSSS"
