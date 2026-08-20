//
//  OnboardingService.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.services

import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheetAlert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.ConfirmationAlert
import us.neotechnica.panther.networking.modules.schema.common.models.PhoneNumber
import us.neotechnica.panther.networking.modules.user.services.UserService
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

/**
 * Carries state through the onboarding flow and finalizes account
 * creation, ported from the iOS `OnboardingService`.
 *
 * A singleton, so every onboarding page reads and writes the same
 * values. Sign-up pages record their results as the user progresses;
 * [createUser] creates the account once the required values are
 * present. [flushValues] resets recorded values when the flow
 * restarts.
 *
 * **Note:** push-token registration is deferred to a later phase, so
 * created users carry no push tokens yet; analytics events are
 * likewise deferred.
 */
object OnboardingService {
    // MARK: - Properties

    var authID: String? = null
        private set

    var createdUserInCurrentAppSession = false
        private set

    var languageCode: String? = null
        private set

    var phoneNumber: PhoneNumber? = null
        private set

    var regionCode: String? = null
        private set

    var userID: String? = null
        private set

    // MARK: - Setters

    fun setAuthID(authID: String) {
        this.authID = authID
    }

    fun setLanguageCode(languageCode: String) {
        this.languageCode = languageCode
    }

    fun setPhoneNumber(phoneNumber: PhoneNumber) {
        this.phoneNumber = phoneNumber
    }

    fun setRegionCode(regionCode: String) {
        this.regionCode = regionCode
    }

    fun setUserID(userID: String) {
        this.userID = userID
    }

    // MARK: - Create User

    /**
     * Creates a user record from the recorded onboarding values and
     * persists the new user's identifier.
     *
     * @throws Exception if a required value is missing or creation
     *   fails.
     */
    suspend fun createUser() {
        val languageCode = languageCode
        val phoneNumber = phoneNumber
        val userID = userID
        if (languageCode == null || phoneNumber == null || userID == null) {
            throw Exception("Insufficient data to create user.", metadata = ExceptionMetadata(this))
        }

        val user =
            UserService.createUser(
                id = userID,
                languageCode = languageCode,
                phoneNumber = phoneNumber,
                pushTokens = null,
            )
        Persistent.setString(PersistentStorageKey.currentUserID, user.id)
        createdUserInCurrentAppSession = true
    }

    // MARK: - Alert Presentation

    /** Offers to sign up when no account exists; returns `true` if cancelled. */
    suspend fun presentAccountDoesNotExistAlert(): Boolean =
        !ConfirmationAlert(
            message = "There is no account registered with this phone number. Please sign up instead.",
            confirmButtonTitle = "Sign Up",
            confirmButtonStyle = ActionStyle.PREFERRED,
        ).present()

    /** Offers to sign in when an account exists; returns `true` if cancelled. */
    suspend fun presentAccountExistsAlert(): Boolean =
        !ConfirmationAlert(
            message = "There is already an account registered with this phone number. Please sign in instead.",
            confirmButtonTitle = "Sign In",
            confirmButtonStyle = ActionStyle.PREFERRED,
        ).present()

    /** Asks the user to agree to the conduct policy; returns `true` if declined. */
    suspend fun presentEulaAlert(): Boolean =
        !ActionSheetAlert(
            message =
                "I agree to help maintain a community of respect towards others " +
                    "via my personal conduct on this app.",
            confirmButtonTitle = "I Agree",
            cancelButtonTitle = "I Do Not Agree",
        ).present()

    // MARK: - Auxiliary

    /** Resets every recorded onboarding value; keeps the session flag. */
    fun flushValues() {
        authID = null
        languageCode = null
        phoneNumber = null
        regionCode = null
        userID = null
    }
}
