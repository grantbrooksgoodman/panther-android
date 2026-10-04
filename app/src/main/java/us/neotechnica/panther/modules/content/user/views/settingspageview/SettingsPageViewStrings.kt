//
//  SettingsPageViewStrings.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
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
    val blockedUsersButtonText = TranslatedLabelStringCollection("settingsPageView.blockedUsersButtonText")
    val changeLanguage = TranslatedLabelStringCollection("settingsPageView.changeLanguage")
    val clearCachesButtonText = TranslatedLabelStringCollection("settingsPageView.clearCachesButtonText")
    val deleteAccountButtonText = TranslatedLabelStringCollection("settingsPageView.deleteAccountButtonText")
    val inviteFriendsButtonText = TranslatedLabelStringCollection("settingsPageView.inviteFriendsButtonText")
    val leaveReviewButtonText = TranslatedLabelStringCollection("settingsPageView.leaveReviewButtonText")
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
