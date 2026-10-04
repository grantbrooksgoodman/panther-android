//
//  ChatMessageCell+ContextMenu.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAction
import us.neotechnica.panther.modules.common.services.TextToSpeechService
import us.neotechnica.panther.modules.content.user.services.ContextMenuActionHandlerService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.networking.message.models.MediaFile
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.session.entity.extensions.isFromCurrentUser
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage

private const val SAVE_ACTION_IMAGE_SYSTEM_NAME = "square.and.arrow.down"

/**
 * The context-menu actions for a media message: a reaction-details
 * action when the message has reactions, followed by a save-file action.
 */
internal fun mediaActionsFor(
    message: Message,
    mediaFile: MediaFile,
    onSaveMedia: (MediaFile) -> Unit,
): List<ContextMenuAction> =
    listOfNotNull(
        ContextMenuActionHandlerService.reactionDetailsAction(message),
        ContextMenuAction(
            title = LocalizedStringKey.SaveFile.localized(),
            systemImageName = SAVE_ACTION_IMAGE_SYSTEM_NAME,
        ) { onSaveMedia(mediaFile) },
    )

/**
 * The context-menu actions for an audio message: a copy action offered
 * only while the transcription is displayed, a speak/stop-speaking
 * action offered while something is speaking or while an own message's
 * transcription is displayed, and always a view-transcription/view-as-
 * audio toggle.
 */
internal fun audioActionsFor(
    row: ChatMessageRowData,
    transcriptionText: String,
    onToggleAudioTranscription: (String) -> Unit,
    onSpeak: (String, String) -> Unit,
    onCopy: () -> Unit,
): List<ContextMenuAction> {
    val actions = mutableListOf<ContextMenuAction>()
    ContextMenuActionHandlerService.reactionDetailsAction(row.message)?.let { actions.add(it) }
    val isDisplayingAudioTranscription = row.isDisplayingAudioTranscription
    val isSpeaking = TextToSpeechService.isSpeaking

    if (isDisplayingAudioTranscription) {
        actions.add(ContextMenuAction(LocalizedStringKey.Copy.localized(), "doc.on.doc") { onCopy() })
    }

    if (isSpeaking || (isDisplayingAudioTranscription && row.message.isFromCurrentUser)) {
        actions.add(
            ContextMenuAction(
                (if (isSpeaking) LocalizedStringKey.StopSpeaking else LocalizedStringKey.Speak).localized(),
                "speaker.wave.2.circle",
            ) { onSpeak(row.message.id, transcriptionText) },
        )
    }

    val title = if (isDisplayingAudioTranscription) LocalizedStringKey.ViewAsAudio else LocalizedStringKey.ViewTranscription
    val systemImageName = if (isDisplayingAudioTranscription) "speaker.wave.2.bubble" else "text.bubble"
    actions.add(ContextMenuAction(title.localized(), systemImageName) { onToggleAudioTranscription(row.message.id) })

    return actions
}

internal fun actionsFor(
    row: ChatMessageRowData,
    displayText: String,
    onToggleAlternate: (String) -> Unit,
    onSpeak: (String, String) -> Unit,
    onCopy: () -> Unit,
): List<ContextMenuAction> {
    val actions = mutableListOf<ContextMenuAction>()
    ContextMenuActionHandlerService.reactionDetailsAction(row.message)?.let { actions.add(it) }
    actions.add(ContextMenuAction(LocalizedStringKey.Copy.localized(), "doc.on.doc") { onCopy() })

    val isSpeaking = TextToSpeechService.isSpeaking
    actions.add(
        ContextMenuAction(
            (if (isSpeaking) LocalizedStringKey.StopSpeaking else LocalizedStringKey.Speak).localized(),
            if (isSpeaking) "speaker.slash.circle" else "speaker.wave.2.circle",
        ) { onSpeak(row.message.id, displayText) },
    )

    // The report and view-alternate actions are hidden while speaking.
    if (isSpeaking) return actions

    ContextMenuActionHandlerService.reportMistranslationAction(row)?.let { actions.add(it) }

    if (shouldShowAlternateAction(row)) {
        val title =
            if (row.message.isFromCurrentUser) {
                if (row.showAlternate) LocalizedStringKey.ViewOriginal else LocalizedStringKey.ViewTranslation
            } else {
                if (row.showAlternate) LocalizedStringKey.ViewTranslation else LocalizedStringKey.ViewOriginal
            }
        actions.add(ContextMenuAction(title.localized(), "globe") { onToggleAlternate(row.message.id) })
    }

    return actions
}

/**
 * Whether the view-original/translation toggle should be offered:
 * available in one-to-one chats or for messages not from the current
 * user (so in group chats only on received messages), and only when the
 * translation is not idempotent (e.g. `en`→`en`), has letters, and its
 * counterpart language differs from the current user's.
 */
private fun shouldShowAlternateAction(row: ChatMessageRowData): Boolean {
    if (row.isGroup && row.message.isFromCurrentUser) return false
    val translation = row.translation ?: return false
    val pair = translation.languagePair
    if (pair.isIdempotent) return false
    if (translation.input.value.none { it.isLetter() }) return false
    val relevantLanguageCode = if (row.message.isFromCurrentUser) pair.to else pair.from
    return relevantLanguageCode != RuntimeStorage.languageCode
}
