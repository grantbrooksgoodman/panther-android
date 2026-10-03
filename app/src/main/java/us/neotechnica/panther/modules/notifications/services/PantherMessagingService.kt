//
//  PantherMessagingService.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
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
import us.neotechnica.panther.MainActivity
import us.neotechnica.panther.R
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.navigation.PendingChatNavigation
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.entity.extensions.currentUserID
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.designsystem.modules.foundation.toast.Toast
import us.neotechnica.panther.modules.common.services.HapticsService
import us.neotechnica.panther.modules.common.services.PushTokenService
import us.neotechnica.panther.modules.session.entity.extensions.isVisibleForCurrentUser
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentNavigatorState
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import kotlin.time.Duration.Companion.seconds
import us.neotechnica.panther.modules.common.constants.NotificationExtensionConstants
import org.json.JSONObject
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

/**
 * Receives FCM messages and token updates.
 *
 * A new token is registered against the current user's record; an
 * incoming message shows a tap-to-open notification, suppressed while
 * its conversation is already on screen.
 */
class PantherMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // MARK: - Token

    override fun onNewToken(token: String) {
        PushTokenService.setCurrentToken(token)
        if (User.currentUserID == null) return
        scope.launch {
            runCatching { PushTokenService.updatePushTokensForCurrentUser() }
                .onFailure { Logger.log(Exception.from(it, exceptionMetadata())) }
        }
    }

    // MARK: - Message

    override fun onMessageReceived(message: RemoteMessage) {
        val conversationIDKey = message.data[PendingChatNavigation.CONVERSATION_ID_KEY_EXTRA] ?: return

        scope.launch(Dispatchers.Main) {
            val lifecycleState = ProcessLifecycleOwner.get().lifecycle.currentState
            // In the foreground, respond in-app (toast + haptic) as iOS's
            // `respondToInAppNotification` does, rather than posting a system notification.
            if (lifecycleState.isAtLeast(Lifecycle.State.STARTED)) {
                respondToInAppNotification(message, conversationIDKey)
            } else {
                showSystemNotification(message, conversationIDKey)
            }
        }
    }

    private fun showSystemNotification(
        message: RemoteMessage,
        conversationIDKey: String,
    ) {
        // Suppress while the conversation is already on screen.
        if (ConversationSessionService.currentConversation?.id?.key == conversationIDKey) return

        val body = message.notification?.body ?: message.data[BODY_KEY].orEmpty()

        // Enrich the title with the sender's contact name, and a group conversation's name as
        // the subtitle, mirroring the iOS notification extension.
        val title = enrichedTitle(message)
        val subtitle =
            SessionStore
                .getConversation(conversationIDKey)
                ?.metadata
                ?.name
                ?.takeUnless { it.isBangQualifiedEmpty || it.isBlank() }
                ?: persistedConversationName(conversationIDKey)

        showNotification(this, conversationIDKey, title, body, subtitle)
    }

    /**
     * Responds to a foreground message as iOS's `respondToInAppNotification`:
     * verifies the message is for the current user and its conversation is
     * visible, then either gives haptic feedback for a reaction to the
     * on-screen conversation or shows a tap-to-navigate in-app toast.
     */
    private fun respondToInAppNotification(
        message: RemoteMessage,
        conversationIDKey: String,
    ) {
        val currentUser = UserSessionService.currentUser ?: return
        val recipientUserID = message.data[RECIPIENT_USER_ID_KEY] ?: return
        if (recipientUserID != currentUser.id) return

        val conversation = SessionStore.getConversation(conversationIDKey) ?: return
        if (!conversation.isVisibleForCurrentUser) return

        val reactionMessageID = message.data[REACTION_MESSAGE_ID_KEY]
        val isReaction = reactionMessageID != null && reactionMessageID != NO_REACTION && reactionMessageID.isNotBlank()

        // Already viewing this conversation: a reaction gives haptic feedback; nothing otherwise.
        if (ConversationSessionService.currentConversation?.id?.key == conversationIDKey) {
            if (isReaction) HapticsService.generateFeedback(HapticsService.HapticFeedbackStyle.MEDIUM)
            return
        }

        val title = enrichedTitle(message)
        val body = message.notification?.body ?: message.data[BODY_KEY].orEmpty()
        val focusedMessageID = if (isReaction) reactionMessageID else null

        Toast.show(
            Toast(
                Toast.Type.Capsule(),
                title = title.ifBlank { null },
                message = body,
                perpetuation = Toast.Perpetuation.Ephemeral(IN_APP_TOAST_SECONDS.seconds),
            ),
            onTap = {
                DependencyValues.current.navigation.navigate(
                    Route.UserContent(
                        UserContentRoute.Push(UserContentNavigatorState.SeguePath.Chat(conversationIDKey, focusedMessageID)),
                    ),
                )
            },
        )
    }

    /**
     * The notification title enriched with the sender's contact name: the
     * contact's full name, or "<full name> <reactionSuffix>" for a
     * reaction, falling back to the payload title when the sender is not a
     * known contact.
     */
    private fun enrichedTitle(message: RemoteMessage): String {
        val fallback = message.notification?.title ?: message.data[TITLE_KEY] ?: getString(R.string.app_name)
        val userNumberHash = message.data[USER_NUMBER_HASH_KEY] ?: return fallback
        val fullName = ContactService.nameForNumberHash(userNumberHash) ?: return fallback

        val reactionMessageID = message.data[REACTION_MESSAGE_ID_KEY]
        val reactionSuffix = message.data[REACTION_SUFFIX_KEY].orEmpty()
        return if (reactionMessageID != null && reactionMessageID != NO_REACTION && reactionSuffix.isNotEmpty()) {
            "$fullName $reactionSuffix"
        } else {
            fullName
        }
    }

    /**
     * The persisted group conversation name for cold-start subtitle
     * resolution, when the session store has not yet loaded.
     */
    private fun persistedConversationName(conversationIDKey: String): String? {
        val nameMap = Persistent.string(PersistentStorageKey(NotificationExtensionConstants.CONVERSATION_NAME_MAP_KEY)) ?: return null
        return runCatching { JSONObject(nameMap).optString(conversationIDKey) }.getOrNull()?.takeUnless { it.isBlank() }
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
                    .setSmallIcon(android.R.drawable.sym_action_email)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    // Group per conversation, matching the iOS per-thread grouping.
                    .setGroup(conversationIDKey)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
            if (subtitle != null) builder.setSubText(subtitle)

            NotificationManagerCompat
                .from(context)
                .notify(conversationIDKey.hashCode(), builder.build())
        }

        // MARK: - Data Keys

        /** The push payload's message-title field. */
        private const val TITLE_KEY = "title"

        /** The push payload's message-body field. */
        private const val BODY_KEY = "body"

        /** The push payload's sender-number-hash field. */
        private const val USER_NUMBER_HASH_KEY = "userNumberHash"

        /** The push payload's reaction-message-identifier field. */
        private const val RECIPIENT_USER_ID_KEY = "recipientUserID"
        private const val IN_APP_TOAST_SECONDS = 5L
        private const val REACTION_MESSAGE_ID_KEY = "reactionMessageID"

        /** The push payload's reaction-suffix field. */
        private const val REACTION_SUFFIX_KEY = "reactionSuffix"

        /** The sentinel `reactionMessageID` for a non-reaction message. */
        private const val NO_REACTION = "!"
    }

    // MARK: - Auxiliary

    private fun exceptionMetadata() =
        us.neotechnica.panther.subsystem.modules.foundation.models
            .ExceptionMetadata(this)
}
