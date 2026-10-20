//
//  NotificationService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

@file:Suppress("FileLength")

package us.neotechnica.panther.modules.common.services.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import us.neotechnica.panther.Application
import us.neotechnica.panther.bundle.LocalizedStringKey
import us.neotechnica.panther.bundle.navigators.UserContentNavigatorState
import us.neotechnica.panther.bundle.navigators.UserContentRoute
import us.neotechnica.panther.bundle.notRegisteredForPushNotifications
import us.neotechnica.panther.bundle.stalePushToken
import us.neotechnica.panther.bundle.users
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.sanitized
import us.neotechnica.panther.modules.common.extensions.runCatchingException
import us.neotechnica.panther.modules.common.services.CommonServices
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.content.user.extensions.currentUserID
import us.neotechnica.panther.modules.content.user.models.ChatPageStateServiceEffectID
import us.neotechnica.panther.modules.content.user.services.ChatPageStateService
import us.neotechnica.panther.modules.networking.sources.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.sources.message.models.HostedContentType
import us.neotechnica.panther.modules.networking.sources.message.models.Message
import us.neotechnica.panther.modules.networking.sources.user.models.DeviceID
import us.neotechnica.panther.modules.networking.sources.user.models.User
import us.neotechnica.panther.modules.session.ClientSession
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.navigation.Navigation
import us.neotechnica.panther.navigation.PendingChatNavigation
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.common.dependencies.networking
import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.common.extensions.digits
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.common.models.NetworkServices
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.extensions.compiledException
import us.neotechnica.panther.subsystem.modules.foundation.extensions.forEachConcurrently
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
@Suppress("LargeClass")
object NotificationService {
    // MARK: - Dependencies

    private val clientSession: ClientSession by Dependency { it.clientSession }
    private val navigation: Navigation by Dependency { it.navigation }
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
     * Notification content is localized to each recipient's
     * language, and each recipient's hosted badge number is
     * incremented atomically. Recipients who have not registered
     * for push notifications are skipped; stale push tokens are
     * erased. Each recipient's push tokens are read from the
     * database at send time, not taken from the given users,
     * whose copies may predate a reinstall or a new sign-in.
     *
     * @param users The users to notify.
     * @param ofReaction The reaction the notification describes;
     *   pass `null` to describe the message itself. The default
     *   is `null`.
     * @param message The message the notification describes.
     * @param conversationIDKey The identifier key of the
     *   conversation containing the message.
     *
     * @throws Exception if the current user has not been set, or
     *   if delivery fails.
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

    // MARK: - Respond to Notification Tap

    /**
     * Opens the conversation a tapped notification names.
     *
     * On a signed-in activity already showing the user content, the
     * stack pops to the conversation list and the chat is pushed – an
     * open chat is dismissed first. Otherwise the key is held for the
     * splash flow to consume after initialization.
     *
     * @param conversationIDKey The identifier key of the conversation
     *   the notification names.
     */
    fun respondToNotificationTap(conversationIDKey: String) {
        val isSignedIn = User.currentUserID != null && clientSession.entity.user.currentUser != null
        if (!isSignedIn || navigation.state.value.modal != RootNavigatorState.ModalPaths.UserContent) {
            return PendingChatNavigation.set(conversationIDKey)
        }

        PendingChatNavigation.consume()
        val chatRoute =
            Route.UserContent(UserContentRoute.Push(UserContentNavigatorState.SeguePaths.Chat(conversationIDKey)))
        navigation.navigate(Route.UserContent(UserContentRoute.Stack(emptyList())))
        if (!ChatPageStateService.isPresented) return navigation.navigate(chatRoute)

        ChatPageStateService.addEffectUponIsPresented(
            state = false,
            id = ChatPageStateServiceEffectID.deeplinkToOtherChat,
        ) {
            Application.dismissSheets()
            navigation.navigate(chatRoute)
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

    /**
     * The push tokens in a user's hosted push token map, or `null` if
     * the value holds none.
     *
     * @param hostedValue The value stored at the user's push tokens
     *   path.
     */
    internal fun pushTokens(hostedValue: Any?): List<String>? {
        val pushTokens =
            (hostedValue as? Map<*, *>)
                ?.keys
                ?.filterIsInstance<String>()
                ?.sorted()
                .orEmpty()
        return if (pushTokens.isBangQualifiedEmpty) null else pushTokens
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
            currentPushTokens(user)
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
                    deviceID = user.deviceID,
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

    @Suppress("LongParameterList")
    private suspend fun sendNotification(
        title: String,
        body: String,
        badgeNumber: Int,
        pushToken: String,
        deviceID: String,
        userInfo: Map<String, String>,
        isReaction: Boolean,
    ) {
        val accessToken = generateAccessToken()
        val payload =
            JSONObject().put(
                "message",
                fcmMessage(
                    title = title,
                    body = body,
                    badgeNumber = badgeNumber,
                    pushToken = pushToken,
                    deviceID = deviceID,
                    userInfo = userInfo,
                    isReaction = isReaction,
                ),
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

    /**
     * Builds the FCM message for one recipient token.
     *
     * A recipient whose device identifier carries the Android
     * prefix receives a data-only message – the title and body
     * travel in `data`, so the app renders the notification
     * itself – at high priority. Every other recipient receives
     * the `notification` block and the APNs payload.
     */
    @Suppress("LongParameterList")
    internal fun fcmMessage(
        title: String,
        body: String,
        badgeNumber: Int,
        pushToken: String,
        deviceID: String,
        userInfo: Map<String, String>,
        isReaction: Boolean,
    ): JSONObject {
        val data = JSONObject(userInfo)
        val message =
            JSONObject()
                .put("android", JSONObject().put("priority", "high"))
                .put("token", pushToken)

        if (deviceID.startsWith(DeviceID.PREFIX)) {
            data.put(TITLE_KEY, title)
            if (!body.isBangQualifiedEmpty) data.put(BODY_KEY, body)
            return message.put("data", data)
        }

        val notificationParameters = JSONObject().put(TITLE_KEY, title)
        if (!body.isBangQualifiedEmpty) notificationParameters.put(BODY_KEY, body)

        val aps =
            JSONObject()
                .put("badge", badgeNumber)
                .put("mutable-content", 1)
                .put("sound", if (isReaction) "Reaction.caf" else "default")

        return message
            .put("apns", JSONObject().put("payload", JSONObject().put("aps", aps)))
            .put("data", data)
            .put("notification", notificationParameters)
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

    // A recipient is fetched into the session store once and never
    // refreshed, so its cached push tokens go stale when the recipient
    // reinstalls, clears data, or signs in again. The tokens are read
    // from the database, with the cached ones as a fallback only if
    // the read fails.
    private suspend fun currentPushTokens(user: User): List<String>? {
        val pushTokensPath =
            listOf(
                NetworkPath.users.rawValue,
                user.id,
                User.SerializableKey.PUSH_TOKENS.rawValue,
            ).joinToString("/")

        return try {
            pushTokens(networking.database.getValues(pushTokensPath, cacheStrategy = CacheStrategy.DISREGARD_CACHE))
        } catch (exception: Exception) {
            if (exception.isEqual(to = AppException.Networking.Database.noValueExists)) null else user.pushTokens
        }
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

    private val Int.isSuccessfulHTTPStatus: Boolean
        get() = this in HTTP_OK_MIN..HTTP_OK_MAX
}

// MARK: - Constants

private const val BODY_KEY = "body"
private const val TITLE_KEY = "title"
private const val ACCESS_TOKEN_URL = "https://us-central1-jaguar-5d735.cloudfunctions.net/generateAccessToken"
private const val BADGE_NUMBER_KEY = "badgeNumber"
private const val FCM_SEND_URL = "https://fcm.googleapis.com/v1/projects/jaguar-5d735/messages:send"
private const val HTTP_NOT_FOUND = 404
private const val HTTP_OK_MAX = 299
private const val HTTP_OK_MIN = 200
private const val UNREGISTERED_RESPONSE_SUBSTRING = "UNREGISTERED"
