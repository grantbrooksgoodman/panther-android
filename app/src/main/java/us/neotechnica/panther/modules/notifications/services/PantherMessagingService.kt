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
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.MainActivity
import us.neotechnica.panther.R
import us.neotechnica.panther.navigation.PendingChatNavigation
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.extensions.currentUserID
import us.neotechnica.panther.networking.modules.session.services.ConversationSessionService
import us.neotechnica.panther.networking.modules.session.services.UserMutationService
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

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
        UserMutationService.setCurrentToken(token)
        if (User.currentUserID == null) return
        scope.launch {
            runCatching { UserMutationService.updatePushTokensForCurrentUser() }
                .onFailure { Logger.log(Exception.from(it, exceptionMetadata())) }
        }
    }

    // MARK: - Message

    override fun onMessageReceived(message: RemoteMessage) {
        val conversationIDKey = message.data[PendingChatNavigation.CONVERSATION_ID_KEY_EXTRA] ?: return

        // Suppress while the conversation is already on screen.
        if (ConversationSessionService.currentConversation?.id?.key == conversationIDKey) return

        val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        showNotification(this, conversationIDKey, title, body)
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

            val notification =
                NotificationCompat
                    .Builder(context, MESSAGES_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.sym_action_email)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()

            NotificationManagerCompat
                .from(context)
                .notify(conversationIDKey.hashCode(), notification)
        }
    }

    // MARK: - Auxiliary

    private fun exceptionMetadata() =
        us.neotechnica.panther.subsystem.modules.foundation.models
            .ExceptionMetadata(this)
}
