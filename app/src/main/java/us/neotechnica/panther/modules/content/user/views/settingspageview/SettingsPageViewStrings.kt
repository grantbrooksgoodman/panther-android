//
//  SettingsPageViewStrings.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.settingspageview

import us.neotechnica.panther.modules.content.user.constants.SettingsPageViewConstants
import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.translator.models.TranslationInput

// MARK: - Constants Accessors

private typealias Strings = SettingsPageViewConstants

/** The translated label strings for the settings page. */
object SettingsPageViewStrings : TranslatedLabelStrings {
    /** The text of the blocked users button. */
    val blockedUsersButtonText = TranslatedLabelStringCollection("settingsPageView.blockedUsersButtonText")

    /** The change language row's text. */
    val changeLanguage = TranslatedLabelStringCollection("settingsPageView.changeLanguage")

    /** The text of the clear caches button. */
    val clearCachesButtonText = TranslatedLabelStringCollection("settingsPageView.clearCachesButtonText")

    /** The text of the delete account button. */
    val deleteAccountButtonText = TranslatedLabelStringCollection("settingsPageView.deleteAccountButtonText")

    /** The text of the invite friends button. */
    val inviteFriendsButtonText = TranslatedLabelStringCollection("settingsPageView.inviteFriendsButtonText")

    /** The text of the leave review button. */
    val leaveReviewButtonText = TranslatedLabelStringCollection("settingsPageView.leaveReviewButtonText")

    /** The text of the sign out button. */
    val signOutButtonText = TranslatedLabelStringCollection("settingsPageView.signOutButtonText")

    override val keyPairs: List<TranslationInputMap> =
        listOf(
            TranslationInputMap(blockedUsersButtonText, TranslationInput(Strings.BLOCKED_USERS_BUTTON_TEXT)),
            TranslationInputMap(changeLanguage, TranslationInput(Strings.CHANGE_LANGUAGE)),
            TranslationInputMap(clearCachesButtonText, TranslationInput(Strings.CLEAR_CACHES_BUTTON_TEXT)),
            TranslationInputMap(deleteAccountButtonText, TranslationInput(Strings.DELETE_ACCOUNT_BUTTON_TEXT)),
            TranslationInputMap(inviteFriendsButtonText, TranslationInput(Strings.INVITE_FRIENDS_BUTTON_TEXT)),
            TranslationInputMap(
                leaveReviewButtonText,
                TranslationInput(Strings.LEAVE_REVIEW_BUTTON_TEXT, alternate = Strings.RATE_THE_APP),
            ),
            TranslationInputMap(
                signOutButtonText,
                TranslationInput(Strings.SIGN_OUT_BUTTON_TEXT, alternate = Strings.LOG_OUT),
            ),
        )
}
