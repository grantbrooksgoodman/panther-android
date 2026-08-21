//
//  MessageSessionService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.services

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.message.services.MessageService
import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.translation.models.ArchiveStrategy
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import us.neotechnica.panther.translator.services.LanguageRecognitionService
import us.neotechnica.panther.networking.modules.translation.models.TranslationReference as HostedTranslationReference

/**
 * Sends text messages, translating them into each recipient's language.
 *
 * **Note:** this Phase 7 port sends text into existing conversations
 * only; new-conversation creation, audio, media, and push notification
 * of recipients arrive with later phases.
 */
object MessageSessionService {
    // MARK: - Properties

    private val hostedTranslation get() = Networking.config.hostedTranslationDelegate

    // MARK: - Send Text Message

    /**
     * Sends a text message to [users] in [conversation].
     *
     * The text is translated into each recipient's language, archived
     * atomically with the message commit, and appended to the
     * conversation.
     *
     * @return The updated conversation.
     *
     * @throws Exception if the current user is unavailable, translation
     *   fails, or the message cannot be sent.
     */
    suspend fun sendTextMessage(
        text: String,
        presetID: String?,
        users: List<User>,
        conversation: Conversation,
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
        // NOTE: push notification of recipients is deferred to Phase 8.
        return ConversationSessionService.addMessages(listOf(message), conversation)
    }

    // MARK: - Auxiliary

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
