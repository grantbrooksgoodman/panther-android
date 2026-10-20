//
//  PantherMessagingService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.notificationextension

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import us.neotechnica.panther.Application
import us.neotechnica.panther.R
import us.neotechnica.panther.bundle.delegates.MainActivity
import us.neotechnica.panther.bundle.navigators.UserContentNavigatorState
import us.neotechnica.panther.bundle.navigators.UserContentRoute
import us.neotechnica.panther.bundle.notifications
import us.neotechnica.panther.designsystem.modules.foundation.services.SoundPlayer
import us.neotechnica.panther.designsystem.modules.foundation.toast.Toast
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.common.services.sources.HapticsService
import us.neotechnica.panther.modules.common.services.sources.PushTokenService
import us.neotechnica.panther.modules.content.user.extensions.currentUserID
import us.neotechnica.panther.modules.content.user.extensions.isVisibleForCurrentUser
import us.neotechnica.panther.modules.content.user.models.ChatPageStateServiceEffectID
import us.neotechnica.panther.modules.content.user.services.ChatPageStateService
import us.neotechnica.panther.modules.networking.sources.user.models.User
import us.neotechnica.panther.modules.session.ClientSession
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.navigation.Navigation
import us.neotechnica.panther.navigation.PendingChatNavigation
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.navigation
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
    private val hapticsService: HapticsService by Dependency { it.commonServices.haptics }
    private val navigation: Navigation by Dependency { it.navigation }
    private val pushTokenService: PushTokenService by Dependency { it.commonServices.pushToken }

    // MARK: - Properties

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // MARK: - Token

    override fun onNewToken(token: String) {
        pushTokenService.setCurrentToken(token)
        if (User.currentUserID == null) return
        scope.launch {
            runCatching { pushTokenService.updatePushTokensForCurrentUser() }
                .onFailure { Logger.log(Exception.from(it, ExceptionMetadata(this@PantherMessagingService))) }
        }
    }

    // MARK: - Message

    override fun onMessageReceived(message: RemoteMessage) {
        scope.launch(Dispatchers.Main) {
            try {
                // The `notification` block is absent from Android pushes;
                // a push sent with it still carries the same data keys.
                val received = ReceivedNotification.from(message.data)
                val lifecycleState = ProcessLifecycleOwner.get().lifecycle.currentState
                if (!lifecycleState.isAtLeast(Lifecycle.State.STARTED)) {
                    return@launch showSystemNotification(message, received)
                }

                respondToInAppNotification(message, received)
            } catch (exception: Exception) {
                Logger.log(
                    exception,
                    domain = LoggerDomain.notifications,
                )
            }
        }
    }

    // MARK: - Respond to In-app Notification

    private fun respondToInAppNotification(
        message: RemoteMessage,
        received: ReceivedNotification,
    ) {
        val body = message.notification?.body ?: received.body
        val currentUser =
            clientSession.entity.user.currentUser
                ?: throw Exception(
                    "No current user – will not respond to notification.",
                    isReportable = false,
                    metadata = ExceptionMetadata(this),
                )

        if (received.recipientUserID != currentUser.id) {
            throw Exception(
                "Notification not intended for current user – ignoring.",
                isReportable = false,
                metadata = ExceptionMetadata(this),
            )
        }

        val conversationIDKey = received.conversationIDKey
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
            if (!received.isReaction) return
            return hapticsService.generateFeedback(HapticsService.HapticFeedbackStyle.MEDIUM)
        }

        val title = enrichedTitle(message, received)
        val focusedMessageID = if (received.isReaction) received.reactionMessageID else null
        val toast =
            Toast(
                Toast.ToastType.Capsule(),
                title = title.ifBlank { null },
                message = body,
                perpetuation = Toast.PerpetuationStrategy.Ephemeral(IN_APP_TOAST_SECONDS.seconds),
            )

        // The notification sound plays beside the toast.
        playNotificationSound(this, isReaction = received.isReaction)
        Toast.show(toast) {
            val chatRoute =
                Route.UserContent(
                    UserContentRoute.Push(
                        UserContentNavigatorState.SeguePaths.Chat(
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
        received: ReceivedNotification,
    ) {
        val conversationIDKey = received.conversationIDKey
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
            title = enrichedTitle(message, received),
            body = message.notification?.body ?: received.body,
            subtitle = subtitle,
            isReaction = received.isReaction,
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
    private fun enrichedTitle(
        message: RemoteMessage,
        received: ReceivedNotification,
    ): String {
        val payloadTitle = message.notification?.title ?: received.title ?: getString(R.string.app_name)
        val userNumberHash =
            message.data[NotificationExtensionConstants.USER_NUMBER_HASH_USER_INFO_KEY] ?: return payloadTitle
        val fullName = contactNameForNumberHash(userNumberHash) ?: return payloadTitle

        if (!received.isReaction) return fullName

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

        /** The identifier of the reactions notification channel. */
        const val REACTIONS_CHANNEL_ID = "reactions"

        /**
         * Creates the messages and reactions notification
         * channels (idempotent).
         */
        fun createChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(
                    MESSAGES_CHANNEL_ID,
                    context.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )

            val reactions =
                NotificationChannel(
                    REACTIONS_CHANNEL_ID,
                    context.getString(R.string.reactions_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                )
            reactions.setSound(
                reactionSoundURI(context),
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            manager.createNotificationChannel(reactions)
        }

        /**
         * Plays the reaction sound, or the default notification
         * sound.
         */
        private fun playNotificationSound(
            context: Context,
            isReaction: Boolean,
        ) {
            if (isReaction) return SoundPlayer.play(R.raw.reaction)
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            runCatching { RingtoneManager.getRingtone(context, uri)?.play() }
        }

        private fun reactionSoundURI(context: Context): Uri =
            "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.reaction}".toUri()

        @Suppress("LongParameterList")
        private fun showNotification(
            context: Context,
            conversationIDKey: String,
            title: String,
            body: String,
            subtitle: String?,
            isReaction: Boolean,
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
                    .Builder(context, if (isReaction) REACTIONS_CHANNEL_ID else MESSAGES_CHANNEL_ID)
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

        private const val IN_APP_TOAST_SECONDS = 5L
    }
}
