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
import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.networking.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.message.models.HostedContentType
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHashOf
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

/**
 * Sends push notifications about a message – or a reaction to it – to
 * a message's recipients.
 *
 * For each recipient the service atomically increments their hosted
 * badge, then delivers an FCM v1 message to each of their registered
 * push tokens, localized to the recipient's language. An unregistered
 * token is erased from every user that holds it.
 *
 * **Note:** the notification title is a formatted phone number; the
 * receiving client enriches it with the sender's contact name using
 * the `userNumberHash` data field.
 */
object NotificationService {
    // MARK: - Types

    private data class NotificationRequest(
        val senderTitle: String,
        val userNumberHash: String,
        val reaction: Reaction?,
        val message: Message,
        val conversationIDKey: String,
    )

    private data class DeliveryPayload(
        val title: String,
        val body: String,
        val badgeNumber: Int,
        val data: Map<String, String>,
        val isReaction: Boolean,
    )

    // MARK: - Properties

    private val database get() = Networking.config.databaseDelegate

    // MARK: - Set Badge Number

    /**
     * Updates the current user's hosted badge number.
     *
     * Negative values are clamped to zero.
     *
     * @param badgeNumber The badge number to set.
     * @param updateHostedValue Whether the current user's hosted badge
     *   number should be updated. The default is `true`.
     *
     * @throws Exception if updating the hosted value fails.
     */
    suspend fun setBadgeNumber(
        badgeNumber: Int,
        updateHostedValue: Boolean = true,
    ) {
        // Android exposes no direct app-icon badge API; the hosted
        // value is authoritative, and the launcher badge is derived
        // from posted notifications.
        val currentUser = UserSessionService.currentUser
        if (!updateHostedValue || currentUser == null) return
        updateHostedBadgeNumber(max(0, badgeNumber), currentUser)
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
     * @throws Exception if the current user has not been set.
     */
    suspend fun notify(
        users: List<User>,
        ofReaction: Reaction? = null,
        message: Message,
        conversationIDKey: String,
    ) {
        val currentUser =
            UserSessionService.currentUser
                ?: throw Exception("Current user has not been set.", metadata = ExceptionMetadata(this))

        val request =
            NotificationRequest(
                senderTitle = senderTitle(currentUser),
                userNumberHash = encodedHashOf(listOf(currentUser.phoneNumber.nationalNumberString)),
                reaction = ofReaction,
                message = message,
                conversationIDKey = conversationIDKey,
            )

        // Notify every recipient concurrently; a recipient with no registered
        // push tokens is skipped silently (see notifyUser), and any other failure propagates.
        coroutineScope {
            users.map { user -> async { notifyUser(user, request) } }.awaitAll()
        }
    }

    // MARK: - Auxiliary

    private suspend fun notifyUser(
        user: User,
        request: NotificationRequest,
    ) {
        val isReaction = request.reaction != null
        val reactionSuffix =
            request.reaction?.let {
                "${LocalizedStringKey.Reacted.localized(languageCode = user.languageCode)} ${it.style.emojiValue}"
            } ?: ""
        val title = if (request.reaction == null) request.senderTitle else "${request.senderTitle} $reactionSuffix"

        var body = notificationBody(request.message, user)
        if (isReaction && body != null && request.message.contentType == HostedContentType.Text) {
            body = "“$body”"
        }

        val data =
            mapOf(
                "conversationIDKey" to request.conversationIDKey,
                "reactionMessageID" to if (isReaction) request.message.id else "!",
                "reactionSuffix" to reactionSuffix,
                "recipientUserID" to user.id,
                "userNumberHash" to request.userNumberHash,
            )

        // Atomic badge increment to avoid read-modify-write races.
        val committed =
            database.runTransaction("${NetworkPath.users.rawValue}/${user.id}/$BADGE_NUMBER_KEY") { current ->
                max(0, ((current as? Number)?.toInt() ?: 0) + 1)
            }
        val newBadgeNumber = (committed as? Number)?.toInt() ?: 0

        val pushTokens = user.pushTokens ?: return
        val payload = DeliveryPayload(title, body ?: "", newBadgeNumber, data, isReaction)
        for (pushToken in pushTokens) {
            val stale = withContext(Dispatchers.IO) { sendNotification(payload, pushToken) }
            if (stale) PushTokenService.eraseStalePushToken(pushToken)
        }
    }

    private fun notificationBody(
        message: Message,
        user: User,
    ): String? =
        when (val contentType = message.contentType) {
            HostedContentType.Text ->
                message.translations
                    ?.firstOrNull { it.languagePair.to == user.languageCode }
                    ?.output
                    ?: message.translations?.firstOrNull()?.output

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
        }

    private fun senderTitle(currentUser: User): String = currentUser.phoneNumber.formattedString()

    /**
     * Delivers a single FCM v1 message. Returns whether the token was
     * rejected as unregistered (stale).
     */
    private fun sendNotification(
        payload: DeliveryPayload,
        pushToken: String,
    ): Boolean {
        val accessToken = generateAccessToken()

        val notification = JSONObject().put("title", payload.title)
        if (payload.body.isNotBlank()) notification.put("body", payload.body)

        val aps =
            JSONObject()
                .put("badge", payload.badgeNumber)
                .put("mutable-content", 1)
                .put("sound", if (payload.isReaction) "Reaction.caf" else "default")
        val apns = JSONObject().put("payload", JSONObject().put("aps", aps))
        val android = JSONObject().put("priority", "high")

        val messageObject =
            JSONObject()
                .put("apns", apns)
                .put("android", android)
                .put("data", JSONObject(payload.data))
                .put("notification", notification)
                .put("token", pushToken)
        val messagePayload = JSONObject().put("message", messageObject)

        val connection =
            (URL(FCM_SEND_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $accessToken")
            }

        return try {
            connection.outputStream.use { it.write(messagePayload.toString().toByteArray()) }
            val code = connection.responseCode
            if (code.isSuccessfulHttpStatus) {
                false
            } else {
                val responseBody =
                    (connection.errorStream ?: connection.inputStream)
                        ?.bufferedReader()
                        ?.use(BufferedReader::readText)
                        .orEmpty()
                Logger.log("FCM send failed ($code): $responseBody")
                code == HTTP_NOT_FOUND || responseBody.contains("UNREGISTERED")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun generateAccessToken(): String {
        val connection =
            (URL(ACCESS_TOKEN_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
            }
        return try {
            val code = connection.responseCode
            val stream = if (code.isSuccessfulHttpStatus) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            if (!code.isSuccessfulHttpStatus) {
                throw Exception("Failed to generate access token ($code).", metadata = ExceptionMetadata(this))
            }
            response
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun updateHostedBadgeNumber(
        badgeNumber: Int,
        user: User,
    ) {
        database.setValue(
            max(0, badgeNumber),
            "${NetworkPath.users.rawValue}/${user.id}/$BADGE_NUMBER_KEY",
        )
    }

    private val Int.isSuccessfulHttpStatus: Boolean
        get() = this in HTTP_OK_MIN..HTTP_OK_MAX

    // MARK: - Companion

    private const val ACCESS_TOKEN_URL = "https://us-central1-jaguar-5d735.cloudfunctions.net/generateAccessToken"
    private const val BADGE_NUMBER_KEY = "badgeNumber"
    private const val FCM_SEND_URL = "https://fcm.googleapis.com/v1/projects/jaguar-5d735/messages:send"
    private const val HTTP_NOT_FOUND = 404
    private const val HTTP_OK_MAX = 299
    private const val HTTP_OK_MIN = 200
}
