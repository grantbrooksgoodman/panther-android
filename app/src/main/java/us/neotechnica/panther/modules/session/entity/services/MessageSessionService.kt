//
//  MessageSessionService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import us.neotechnica.panther.bundle.notifications
import us.neotechnica.panther.modules.common.services.AnalyticsService
import us.neotechnica.panther.modules.common.services.NotificationService
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.Participant
import us.neotechnica.panther.modules.networking.conversation.services.ConversationService
import us.neotechnica.panther.modules.networking.message.models.MediaFile
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.message.models.RichMessageContent
import us.neotechnica.panther.modules.networking.message.services.MessageService
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.entity.constants.MessageSessionServiceFloats
import us.neotechnica.panther.modules.session.state.services.PendingTranslationArchive
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.translation.extensions.reference
import us.neotechnica.panther.networking.modules.translation.models.ArchiveStrategy
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import us.neotechnica.panther.translator.services.LanguageRecognitionService

// MARK: - Constants Accessors

private typealias Floats = MessageSessionServiceFloats

/**
 * The service that sends text and media messages.
 */
object MessageSessionService {
    // MARK: - Properties

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val hostedTranslation get() = Networking.config.hostedTranslationDelegate

    // MARK: - Send Media Message

    /**
     * Sends a media message to the given users in the given
     * conversation.
     *
     * When no conversation is provided, a new conversation is created
     * for the message.
     *
     * @param mediaFile The media to send.
     * @param presetID A preset identifier to use for the message, or
     *   `null` to generate one.
     * @param toUsers The users to send the message to.
     * @param inConversation The conversation to send the message in.
     *   Pass `null` to create a new conversation.
     * @param isPenPalsConversation A Boolean value that indicates
     *   whether the conversation is a PenPals conversation.
     *
     * @return The conversation the message was sent in.
     *
     * @throws Exception if the current user is unavailable or the
     *   message cannot be sent.
     */
    suspend fun sendMediaMessage(
        mediaFile: MediaFile,
        presetID: String? = null,
        toUsers: List<User>,
        inConversation: Conversation?,
        isPenPalsConversation: Boolean,
    ): Conversation {
        val currentUser =
            DependencyValues.current.clientSession.entity.user.currentUser
                ?: throw Exception(
                    "Current user has not been set.",
                    metadata = ExceptionMetadata(this),
                )

        return createMessageAndAddToConversation(
            conversation = inConversation,
            isPenPalsConversation = isPenPalsConversation,
            initiatingUser = currentUser,
            otherUsers = toUsers,
            presetID = presetID,
            richContent = RichMessageContent.Media(mediaFile),
            translations = null,
        )
    }

    // MARK: - Send Text Message

    /**
     * Sends a text message to the given users in the given
     * conversation.
     *
     * The text is translated into each recipient's language and
     * delivered. When no conversation is provided, a new conversation
     * is created for the message.
     *
     * @param text The text to send.
     * @param presetID A preset identifier to use for the message, or
     *   `null` to generate one.
     * @param toUsers The users to send the message to.
     * @param inConversation The conversation to send the message in.
     *   Pass `null` to create a new conversation.
     * @param isPenPalsConversation A Boolean value that indicates
     *   whether the conversation is a PenPals conversation.
     *
     * @return The conversation the message was sent in.
     *
     * @throws Exception if the current user is unavailable, translation
     *   fails, or the message cannot be sent.
     */
    suspend fun sendTextMessage(
        text: String,
        presetID: String? = null,
        toUsers: List<User>,
        inConversation: Conversation?,
        isPenPalsConversation: Boolean,
    ): Conversation {
        val currentUser =
            DependencyValues.current.clientSession.entity.user.currentUser
                ?: throw Exception(
                    "Current user has not been set.",
                    metadata = ExceptionMetadata(this),
                )

        var text = text
        if (Build.isDeveloperModeEnabled &&
            LanguageRecognitionService.shared.matchConfidence(text, currentUser.languageCode) <
            Floats.LANGUAGE_RECOGNITION_SERVICE_MATCH_CONFIDENCE_THRESHOLD
        ) {
            try {
                text =
                    hostedTranslation
                        .translate(
                            input = TranslationInput(text),
                            languagePair = LanguagePair(from = "en", to = currentUser.languageCode),
                        ).output
                        .sanitized
            } catch (exception: Exception) {
                Logger.log(exception)
            }
        }

        val users = toUsers.filter { it != currentUser }
        val uniqueLanguageCodes = users.map { it.languageCode }.distinct()

        val sourceLanguageCode =
            resolveSourceLanguageCode(
                forText = text,
                currentUserLanguageCode = currentUser.languageCode,
                recipientLanguageCodes = uniqueLanguageCodes,
            )

        val translations =
            coroutineScope {
                uniqueLanguageCodes
                    .map { languageCode ->
                        async {
                            val translation =
                                hostedTranslation.translate(
                                    input = TranslationInput(text),
                                    languagePair =
                                        LanguagePair(
                                            from = sourceLanguageCode,
                                            to = languageCode,
                                        ),
                                    archiveStrategy = ArchiveStrategy.DEFERRED,
                                )

                            recordPendingArchiveEntry(translation)
                            translation
                        }
                    }.awaitAll()
            }

        if (translations.isEmpty() || !translations.all { it.isWellFormed }) {
            throw Exception(
                "Translations fail validation.",
                metadata = ExceptionMetadata(this),
            )
        }

        return createMessageAndAddToConversation(
            conversation = inConversation,
            isPenPalsConversation = isPenPalsConversation,
            initiatingUser = currentUser,
            otherUsers = users,
            presetID = presetID,
            richContent = null,
            translations = translations,
        )
    }

    // MARK: - Auxiliary

    @Suppress("LongParameterList")
    private suspend fun createMessageAndAddToConversation(
        conversation: Conversation?,
        isPenPalsConversation: Boolean,
        initiatingUser: User,
        otherUsers: List<User>,
        presetID: String? = null,
        richContent: RichMessageContent?,
        translations: List<Translation>?,
    ): Conversation {
        suspend fun addMessage(
            message: Message,
            to: Conversation,
        ): Conversation {
            incrementDeliveryProgress(
                inConversation = to,
                by = Floats.ADD_MESSAGE_DELIVERY_PROGRESS_INCREMENT,
            )

            return ConversationSessionService.addMessages(listOf(message), to)
        }

        fun notifyUsers(
            ofMessage: Message,
            conversationIDKey: String,
        ) {
            backgroundScope.launch {
                try {
                    NotificationService.notify(
                        users = otherUsers.filter { !(it.blockedUserIDs ?: emptyList()).contains(initiatingUser.id) },
                        message = ofMessage,
                        conversationIDKey = conversationIDKey,
                    )
                } catch (exception: Exception) {
                    Logger.log(
                        exception,
                        domain = LoggerDomain.notifications,
                    )
                }

                incrementDeliveryProgress(
                    inConversation = conversation,
                    by = Floats.NOTIFY_DELIVERY_PROGRESS_INCREMENT,
                )
            }
        }

        incrementDeliveryProgress(
            inConversation = conversation,
            by = Floats.CREATE_MESSAGE_DELIVERY_PROGRESS_INCREMENT,
        )

        val message =
            MessageService.buildMessage(
                fromAccountID = initiatingUser.id,
                presetID = presetID,
                richContent = richContent,
                translations = translations,
            )

        if (conversation != null) {
            notifyUsers(
                ofMessage = message,
                conversationIDKey = conversation.id.key,
            )

            // Participant un-delete is merged into the
            // willWrite(.messages) atomic fan-out.
            return addMessage(
                message,
                to = conversation,
            )
        }

        val participantUsers = listOf(initiatingUser) + otherUsers

        AnalyticsService.logEvent(AnalyticsService.AnalyticsEvent.CREATE_NEW_CONVERSATION)
        incrementDeliveryProgress(
            inConversation = conversation,
            by = Floats.CREATE_CONVERSATION_DELIVERY_PROGRESS_INCREMENT,
        )

        val createdConversation =
            ConversationService.createConversation(
                firstMessage = message,
                isPenPalsConversation = isPenPalsConversation,
                participants = participantUsers.map { Participant(userID = it.id) },
            )

        notifyUsers(
            ofMessage = message,
            conversationIDKey = createdConversation.id.key,
        )

        return createdConversation
    }

    private fun incrementDeliveryProgress(
        inConversation: Conversation?,
        by: Float,
    ) {
        if (!shouldAnimateDeliveryProgress(inConversation)) return
        mainScope.launch {
            DependencyValues.current.clientSession.deliveryProgressIndicator
                ?.incrementDeliveryProgress(by)
        }
    }

    /**
     * Deferred-archival translations archive atomically with the
     * message commit; the entry waits in [PendingTranslationArchive]
     * until the commit drains it into its fan-out payload.
     */
    private fun recordPendingArchiveEntry(translation: Translation) {
        val archiveEntry = hostedTranslation.hostedArchiveEntry(translation) ?: return
        PendingTranslationArchive.record(archiveEntry, translation.reference.hostingKey)
    }

    /**
     * An input written in another participant's language becomes the
     * translation source for every recipient.
     */
    private suspend fun resolveSourceLanguageCode(
        forText: String,
        currentUserLanguageCode: String,
        recipientLanguageCodes: List<String>,
    ): String {
        val candidateLanguageCodes = recipientLanguageCodes.filter { it != currentUserLanguageCode }
        if (candidateLanguageCodes.isEmpty() ||
            LanguageRecognitionService.shared.matchConfidence(forText, currentUserLanguageCode) >=
            Floats.LANGUAGE_RECOGNITION_SERVICE_MATCH_CONFIDENCE_THRESHOLD
        ) {
            return currentUserLanguageCode
        }

        var bestMatch: Pair<String, Float>? = null
        for (languageCode in candidateLanguageCodes) {
            val confidence = LanguageRecognitionService.shared.matchConfidence(forText, languageCode)

            if (confidence < Floats.LANGUAGE_RECOGNITION_SERVICE_MATCH_CONFIDENCE_THRESHOLD ||
                confidence <= (bestMatch?.second ?: 0f)
            ) {
                continue
            }

            bestMatch = languageCode to confidence
        }

        return bestMatch?.first ?: currentUserLanguageCode
    }

    private fun shouldAnimateDeliveryProgress(inConversation: Conversation?): Boolean =
        DependencyValues.current.clientSession.entity.conversation.currentConversation
            ?.id
            ?.key == inConversation?.id?.key

    /** The string with the translation processing sentinels removed. */
    private val String.sanitized: String
        get() = replace("⁂", "").replace("⌘", "").replace("※", "")
}
