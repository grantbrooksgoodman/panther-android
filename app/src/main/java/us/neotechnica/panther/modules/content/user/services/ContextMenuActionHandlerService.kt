//
//  ContextMenuActionHandlerService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.R
import us.neotechnica.panther.bundle.reactionDetailsPageView
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.sanitized
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAction
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.designsystem.modules.foundation.rootsheet.RootSheet
import us.neotechnica.panther.designsystem.modules.foundation.rootsheet.RootSheets
import us.neotechnica.panther.modules.common.services.DocumentExportService
import us.neotechnica.panther.modules.common.services.ErrorReportingService
import us.neotechnica.panther.modules.common.services.TextToSpeechService
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewServiceFloats
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewServiceStrings
import us.neotechnica.panther.modules.content.user.extensions.isConsentMessage
import us.neotechnica.panther.modules.content.user.extensions.isFromCurrentUser
import us.neotechnica.panther.modules.content.user.extensions.messageLabelText
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.message.models.MediaFile
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.networking.modules.translation.extensions.system
import us.neotechnica.panther.subsystem.modules.foundation.extensions.containsLetters
import us.neotechnica.panther.subsystem.modules.foundation.extensions.lowercasedTrimmingWhitespaceAndNewlines
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.services.LanguageRecognitionService
import java.io.File

// MARK: - Constants Accessors

private typealias ContextMenuFloats = ChatPageViewServiceFloats.ContextMenu
private typealias ContextMenuStrings = ChatPageViewServiceStrings.ContextMenu

/**
 * The service that builds context menus and handles their actions.
 *
 * [ContextMenuActionHandlerService] composes the menu shown for a
 * message, tailoring the available actions to the message's content and
 * state, and performs each action the user chooses – copying text,
 * saving media, speaking a message aloud, viewing an alternate
 * translation, and reporting a translation. It also tracks the message
 * currently being spoken aloud.
 *
 * @param dependencies The collaborators the service acts through; the
 *   defaults resolve the live services.
 */
class ContextMenuActionHandlerService(
    private val dependencies: Dependencies = Dependencies(),
) {
    // MARK: - Types

    /**
     * The collaborators the context menu action handler acts through.
     *
     * @property copyToPasteboard Copies the given text to the system
     *   pasteboard.
     * @property currentConversation Returns the current conversation.
     * @property currentUser Returns the current user.
     * @property fileReport Files an error report for the given
     *   exception.
     * @property isDisplayingAlternateText Returns whether the given
     *   message displays its alternate text.
     * @property isDisplayingAudioTranscription Returns whether the
     *   given message displays its audio transcription.
     * @property isSpeaking Returns whether speech synthesis is in
     *   progress.
     * @property isTextToSpeechSupported Returns whether the device has
     *   a voice installed for the given language code.
     * @property matchConfidence Returns the confidence that the given
     *   text is in the given language.
     * @property presentExportController Presents the document export
     *   picker for the given file.
     * @property presentReactionDetails Presents the reaction details
     *   page for the given message identifier.
     * @property reportedErrorCodes Returns the error codes already
     *   reported during this session.
     * @property saveMedia Saves the given media file to the device's
     *   shared collections.
     * @property scope The scope on which asynchronous actions run.
     * @property showSuccessHUD Shows the success HUD.
     * @property speak Speaks the given text in the given language for
     *   the given message identifier, returning whether the utterance
     *   was queued.
     * @property stopPlayback Stops audio message playback.
     * @property stopSpeakingIfNeeded Stops speech synthesis if in
     *   progress.
     * @property toggleAlternate Toggles the given kind of alternate
     *   content for the given message.
     */
    @Suppress("LongParameterList")
    data class Dependencies(
        val copyToPasteboard: (String) -> Unit = { text -> copyToSystemPasteboard(text) },
        val currentConversation: () -> Conversation? = { ConversationSessionService.currentConversation },
        val currentUser: () -> User? = { UserSessionService.currentUser },
        val fileReport: (Exception) -> Unit = { ErrorReportingService.fileReport(it) },
        val isDisplayingAlternateText: (Message) -> Boolean = { AlternateMessageService.isDisplayingAlternateText(it) },
        val isDisplayingAudioTranscription: (Message) -> Boolean = { AlternateMessageService.isDisplayingAudioTranscription(it) },
        val isSpeaking: () -> Boolean = { TextToSpeechService.isSpeaking },
        val isTextToSpeechSupported: (String) -> Boolean = { TextToSpeechService.isTextToSpeechSupported(it) },
        val matchConfidence: suspend (String, String) -> Float = { text, languageCode ->
            LanguageRecognitionService.shared.matchConfidence(text, languageCode)
        },
        val presentExportController: (File) -> Unit = { DocumentExportService.presentExportController(forFileAt = it) },
        val presentReactionDetails: (String) -> Unit = { RootSheets.present(RootSheet.reactionDetailsPageView(it)) },
        val reportedErrorCodes: () -> List<String> = { ErrorReportingService.reportedErrorCodes },
        val saveMedia: suspend (MediaFile) -> Unit = { MediaActionHandlerService.saveMedia(it) },
        val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
        val showSuccessHUD: () -> Unit = { HUD.showSuccess() },
        val speak: (String, String, String) -> Boolean = { text, languageCode, messageID ->
            TextToSpeechService.speak(text, languageCode, messageID)
        },
        val stopPlayback: () -> Unit = { AudioMessagePlaybackService.stopPlayback() },
        val stopSpeakingIfNeeded: () -> Unit = { TextToSpeechService.stopSpeakingIfNeeded() },
        val toggleAlternate: (AlternateMessageService.AlternateMessageType, Message) -> Unit = { type, message ->
            AlternateMessageService.toggle(type, message)
        },
    )

    // MARK: - Properties

    /**
     * The message currently being spoken aloud, or `null` if no
     * message is being spoken.
     */
    var speakingMessage: Message? by mutableStateOf(null)
        private set

    // MARK: - Menu for Message

    /**
     * Returns the context menu actions for the given message.
     *
     * The available actions depend on the message's content type and
     * state: media messages offer to save the file; audio messages
     * offer to view the transcription or audio, copy the transcription,
     * and speak it aloud; text messages offer to copy or speak the
     * text, view an alternate translation, and report the translation.
     * Speaking is offered only when the device has a voice installed
     * for the message's language. A reaction details action is added
     * for any message that has reactions.
     *
     * @param message The message to build the menu for.
     *
     * @return The context menu actions for the message, or `null` if
     *   the message is a consent message.
     */
    fun menuForMessage(message: Message): List<ContextMenuAction>? {
        if (message.isConsentMessage) return null
        val actions = mutableListOf<ContextMenuAction>()
        if (message.reactions != null) {
            actions.add(
                ContextMenuAction(
                    title = LocalizedStringKey.ReactionDetails.localized(),
                    systemImageName = ContextMenuStrings.REACTION_DETAILS_ACTION_IMAGE_SYSTEM_NAME,
                ) { handleAction(ContextMenuStrings.REACTION_DETAILS_ACTION_IDENTIFIER_RAW_VALUE, message) },
            )
        }

        if (message.contentType.isMedia) {
            actions.add(
                ContextMenuAction(
                    title = LocalizedStringKey.SaveFile.localized(),
                    systemImageName = ContextMenuStrings.SAVE_ACTION_IMAGE_SYSTEM_NAME,
                ) { handleAction(ContextMenuStrings.SAVE_ACTION_IDENTIFIER_RAW_VALUE, message) },
            )

            return actions
        }

        if (message.contentType.isAudio) return actions + getAudioMessageActions(message)

        actions.addAll(getTextMessageMenuActions(message))
        return actions
    }

    // MARK: - Reset Speaking Message

    /** Clears the message tracked as currently being spoken aloud. */
    fun resetSpeakingMessage() {
        speakingMessage = null
    }

    // MARK: - Action Handlers

    private fun handleAudioMessageAction(message: Message) {
        dependencies.toggleAlternate(AlternateMessageService.AlternateMessageType.AUDIO_TRANSCRIPTION, message)
    }

    private fun handleCopyAction(message: Message) {
        val text = message.messageLabelText ?: return
        dependencies.copyToPasteboard(text)
    }

    private fun handleReactionDetailsAction(message: Message) {
        dependencies.presentReactionDetails(message.id)
    }

    private fun handleReportMistranslationAction(message: Message) {
        val hostingKey = message.translationReferences?.firstOrNull()?.hostingKey ?: return
        dependencies.fileReport(mistranslationException(hostingKey))
    }

    private fun handleSaveAction(message: Message) {
        val imageOrVideo = message.imageComponent ?: message.videoComponent
        if (imageOrVideo != null) {
            dependencies.scope.launch {
                try {
                    dependencies.saveMedia(imageOrVideo)
                    dependencies.showSuccessHUD()
                } catch (exception: Exception) {
                    Logger.log(exception, with = AlertType.toast)
                }
            }
            return
        }

        val documentFile = message.documentComponent?.localPathFile ?: return
        try {
            dependencies.presentExportController(documentFile)
            DocumentExportService.onDismiss { cancelled ->
                if (!cancelled) dependencies.showSuccessHUD()
            }
        } catch (exception: Exception) {
            Logger.log(exception, with = AlertType.toast)
        }
    }

    private fun handleSpeakAction(message: Message) {
        dependencies.scope.launch {
            val messageLabelText = message.messageLabelText ?: return@launch
            if (dependencies.isSpeaking()) return@launch dependencies.stopSpeakingIfNeeded()

            dependencies.stopPlayback()

            val (currentUserUtteranceLanguageCode, notCurrentUserUtteranceLanguageCode) = utteranceLanguageCodes(message)
            var utteranceLanguageCode = utteranceLanguageCode(message)
            val alternateUtteranceLanguageCode =
                listOf(currentUserUtteranceLanguageCode, notCurrentUserUtteranceLanguageCode)
                    .firstOrNull { it != utteranceLanguageCode } ?: utteranceLanguageCode

            // The menu offers Speak only for a language with an installed
            // voice, so the language switches only to another such language.
            val threshold = ContextMenuFloats.LANGUAGE_RECOGNITION_MATCH_CONFIDENCE_THRESHOLD
            if (dependencies.matchConfidence(messageLabelText, utteranceLanguageCode) <= threshold &&
                dependencies.matchConfidence(messageLabelText, notCurrentUserUtteranceLanguageCode) >= threshold &&
                processed(message.translation?.input?.value) == processed(message.translation?.output) &&
                dependencies.isTextToSpeechSupported(alternateUtteranceLanguageCode)
            ) {
                utteranceLanguageCode = alternateUtteranceLanguageCode
            }

            if (!dependencies.speak(messageLabelText, utteranceLanguageCode, message.id)) return@launch
            speakingMessage = message
        }
    }

    private fun handleViewAlternateAction(message: Message) {
        dependencies.toggleAlternate(AlternateMessageService.AlternateMessageType.ALTERNATE_TEXT, message)
    }

    // MARK: - Auxiliary

    private fun getAudioMessageActions(message: Message): List<ContextMenuAction> {
        val actions = mutableListOf<ContextMenuAction>()

        val isDisplayingAudioTranscription = dependencies.isDisplayingAudioTranscription(message)
        val actionTitleKey = if (isDisplayingAudioTranscription) LocalizedStringKey.ViewAsAudio else LocalizedStringKey.ViewTranscription
        val actionTitle = actionTitleKey.localized()

        if (isDisplayingAudioTranscription) {
            actions.add(
                ContextMenuAction(
                    title = LocalizedStringKey.Copy.localized(),
                    systemImageName = ContextMenuStrings.COPY_ACTION_IMAGE_SYSTEM_NAME,
                ) { handleAction(ContextMenuStrings.COPY_ACTION_IDENTIFIER_RAW_VALUE, message) },
            )
        }

        val isSpeaking = dependencies.isSpeaking()
        if (isSpeaking || (isDisplayingAudioTranscription && message.isFromCurrentUser && isSpeakable(message))) {
            actions.add(
                ContextMenuAction(
                    title = (if (isSpeaking) LocalizedStringKey.StopSpeaking else LocalizedStringKey.Speak).localized(),
                    systemImageName = ContextMenuStrings.SPEAK_ACTION_IMAGE_SYSTEM_NAME,
                ) { handleAction(ContextMenuStrings.SPEAK_ACTION_IDENTIFIER_RAW_VALUE, message) },
            )
        }

        val audioMessageActionImage =
            if (isDisplayingAudioTranscription) {
                ContextMenuStrings.AUDIO_MESSAGE_ACTION_IMAGE_SYSTEM_NAME
            } else {
                ContextMenuStrings.AUDIO_MESSAGE_ACTION_ALTERNATE_IMAGE_SYSTEM_NAME
            }

        actions.add(
            ContextMenuAction(
                title = actionTitle,
                systemImageName = audioMessageActionImage,
            ) { handleAction(ContextMenuStrings.AUDIO_MESSAGE_ACTION_IDENTIFIER_RAW_VALUE, message) },
        )

        return actions
    }

    private fun getViewAlternateAction(message: Message): ContextMenuAction? {
        val translation = message.translation ?: return null
        val isOneToOne = dependencies.currentConversation()?.participants?.size == 2
        if (!(isOneToOne || !message.isFromCurrentUser)) return null
        if (translation.languagePair.isIdempotent) return null
        if (!translation.input.value.sanitized.containsLetters) return null

        val languageCode = if (message.isFromCurrentUser) translation.languagePair.to else translation.languagePair.from
        if (languageCode == dependencies.currentUser()?.languageCode) return null
        if (dependencies.isSpeaking()) return null

        val isDisplayingAlternateText = dependencies.isDisplayingAlternateText(message)
        val actionTitleKey =
            if (message.isFromCurrentUser) {
                if (isDisplayingAlternateText) LocalizedStringKey.ViewOriginal else LocalizedStringKey.ViewTranslation
            } else {
                if (isDisplayingAlternateText) LocalizedStringKey.ViewTranslation else LocalizedStringKey.ViewOriginal
            }

        return ContextMenuAction(
            title = actionTitleKey.localized(),
            systemImageName = ContextMenuStrings.VIEW_ALTERNATE_ACTION_IMAGE_SYSTEM_NAME,
            imageResourceID = R.drawable.view_alternate,
        ) { handleAction(ContextMenuStrings.VIEW_ALTERNATE_ACTION_IDENTIFIER_RAW_VALUE, message) }
    }

    private fun handleAction(
        identifier: String,
        message: Message,
    ) {
        when (identifier) {
            ContextMenuStrings.AUDIO_MESSAGE_ACTION_IDENTIFIER_RAW_VALUE -> handleAudioMessageAction(message)
            ContextMenuStrings.COPY_ACTION_IDENTIFIER_RAW_VALUE -> handleCopyAction(message)
            ContextMenuStrings.REACTION_DETAILS_ACTION_IDENTIFIER_RAW_VALUE -> handleReactionDetailsAction(message)
            ContextMenuStrings.REPORT_MISTRANSLATION_ACTION_IDENTIFIER_RAW_VALUE -> handleReportMistranslationAction(message)
            ContextMenuStrings.SAVE_ACTION_IDENTIFIER_RAW_VALUE -> handleSaveAction(message)
            ContextMenuStrings.SPEAK_ACTION_IDENTIFIER_RAW_VALUE -> handleSpeakAction(message)
            ContextMenuStrings.VIEW_ALTERNATE_ACTION_IDENTIFIER_RAW_VALUE -> handleViewAlternateAction(message)
            else -> Unit
        }
    }

    private fun getTextMessageMenuActions(message: Message): List<ContextMenuAction> {
        val isSpeaking = dependencies.isSpeaking()
        val speakActionImage =
            if (isSpeaking) {
                ContextMenuStrings.SPEAK_ACTION_ALTERNATE_IMAGE_SYSTEM_NAME
            } else {
                ContextMenuStrings.SPEAK_ACTION_IMAGE_SYSTEM_NAME
            }

        val actions =
            mutableListOf(
                ContextMenuAction(
                    title = LocalizedStringKey.Copy.localized(),
                    systemImageName = ContextMenuStrings.COPY_ACTION_IMAGE_SYSTEM_NAME,
                ) { handleAction(ContextMenuStrings.COPY_ACTION_IDENTIFIER_RAW_VALUE, message) },
            )

        if (isSpeaking || isSpeakable(message)) {
            actions.add(
                ContextMenuAction(
                    title = (if (isSpeaking) LocalizedStringKey.StopSpeaking else LocalizedStringKey.Speak).localized(),
                    systemImageName = speakActionImage,
                ) { handleAction(ContextMenuStrings.SPEAK_ACTION_IDENTIFIER_RAW_VALUE, message) },
            )
        }

        val translation = message.translation
        val containsLetters = translation?.input?.value?.containsLetters == true || translation?.output?.sanitized?.containsLetters == true
        val isDisplayingAlternate = dependencies.isDisplayingAlternateText(message)
        val hostingKey = translation?.let { message.translationReferences?.firstOrNull()?.hostingKey }

        val isOutputDistinct =
            translation?.output?.lowercasedTrimmingWhitespaceAndNewlines !=
                translation?.input?.value?.lowercasedTrimmingWhitespaceAndNewlines
        if (!isOutputDistinct || isSpeaking || !containsLetters || hostingKey == null) return actions

        // The report is offered only while the translation itself is on
        // display – a mistranslation must be visible to be reported.
        val isDisplayingTranslation = if (message.isFromCurrentUser) isDisplayingAlternate else !isDisplayingAlternate
        val exceptionCode = mistranslationException(hostingKey).code
        if (translation.languagePair.isIdempotent == false &&
            isDisplayingTranslation &&
            exceptionCode !in dependencies.reportedErrorCodes()
        ) {
            actions.add(
                ContextMenuAction(
                    title = LocalizedStringKey.ReportMistranslation.localized(),
                    systemImageName = ContextMenuStrings.REPORT_MISTRANSLATION_ACTION_IMAGE_SYSTEM_NAME,
                ) { handleAction(ContextMenuStrings.REPORT_MISTRANSLATION_ACTION_IDENTIFIER_RAW_VALUE, message) },
            )
        }

        getViewAlternateAction(message)?.let { actions.add(it) }
        return actions
    }

    private fun mistranslationException(hostingKey: String): Exception =
        Exception(
            "A mistranslation has been reported (${hostingKey.shortCode}).",
            userInfo =
                mapOf(
                    "Descriptor" to "A mistranslation has been reported.",
                    "HostedOverrideErrorCode" to "CA45",
                    "ReferenceHostingKey" to hostingKey,
                ),
            metadata = ExceptionMetadata(this),
        )

    // Whether the device has a voice installed for the language in which
    // the given message is spoken.
    private fun isSpeakable(message: Message): Boolean = dependencies.isTextToSpeechSupported(utteranceLanguageCode(message))

    private fun processed(string: String?): String = string?.lowercasedTrimmingWhitespaceAndNewlines?.sanitized ?: ""

    // The language in which the given message is spoken before language
    // recognition is consulted: the sender's side of its language pair.
    private fun utteranceLanguageCode(message: Message): String {
        val (currentUserUtteranceLanguageCode, notCurrentUserUtteranceLanguageCode) = utteranceLanguageCodes(message)
        return if (message.isFromCurrentUser) currentUserUtteranceLanguageCode else notCurrentUserUtteranceLanguageCode
    }

    // The current user's and the other participant's sides of the given
    // message's language pair, swapped while its alternate text is on
    // display.
    private fun utteranceLanguageCodes(message: Message): Pair<String, String> {
        val languagePair = message.translation?.languagePair ?: LanguagePair.system
        if (dependencies.isDisplayingAlternateText(message)) return languagePair.to to languagePair.from
        return languagePair.from to languagePair.to
    }

    private val String.shortCode: String
        get() = "${take(2)}${takeLast(2)}".uppercase()

    // MARK: - Companion

    companion object {
        private var appContext: Context? = null

        /** Prepares the service with the application context. */
        fun initialize(context: Context) {
            appContext = context.applicationContext
        }

        private fun copyToSystemPasteboard(text: String) {
            val clipboard = appContext?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            clipboard.setPrimaryClip(ClipData.newPlainText(null, text))
        }
    }
}
