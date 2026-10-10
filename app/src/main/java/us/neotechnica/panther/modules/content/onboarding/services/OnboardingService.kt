//
//  OnboardingService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.services

import us.neotechnica.panther.bundle.currentUserID
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheet
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.designsystem.modules.foundation.extensions.cancelAction
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.common.services.AnalyticsService
import us.neotechnica.panther.modules.common.services.PushTokenService
import us.neotechnica.panther.modules.networking.user.services.UserService
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

/**
 * An object that carries state through the onboarding flow and
 * finalizes account creation.
 *
 * Use `OnboardingService` to share values gathered across the
 * onboarding pages – the user's language, phone number, region, and
 * authentication identifiers – without threading them through each
 * page's state. Resolve the service through the
 * [onboardingService][us.neotechnica.panther.modules.content.onboarding.dependencies.onboardingService]
 * dependency; the dependency system caches the resolved instance, so
 * every page in the flow reads and writes the same values.
 *
 * The service supports both onboarding paths:
 *
 * - For sign-up, each page records its result with the corresponding
 *   setter as the user progresses. When all required values are
 *   present, [createUser] creates the account.
 * - For sign-in, the recorded phone number and region let a page
 *   restore the user's prior input when they navigate back through the
 *   flow.
 *
 * The service also presents the flow's confirmation alerts, such as
 * when the given phone number is already registered, and reports the
 * user's selection to the caller.
 *
 * **Important:** Recorded values persist for the lifetime of the
 * resolved instance. Call [flushValues] when the user restarts the flow
 * so a previous attempt's values do not leak into the next one.
 *
 * **Warning:** `OnboardingService` does not synchronize access to its
 * stored properties. Access the service from a single concurrency
 * context, such as the reducers that drive the onboarding pages.
 */
class OnboardingService {
    // MARK: - Properties

    /**
     * The identifier issued when a verification code was sent to the
     * user's phone number, or `null` if verification has not begun.
     */
    var authID: String? = null
        private set

    /**
     * A Boolean value that indicates whether [createUser] completed
     * successfully during the current app session.
     *
     * **Note:** [flushValues] does not reset this value.
     */
    var createdUserInCurrentAppSession = false
        private set

    /** The language code the user selected during onboarding, or `null` if one has not been recorded. */
    var languageCode: String? = null
        private set

    /** The phone number the user entered during onboarding, or `null` if one has not been recorded. */
    var phoneNumber: PhoneNumber? = null
        private set

    /** The region code associated with the user's phone number, or `null` if one has not been recorded. */
    var regionCode: String? = null
        private set

    /** The authenticated user's identifier, or `null` if authentication has not completed. */
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
                pushTokens = PushTokenService.currentToken?.let { listOf(it) },
            )
        Persistent.setString(PersistentStorageKey.currentUserID, user.id)
        createdUserInCurrentAppSession = true
        AnalyticsService.logEvent(AnalyticsService.AnalyticsEvent.SIGN_UP)
    }

    // MARK: - Alert Presentation

    /** Offers to sign up when no account exists; returns `true` if cancelled. */
    suspend fun presentAccountDoesNotExistAlert(): Boolean {
        var cancelled = true
        val signUpAction =
            Action(
                "Sign Up",
                style = ActionStyle.PREFERRED,
            ) { cancelled = false }

        Alert(
            message = "There is no account registered with this phone number. Please sign up instead.",
            actions =
                listOf(
                    signUpAction,
                    Action.cancelAction,
                ),
        ).present(
            translating =
                listOf(
                    Alert.TranslationOptionKey.Actions(listOf(signUpAction)),
                    Alert.TranslationOptionKey.Message,
                ),
        )

        return cancelled
    }

    /** Offers to sign in when an account exists; returns `true` if cancelled. */
    suspend fun presentAccountExistsAlert(): Boolean {
        var cancelled = true
        val signInAction =
            Action(
                "Sign In",
                style = ActionStyle.PREFERRED,
            ) { cancelled = false }

        Alert(
            message = "There is already an account registered with this phone number. Please sign in instead.",
            actions =
                listOf(
                    signInAction,
                    Action.cancelAction,
                ),
        ).present(
            translating =
                listOf(
                    Alert.TranslationOptionKey.Actions(listOf(signInAction)),
                    Alert.TranslationOptionKey.Message,
                ),
        )

        return cancelled
    }

    /** Asks the user to agree to the conduct policy; returns `true` if declined. */
    suspend fun presentEULAAlert(): Boolean {
        var cancelled = true
        val agreeAction =
            Action(
                "I Agree",
                style = ActionStyle.PREFERRED,
            ) { cancelled = false }

        ActionSheet(
            message =
                "I agree to help maintain a community of respect towards others " +
                    "via my personal conduct on this app.",
            actions = listOf(agreeAction),
            cancelButtonTitle = "I Do Not Agree",
        ).present()

        return cancelled
    }

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
