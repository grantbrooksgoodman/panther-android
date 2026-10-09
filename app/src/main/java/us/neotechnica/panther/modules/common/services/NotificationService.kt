//
//  NotificationService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import us.neotechnica.panther.bundle.users
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.sanitized
import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.modules.common.extensions.notRegisteredForPushNotifications
import us.neotechnica.panther.modules.common.extensions.stalePushToken
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.networking.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.message.models.HostedContentType
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.ClientSession
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.entity.extensions.calculateBadgeNumber
import us.neotechnica.panther.modules.session.entity.extensions.currentUserID
import us.neotechnica.panther.networking.modules.common.dependencies.networking
import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.networking.modules.common.extensions.digits
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.common.models.NetworkServices
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.foundation.extensions.compiledException
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHashOf
import us.neotechnica.panther.subsystem.modules.foundation.models.AppException
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

/**
 * Sends push notifications and responds to notifications received
 * while the app is in the foreground.
 *
 * Notifications are delivered directly through the push
 * notification backend, localized to each recipient's language.
 */
// The counterpart service carries `// swiftlint:disable file_length type_body_length`.
@Suppress("LargeClass")
object NotificationService {
    // MARK: - Dependencies

    private val clientSession: ClientSession by Dependency { it.clientSession }
    private val networking: NetworkServices by Dependency { it.networking }
    private val services: CommonServices by Dependency { it.commonServices }

    // MARK: - Set Badge Number

    /**
     * Sets the app's badge number.
     *
     * Negative values are clamped to zero. The launcher badge is
     * derived from posted notifications, so this method updates only
     * the current user's hosted badge number.
     *
     * @param badgeNumber The badge number to set.
     * @param updateHostedValue A Boolean value that indicates
     *   whether the current user's hosted badge number should also be
     *   updated. The default is `true`.
     *
     * @throws Exception if updating the hosted value fails.
     */
    suspend fun setBadgeNumber(
        badgeNumber: Int,
        updateHostedValue: Boolean = true,
    ) {
        val currentUser = clientSession.entity.user.currentUser
        if (!updateHostedValue || currentUser == null) return
        updateHostedBadgeNumber(
            max(0, badgeNumber),
            user = currentUser,
        )
    }

    // MARK: - Notify Users of Message

    /**
     * Sends a push notification about the given message – or a
     * reaction to it – to each of the given users.
     *
     * Notification content is localized to each recipient's language,
     * and each recipient's hosted badge number is incremented
     * atomically. Recipients who have not registered for push
     * notifications are skipped; stale push tokens are erased.
     *
     * @param users The users to notify.
     * @param ofReaction The reaction the notification describes; pass
     *   `null` to describe the message itself. The default is `null`.
     * @param message The message the notification describes.
     * @param conversationIDKey The identifier key of the conversation
     *   containing the message.
     *
     * @throws Exception if the current user has not been set, or if
     *   delivery fails.
     */
    suspend fun notify(
        users: List<User>,
        ofReaction: Reaction? = null,
        message: Message,
        conversationIDKey: String,
    ) {
        val currentUser =
            clientSession.entity.user.currentUser
                ?: throw Exception("Current user has not been set.", metadata = ExceptionMetadata(this))

        val currentUserFormattedPhoneNumberString = currentUser.phoneNumber.formattedString()
        if (ofReaction == null) {
            return users.forEachConcurrently { user ->
                try {
                    notify(
                        user,
                        title = currentUserFormattedPhoneNumberString,
                        body = notificationBody(message, user),
                        conversationIDKey = conversationIDKey,
                        reactionMessageID = null,
                    )
                } catch (exception: Exception) {
                    if (!exception.isEqual(AppException.notRegisteredForPushNotifications)) throw exception
                }
            }
        }

        users.forEachConcurrently { user ->
            val reactedString = LocalizedStringKey.Reacted.localized(languageCode = user.languageCode)
            val reactionSuffix = "$reactedString ${ofReaction.style.emojiValue}"

            var body = notificationBody(message, user)
            if (body != null && message.contentType == HostedContentType.Text) {
                body = "“$body”"
            }

            try {
                notify(
                    user,
                    title = "$currentUserFormattedPhoneNumberString $reactionSuffix",
                    body = body,
                    conversationIDKey = conversationIDKey,
                    reactionMessageID = message.id,
                    reactionSuffix = reactionSuffix,
                )
            } catch (exception: Exception) {
                if (!exception.isEqual(AppException.notRegisteredForPushNotifications)) throw exception
            }
        }
    }

    // MARK: - Auxiliary

    private suspend fun generateAccessToken(): String =
        withContext(Dispatchers.IO) {
            val connection =
                (URL(ACCESS_TOKEN_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                }

            try {
                val responseCode = connection.responseCode
                val stream = if (responseCode.isSuccessfulHTTPStatus) connection.inputStream else connection.errorStream
                val responseBody = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
                if (!responseCode.isSuccessfulHTTPStatus) {
                    throw Exception(
                        "Failed to decode URL response or status did not indicate success.",
                        userInfo =
                            mapOf(
                                "ResponseBody" to responseBody,
                                "URLResponseCode" to responseCode,
                            ),
                        metadata = ExceptionMetadata(this@NotificationService),
                    )
                }

                responseBody
            } catch (exception: Exception) {
                throw exception
            } catch (throwable: Throwable) {
                throw Exception.from(throwable, ExceptionMetadata(this@NotificationService))
            } finally {
                connection.disconnect()
            }
        }

    internal fun notificationBody(
        message: Message,
        user: User,
    ): String? =
        when (val contentType = message.contentType) {
            is HostedContentType.Audio -> "🔊 ${LocalizedStringKey.AudioMessage.localized(languageCode = user.languageCode)}"

            is HostedContentType.Media ->
                when {
                    contentType.fileExtension.isDocument ->
                        "📄 ${LocalizedStringKey.Document.localized(languageCode = user.languageCode)}"

                    contentType.fileExtension.isImage ->
                        "🏞️ ${LocalizedStringKey.Image.localized(languageCode = user.languageCode)}"

                    contentType.fileExtension.isVideo ->
                        "🎥 ${LocalizedStringKey.Video.localized(languageCode = user.languageCode)}"

                    else ->
                        "📎 ${LocalizedStringKey.Attachment.localized(languageCode = user.languageCode)}"
                }

            HostedContentType.Text ->
                message.translations?.let { translations ->
                    (
                        translations.firstOrNull { it.languagePair.to == user.languageCode }?.output
                            ?: translations.firstOrNull { it.languagePair.from == user.languageCode }?.input?.value
                    )?.sanitized
                }
        }

    @Suppress("LongParameterList")
    private suspend fun notify(
        user: User,
        title: String,
        body: String?,
        conversationIDKey: String,
        reactionMessageID: String?,
        reactionSuffix: String? = null,
    ) {
        val isReaction = reactionMessageID != null
        val userInfo = mapOf("UserID" to user.id)

        val currentUser =
            clientSession.entity.user.currentUser
                ?: throw Exception("Current user has not been set.", metadata = ExceptionMetadata(this))
                    .appending(userInfo)

        // Atomic badge increment via transaction to avoid
        // read-modify-write races when multiple senders
        // notify the same recipient concurrently.
        val badgePath =
            listOf(
                NetworkPath.users.rawValue,
                user.id,
                BADGE_NUMBER_KEY,
            ).joinToString("/")

        val newBadgeNumber =
            (
                networking.database.runTransaction(badgePath) { currentValue ->
                    max(0, ((currentValue as? Number)?.toInt() ?: 0) + 1)
                } as? Number
            )?.toInt() ?: 0

        val pushTokens =
            user.pushTokens
                ?: throw Exception(
                    "The specified user has not registered for push notifications.",
                    metadata = ExceptionMetadata(this),
                ).appending(userInfo)

        val userNumberHash = encodedHashOf(listOf(currentUser.phoneNumber.nationalNumberString.digits))
        val exceptions = mutableListOf<Exception>()
        for (pushToken in pushTokens) {
            runCatchingException {
                sendNotification(
                    title = title,
                    body = body ?: BANG_QUALIFIED_EMPTY,
                    badgeNumber = newBadgeNumber,
                    pushToken = pushToken,
                    userInfo =
                        mapOf(
                            "conversationIDKey" to conversationIDKey,
                            "reactionMessageID" to (reactionMessageID ?: BANG_QUALIFIED_EMPTY),
                            "reactionSuffix" to (reactionSuffix ?: ""),
                            "recipientUserID" to user.id,
                            "userNumberHash" to userNumberHash,
                        ),
                    isReaction = isReaction,
                )
            }?.let { exception ->
                if (!exception.isEqual(AppException.stalePushToken)) return@let exceptions.add(exception)
                runCatchingException { services.pushToken.eraseStalePushToken(pushToken) }?.let(exceptions::add)
            }
        }

        exceptions.compiledException?.let { throw it.appending(userInfo) }
    }

    private suspend fun runCatchingException(block: suspend () -> Unit): Exception? =
        try {
            block()
            null
        } catch (exception: Exception) {
            exception
        }

    @Suppress("LongParameterList")
    private suspend fun sendNotification(
        title: String,
        body: String,
        badgeNumber: Int,
        pushToken: String,
        userInfo: Map<String, String>,
        isReaction: Boolean,
    ) {
        val accessToken = generateAccessToken()

        val notificationParameters = JSONObject().put("title", title)
        if (!body.isBangQualifiedEmpty) notificationParameters.put("body", body)

        val aps =
            JSONObject()
                .put("badge", badgeNumber)
                .put("mutable-content", 1)
                .put("sound", if (isReaction) "Reaction.caf" else "default")

        val payload =
            JSONObject().put(
                "message",
                JSONObject()
                    .put("android", JSONObject().put("priority", "high"))
                    .put("apns", JSONObject().put("payload", JSONObject().put("aps", aps)))
                    .put("data", JSONObject(userInfo))
                    .put("notification", notificationParameters)
                    .put("token", pushToken),
            )

        withContext(Dispatchers.IO) {
            val connection =
                (URL(FCM_SEND_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $accessToken")
                }

            try {
                connection.outputStream.use { it.write(payload.toString().toByteArray()) }
                val responseCode = connection.responseCode
                if (responseCode.isSuccessfulHTTPStatus) return@withContext

                val responseBody =
                    (connection.errorStream ?: connection.inputStream)
                        ?.bufferedReader()
                        ?.use(BufferedReader::readText)
                        .orEmpty()

                throw sendNotificationException(
                    responseBody,
                    responseCode = responseCode,
                    pushToken = pushToken,
                )
            } catch (exception: Exception) {
                throw exception
            } catch (throwable: Throwable) {
                throw Exception.from(throwable, ExceptionMetadata(this@NotificationService))
            } finally {
                connection.disconnect()
            }
        }
    }

    internal fun sendNotificationException(
        responseBody: String,
        responseCode: Int,
        pushToken: String,
    ): Exception {
        if (responseBody.contains(UNREGISTERED_RESPONSE_SUBSTRING) &&
            responseCode == HTTP_NOT_FOUND
        ) {
            return Exception(
                "The provided push token is stale.",
                isReportable = false,
                userInfo = mapOf("PushToken" to pushToken),
                metadata = ExceptionMetadata(this),
            )
        }

        return Exception(
            "Failed to decode URL response or status did not indicate success.",
            isReportable = !responseBody.contains(UNREGISTERED_RESPONSE_SUBSTRING),
            userInfo =
                mapOf(
                    "ResponseBody" to responseBody,
                    "URLResponseCode" to responseCode,
                ),
            metadata = ExceptionMetadata(this),
        )
    }

    private suspend fun updateHostedBadgeNumber(
        badgeNumber: Int? = null,
        user: User,
    ) {
        val newBadgeNumber =
            if (user.id == User.currentUserID) {
                badgeNumber ?: user.calculateBadgeNumber()
            } else {
                badgeNumber
                    ?: throw Exception(
                        "Must supply badge number for users other than current user.",
                        metadata = ExceptionMetadata(this),
                    )
            }

        networking.database.setValue(
            max(0, newBadgeNumber),
            listOf(
                NetworkPath.users.rawValue,
                user.id,
                BADGE_NUMBER_KEY,
            ).joinToString("/"),
        )
    }

    private suspend fun <T> List<T>.forEachConcurrently(action: suspend (T) -> Unit) {
        coroutineScope {
            map { async { action(it) } }.awaitAll()
        }
    }

    private val Int.isSuccessfulHTTPStatus: Boolean
        get() = this in HTTP_OK_MIN..HTTP_OK_MAX
}

// MARK: - Constants

private const val ACCESS_TOKEN_URL = "https://us-central1-jaguar-5d735.cloudfunctions.net/generateAccessToken"
private const val BADGE_NUMBER_KEY = "badgeNumber"
private const val FCM_SEND_URL = "https://fcm.googleapis.com/v1/projects/jaguar-5d735/messages:send"
private const val HTTP_NOT_FOUND = 404
private const val HTTP_OK_MAX = 299
private const val HTTP_OK_MIN = 200
private const val UNREGISTERED_RESPONSE_SUBSTRING = "UNREGISTERED"
