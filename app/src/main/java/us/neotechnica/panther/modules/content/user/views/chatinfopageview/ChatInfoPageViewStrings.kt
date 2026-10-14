//
//  ChatInfoPageViewStrings.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatinfopageview

import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.translator.models.TranslationInput

/** The translated label strings for the chat info page. */
object ChatInfoPageViewStrings : TranslatedLabelStrings {
    /** The text of the add contact button. */
    val addContactButtonText = TranslatedLabelStringCollection("chatInfoPageView.addContactButtonText")

    /** The text of the change metadata button. */
    val changeMetadataButtonText = TranslatedLabelStringCollection("chatInfoPageView.changeMetadataButtonText")

    /** The leave conversation row's text. */
    val leaveConversation = TranslatedLabelStringCollection("chatInfoPageView.leaveConversation")

    /** The text of the participant count label. */
    val participantCountLabelText = TranslatedLabelStringCollection("chatInfoPageView.participantCountLabelText")

    /** The text of the segmented control's media option. */
    val segmentedControlMediaOptionText = TranslatedLabelStringCollection("chatInfoPageView.segmentedControlMediaOptionText")

    /** The text of the segmented control's participants option. */
    val segmentedControlParticipantsOptionText =
        TranslatedLabelStringCollection("chatInfoPageView.segmentedControlParticipantsOptionText")

    override val keyPairs: List<TranslationInputMap> =
        listOf(
            TranslationInputMap(addContactButtonText, TranslationInput("Add Contact")),
            TranslationInputMap(changeMetadataButtonText, TranslationInput("Change name and photo")),
            TranslationInputMap(leaveConversation, TranslationInput("Leave this Conversation")),
            TranslationInputMap(participantCountLabelText, TranslationInput("people", alternate = "persons")),
            TranslationInputMap(
                segmentedControlMediaOptionText,
                TranslationInput("Attachments", alternate = "Shared Media"),
            ),
            TranslationInputMap(segmentedControlParticipantsOptionText, TranslationInput("Participants")),
        )
}
