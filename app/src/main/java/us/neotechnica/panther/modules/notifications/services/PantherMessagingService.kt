//
//  PantherMessagingService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.notifications.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import us.neotechnica.panther.MainActivity
import us.neotechnica.panther.R
import us.neotechnica.panther.bundle.Application
import us.neotechnica.panther.bundle.notifications
import us.neotechnica.panther.designsystem.modules.foundation.toast.Toast
import us.neotechnica.panther.modules.common.constants.NotificationExtensionConstants
import us.neotechnica.panther.modules.common.services.HapticsService
import us.neotechnica.panther.modules.common.services.PushTokenService
import us.neotechnica.panther.modules.content.user.models.ChatPageStateServiceEffectID
import us.neotechnica.panther.modules.content.user.services.ChatPageStateService
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.ClientSession
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.entity.extensions.currentUserID
import us.neotechnica.panther.modules.session.entity.extensions.isVisibleForCurrentUser
import us.neotechnica.panther.navigation.PendingChatNavigation
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentNavigatorState
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import kotlin.time.Duration.Companion.seconds

/**
 * Receives push messages and push token updates.
 *
 * A new token is registered against the current user's record. An
 * incoming message received in the foreground is handled in-app –
 * with haptic feedback or a tap-to-open toast – and one received in
 * the background posts a tap-to-open system notification.
 */
class PantherMessagingService : FirebaseMessagingService() {
    // MARK: - Dependencies

    private val clientSession: ClientSession by Dependency { it.clientSession }

    // MARK: - Properties

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // MARK: - Token

    override fun onNewToken(token: String) {
        PushTokenService.setCurrentToken(token)
        if (User.currentUserID == null) return
        scope.launch {
            runCatching { PushTokenService.updatePushTokensForCurrentUser() }
                .onFailure { Logger.log(Exception.from(it, ExceptionMetadata(this@PantherMessagingService))) }
        }
    }

    // MARK: - Message

    override fun onMessageReceived(message: RemoteMessage) {
        val conversationIDKey =
            message.data[NotificationExtensionConstants.CONVERSATION_ID_KEY_USER_INFO_KEY] ?: return

        scope.launch(Dispatchers.Main) {
            val lifecycleState = ProcessLifecycleOwner.get().lifecycle.currentState
            if (!lifecycleState.isAtLeast(Lifecycle.State.STARTED)) {
                return@launch showSystemNotification(message, conversationIDKey)
            }

            try {
                respondToInAppNotification(message)
            } catch (exception: Exception) {
                Logger.log(
                    exception,
                    domain = LoggerDomain.notifications,
                )
            }
        }
    }

    // MARK: - Respond to In-app Notification

    private fun respondToInAppNotification(message: RemoteMessage) {
        val body = message.notification?.body ?: message.data[BODY_KEY].orEmpty()
        Logger.log(
            "Received notification.\n\"$body\"",
            domain = LoggerDomain.notifications,
        )

        val currentUser =
            clientSession.entity.user.currentUser
                ?: throw Exception(
                    "No current user – will not respond to notification.",
                    isReportable = false,
                    metadata = ExceptionMetadata(this),
                )

        val conversationIDKey = message.data[NotificationExtensionConstants.CONVERSATION_ID_KEY_USER_INFO_KEY]
        val reactionMessageID = message.data[NotificationExtensionConstants.REACTION_MESSAGE_ID_USER_INFO_KEY]
        val recipientUserID = message.data[RECIPIENT_USER_ID_USER_INFO_KEY]
        if (conversationIDKey == null ||
            reactionMessageID == null ||
            recipientUserID == null
        ) {
            throw Exception(
                "Failed to resolve required values.",
                metadata = ExceptionMetadata(this),
            )
        }

        if (recipientUserID != currentUser.id) {
            throw Exception(
                "Notification not intended for current user – ignoring.",
                isReportable = false,
                metadata = ExceptionMetadata(this),
            )
        }

        val conversation = clientSession.store.getConversation(conversationIDKey)
        if (conversation == null || !conversation.isVisibleForCurrentUser) {
            throw Exception(
                "Conversation associated with this notification is not visible to the current user.",
                isReportable = false,
                metadata = ExceptionMetadata(this),
            )
        }

        val currentConversationIDKey =
            clientSession
                .entity
                .conversation
                .currentConversation
                ?.id
                ?.key

        if (ChatPageStateService.isPresented &&
            currentConversationIDKey == conversationIDKey
        ) {
            if (reactionMessageID.isBangQualifiedEmpty) return
            return HapticsService.generateFeedback(HapticsService.HapticFeedbackStyle.MEDIUM)
        }

        val title = enrichedTitle(message)
        val focusedMessageID = if (reactionMessageID.isBangQualifiedEmpty) null else reactionMessageID
        val toast =
            Toast(
                Toast.ToastType.Capsule(),
                title = title.ifBlank { null },
                message = body,
                perpetuation = Toast.PerpetuationStrategy.Ephemeral(IN_APP_TOAST_SECONDS.seconds),
            )

        Toast.show(toast) {
            val navigation = DependencyValues.current.navigation
            val chatRoute =
                Route.UserContent(
                    UserContentRoute.Push(
                        UserContentNavigatorState.SeguePath.Chat(
                            conversationIDKey,
                            focusedMessageID = focusedMessageID,
                        ),
                    ),
                )

            if (!ChatPageStateService.isPresented) return@show navigation.navigate(chatRoute)

            navigation.navigate(Route.UserContent(UserContentRoute.Stack(emptyList())))
            ChatPageStateService.addEffectUponIsPresented(
                state = false,
                id = ChatPageStateServiceEffectID.deeplinkToOtherChat,
            ) {
                Application.dismissSheets()
                navigation.navigate(chatRoute)
            }
        }
    }

    // MARK: - Auxiliary

    private fun showSystemNotification(
        message: RemoteMessage,
        conversationIDKey: String,
    ) {
        val body = message.notification?.body ?: message.data[BODY_KEY].orEmpty()
        val subtitle =
            clientSession
                .store
                .getConversation(conversationIDKey)
                ?.metadata
                ?.name
                ?.takeUnless { it.isBangQualifiedEmpty }
                ?: persistedConversationName(conversationIDKey)

        showNotification(
            this,
            conversationIDKey = conversationIDKey,
            title = enrichedTitle(message),
            body = body,
            subtitle = subtitle,
        )
    }

    /**
     * The notification title enriched with the sender's contact name.
     *
     * A message's title becomes the contact's full name; a reaction's
     * becomes "<full name> <reactionSuffix>", or keeps the payload
     * title when the suffix is empty. The payload title is used when
     * the sender is not a known contact.
     */
    private fun enrichedTitle(message: RemoteMessage): String {
        val payloadTitle = message.notification?.title ?: message.data[TITLE_KEY] ?: getString(R.string.app_name)
        val userNumberHash =
            message.data[NotificationExtensionConstants.USER_NUMBER_HASH_USER_INFO_KEY] ?: return payloadTitle
        val fullName = contactNameForNumberHash(userNumberHash) ?: return payloadTitle

        val reactionMessageID = message.data[NotificationExtensionConstants.REACTION_MESSAGE_ID_USER_INFO_KEY]
        if (reactionMessageID == null || reactionMessageID == BANG_QUALIFIED_EMPTY) return fullName

        val reactionSuffix = message.data[NotificationExtensionConstants.REACTION_SUFFIX_USER_INFO_KEY].orEmpty()
        return if (reactionSuffix.isEmpty()) payloadTitle else "$fullName $reactionSuffix"
    }

    private fun contactNameForNumberHash(userNumberHash: String): String? {
        val nameMap =
            Persistent.string(
                PersistentStorageKey(NotificationExtensionConstants.CONTACT_ARCHIVE_DEFAULTS_KEY_NAME),
            ) ?: return null

        return runCatching { JSONObject(nameMap).optString(userNumberHash) }
            .getOrNull()
            ?.takeUnless { it.isBlank() }
    }

    private fun persistedConversationName(conversationIDKey: String): String? {
        val nameMap =
            Persistent.string(
                PersistentStorageKey(NotificationExtensionConstants.CONVERSATION_NAME_MAP_DEFAULTS_KEY_NAME),
            ) ?: return null

        return runCatching { JSONObject(nameMap).optString(conversationIDKey) }
            .getOrNull()
            ?.takeUnless { it.isBlank() }
    }

    // MARK: - Companion

    companion object {
        /** The identifier of the messages notification channel. */
        const val MESSAGES_CHANNEL_ID = "messages"

        /** Creates the messages notification channel (idempotent; API 26+). */
        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val channel =
                NotificationChannel(
                    MESSAGES_CHANNEL_ID,
                    context.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_HIGH,
                )
            manager.createNotificationChannel(channel)
        }

        private fun showNotification(
            context: Context,
            conversationIDKey: String,
            title: String,
            body: String,
            subtitle: String?,
        ) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            createChannel(context)

            val intent =
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(PendingChatNavigation.CONVERSATION_ID_KEY_EXTRA, conversationIDKey)
                }
            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    conversationIDKey.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            val builder =
                NotificationCompat
                    .Builder(context, MESSAGES_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_notification)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    // Group per conversation.
                    .setGroup(conversationIDKey)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
            if (subtitle != null) builder.setSubText(subtitle)

            NotificationManagerCompat
                .from(context)
                .notify(conversationIDKey.hashCode(), builder.build())
        }

        // MARK: - Constants

        private const val BODY_KEY = "body"
        private const val IN_APP_TOAST_SECONDS = 5L
        private const val RECIPIENT_USER_ID_USER_INFO_KEY = "recipientUserID"
        private const val TITLE_KEY = "title"
    }
}
