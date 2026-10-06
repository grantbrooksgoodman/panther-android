//
//  ChatInfoPageViewStrings.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatinfopageview

import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.translator.models.TranslationInput

/** The translated label strings for the chat info page. */
object ChatInfoPageViewStrings : TranslatedLabelStrings {
    val addContactButtonText = TranslatedLabelStringCollection("chatInfoPageView.addContactButtonText")
    val changeMetadataButtonText = TranslatedLabelStringCollection("chatInfoPageView.changeMetadataButtonText")
    val leaveConversation = TranslatedLabelStringCollection("chatInfoPageView.leaveConversation")
    val participantCountLabelText = TranslatedLabelStringCollection("chatInfoPageView.participantCountLabelText")
    val segmentedControlMediaOptionText = TranslatedLabelStringCollection("chatInfoPageView.segmentedControlMediaOptionText")
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
