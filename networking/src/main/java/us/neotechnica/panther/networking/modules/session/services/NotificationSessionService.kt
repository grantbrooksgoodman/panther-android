//
//  NotificationSessionService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.schema.message.models.HostedContentType
import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHashOf
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

/**
 * Sends push notifications to a message's recipients.
 *
 * Mirrors the iOS `NotificationService` send path: for each recipient it
 * atomically increments their badge, then delivers a FCM v1 message to
 * each of their registered push tokens. An unregistered token is erased
 * from every user that holds it.
 *
 * **Note:** the notification title is a formatted phone number; the
 * receiving client enriches it with the sender's contact name using the
 * `userNumberHash` data field.
 */
object NotificationSessionService {
    // MARK: - Properties

    private val database get() = Networking.config.databaseDelegate

    // MARK: - Notify

    /**
     * Notifies each of [users] of the given [message].
     *
     * Recipients not registered for push notifications are skipped.
     * Delivery failures are logged and do not halt the batch.
     */
    suspend fun notify(
        users: List<User>,
        message: Message,
        conversationIDKey: String,
    ) {
        // NOTE: PenPals notification titles are deferred with the PenPals surface.
        val currentUser = UserSessionService.currentUser ?: return
        val title = senderTitle(currentUser)
        val userNumberHash = encodedHashOf(listOf(currentUser.phoneNumber.nationalNumberString))

        for (user in users) {
            try {
                notifyUser(
                    user = user,
                    title = title,
                    body = notificationBody(message, user),
                    conversationIDKey = conversationIDKey,
                    userNumberHash = userNumberHash,
                )
            } catch (exception: Exception) {
                Logger.log(exception)
            }
        }
    }

    // MARK: - Auxiliary

    private suspend fun notifyUser(
        user: User,
        title: String,
        body: String?,
        conversationIDKey: String,
        userNumberHash: String,
    ) {
        // Atomic badge increment to avoid read-modify-write races.
        val committed =
            database.runTransaction("${NetworkPath.users.rawValue}/${user.id}/$BADGE_NUMBER_KEY") { current ->
                max(0, ((current as? Number)?.toInt() ?: 0) + 1)
            }
        val newBadgeNumber = (committed as? Number)?.toInt() ?: 0

        val pushTokens = user.pushTokens ?: return

        val data =
            mapOf(
                "conversationIDKey" to conversationIDKey,
                "reactionMessageID" to "!",
                "reactionSuffix" to "",
                "recipientUserID" to user.id,
                "userNumberHash" to userNumberHash,
            )

        for (pushToken in pushTokens) {
            val stale =
                withContext(Dispatchers.IO) {
                    sendNotification(title, body ?: "", newBadgeNumber, pushToken, data)
                }
            if (stale) UserMutationService.eraseStalePushToken(pushToken)
        }
    }

    private fun senderTitle(currentUser: User): String =
        "+${currentUser.phoneNumber.callingCode} ${currentUser.phoneNumber.nationalNumberString}".trim()

    private fun notificationBody(
        message: Message,
        user: User,
    ): String? =
        when (message.contentType) {
            HostedContentType.Text ->
                message.translations
                    ?.firstOrNull { it.languagePair.to == user.languageCode }
                    ?.output
                    ?: message.translations?.firstOrNull()?.output
            else -> "📎"
        }

    /**
     * Delivers a single FCM v1 message. Returns whether the token was
     * rejected as unregistered (stale).
     */
    private fun sendNotification(
        title: String,
        body: String,
        badgeNumber: Int,
        pushToken: String,
        data: Map<String, String>,
    ): Boolean {
        val accessToken = generateAccessToken()

        val notification = JSONObject().put("title", title)
        if (body.isNotBlank()) notification.put("body", body)

        val aps =
            JSONObject()
                .put("badge", badgeNumber)
                .put("mutable-content", 1)
                .put("sound", "default")
        val apns = JSONObject().put("payload", JSONObject().put("aps", aps))
        val android = JSONObject().put("priority", "high")

        val messageObject =
            JSONObject()
                .put("apns", apns)
                .put("android", android)
                .put("data", JSONObject(data))
                .put("notification", notification)
                .put("token", pushToken)
        val payload = JSONObject().put("message", messageObject)

        val connection =
            (URL(FCM_SEND_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $accessToken")
            }

        return try {
            connection.outputStream.use { it.write(payload.toString().toByteArray()) }
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

    private val Int.isSuccessfulHttpStatus: Boolean
        get() = this in HTTP_OK_MIN..HTTP_OK_MAX

    // MARK: - Companion

    private const val HTTP_OK_MIN = 200
    private const val HTTP_OK_MAX = 299
    private const val FCM_SEND_URL = "https://fcm.googleapis.com/v1/projects/jaguar-5d735/messages:send"
    private const val ACCESS_TOKEN_URL = "https://us-central1-jaguar-5d735.cloudfunctions.net/generateAccessToken"
    private const val BADGE_NUMBER_KEY = "badgeNumber"
    private const val HTTP_NOT_FOUND = 404
}
