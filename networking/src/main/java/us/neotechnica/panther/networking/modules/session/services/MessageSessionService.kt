//
//  MessageSessionService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.conversation.services.ConversationService
import us.neotechnica.panther.networking.modules.message.services.MediaMessageService
import us.neotechnica.panther.networking.modules.message.services.MessageService
import us.neotechnica.panther.networking.modules.common.services.AnalyticsService
import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation
import us.neotechnica.panther.networking.modules.schema.conversation.models.Participant
import us.neotechnica.panther.networking.modules.schema.message.models.MediaFile
import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.translation.models.ArchiveStrategy
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import us.neotechnica.panther.translator.services.LanguageRecognitionService
import us.neotechnica.panther.networking.modules.translation.models.TranslationReference as HostedTranslationReference

/**
 * Sends text messages, translating them into each recipient's language.
 *
 * Sends into an existing conversation or creates a new one, then
 * notifies recipients. Audio and media messages arrive with later
 * phases.
 */
object MessageSessionService {
    // MARK: - Properties

    private val hostedTranslation get() = Networking.config.hostedTranslationDelegate

    private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // MARK: - Send Text Message

    /**
     * Sends a text message to [users], appending it to [conversation]
     * or creating a new conversation when [conversation] is `null`.
     *
     * The text is translated into each recipient's language and archived
     * atomically with the message commit.
     *
     * @return The updated (or newly created) conversation.
     *
     * @throws Exception if the current user is unavailable, translation
     *   fails, or the message cannot be sent.
     */
    suspend fun sendTextMessage(
        text: String,
        presetID: String?,
        users: List<User>,
        conversation: Conversation?,
        isPenPalsConversation: Boolean = false,
    ): Conversation {
        val currentUser =
            UserSessionService.currentUser
                ?: throw Exception("Current user has not been set.", metadata = ExceptionMetadata(this))

        val recipients = users.filter { it.id != currentUser.id }
        val uniqueLanguageCodes = recipients.map { it.languageCode }.distinct()
        val sourceLanguageCode =
            resolveSourceLanguageCode(text, currentUser.languageCode, uniqueLanguageCodes)

        val translations =
            coroutineScope {
                uniqueLanguageCodes
                    .map { languageCode ->
                        async { translate(text, sourceLanguageCode, languageCode) }
                    }.awaitAll()
            }

        if (translations.isEmpty() || !translations.all { it.isWellFormed }) {
            throw Exception("Translations fail validation.", metadata = ExceptionMetadata(this))
        }

        val message = MessageService.buildTextMessage(currentUser.id, presetID, translations)
        return createMessageAndAddToConversation(
            conversation = conversation,
            initiatingUser = currentUser,
            otherUsers = recipients,
            message = message,
            isPenPalsConversation = isPenPalsConversation,
        )
    }

    // MARK: - Send Media Message

    /**
     * Sends [mediaFile] as a media message to the given recipients.
     *
     * Mirrors the iOS ordering: the media and its thumbnail are uploaded
     * first, then the message is written and recipients are notified.
     *
     * @return The updated (or newly created) conversation.
     *
     * @throws Exception if the current user is unavailable, the upload
     *   fails, or the message cannot be sent.
     */
    suspend fun sendMediaMessage(
        mediaFile: MediaFile,
        users: List<User>,
        conversation: Conversation?,
        isPenPalsConversation: Boolean = false,
        presetID: String? = null,
    ): Conversation {
        val currentUser =
            UserSessionService.currentUser
                ?: throw Exception("Current user has not been set.", metadata = ExceptionMetadata(this))

        val recipients = users.filter { it.id != currentUser.id }
        val message = MessageService.buildMediaMessage(currentUser.id, mediaFile, presetID)

        MediaMessageService.uploadMediaComponent(mediaFile, message)

        return createMessageAndAddToConversation(
            conversation = conversation,
            initiatingUser = currentUser,
            otherUsers = recipients,
            message = message,
            isPenPalsConversation = isPenPalsConversation,
        )
    }

    // MARK: - Auxiliary

    private suspend fun createMessageAndAddToConversation(
        conversation: Conversation?,
        initiatingUser: User,
        otherUsers: List<User>,
        message: Message,
        isPenPalsConversation: Boolean,
    ): Conversation {
        val resolvedConversation =
            if (conversation != null) {
                ConversationSessionService.addMessages(listOf(message), conversation)
            } else {
                val participants = (listOf(initiatingUser) + otherUsers).map { Participant(userID = it.id) }
                AnalyticsService.logEvent(AnalyticsService.AnalyticsEvent.CREATE_NEW_CONVERSATION)
                ConversationService.createConversation(
                    firstMessage = message,
                    isPenPalsConversation = isPenPalsConversation,
                    participants = participants,
                )
            }

        // Push is fire-and-forget and best-effort: a slow or failed push
        // must never block the send or strand the outbox entry, which would
        // otherwise leave a mock lingering beside the committed message.
        notificationScope.launch {
            runCatching {
                NotificationSessionService.notify(
                    users = otherUsers,
                    message = message,
                    conversationIDKey = resolvedConversation.id.key,
                )
            }.onFailure { Logger.log("Push notification failed: $it") }
        }

        return resolvedConversation
    }

    private suspend fun translate(
        text: String,
        sourceLanguageCode: String,
        languageCode: String,
    ): Translation {
        val translation =
            hostedTranslation.translate(
                input = TranslationInput(text),
                languagePair = LanguagePair(from = sourceLanguageCode, to = languageCode),
                archiveStrategy = ArchiveStrategy.DEFERRED,
            )

        hostedTranslation.hostedArchiveEntry(translation)?.let { entry ->
            PendingTranslationArchive.record(entry, HostedTranslationReference.from(translation).hostingKey)
        }
        return translation
    }

    /**
     * An input written in another participant's language becomes the
     * translation source for every recipient.
     */
    private suspend fun resolveSourceLanguageCode(
        text: String,
        currentUserLanguageCode: String,
        recipientLanguageCodes: List<String>,
    ): String {
        val candidates = recipientLanguageCodes.filter { it != currentUserLanguageCode }
        if (candidates.isEmpty() ||
            LanguageRecognitionService.shared.matchConfidence(text, currentUserLanguageCode) >= MATCH_CONFIDENCE_THRESHOLD
        ) {
            return currentUserLanguageCode
        }

        var best: Pair<String, Float>? = null
        for (languageCode in candidates) {
            val confidence = LanguageRecognitionService.shared.matchConfidence(text, languageCode)
            if (confidence >= MATCH_CONFIDENCE_THRESHOLD && confidence > (best?.second ?: 0f)) {
                best = languageCode to confidence
            }
        }
        return best?.first ?: currentUserLanguageCode
    }

    // MARK: - Companion

    private const val MATCH_CONFIDENCE_THRESHOLD = 0.8f
}
