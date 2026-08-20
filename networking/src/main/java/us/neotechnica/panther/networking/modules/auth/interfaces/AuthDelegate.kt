//
//  AuthDelegate.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.auth.interfaces

import android.app.Activity

/**
 * An interface for managing user authentication.
 *
 * [AuthDelegate] supports two authentication modes:
 *
 * - **Anonymous sign-in.** Call [signInAnonymously] at launch to
 *   establish a lightweight session that satisfies backend
 *   security rules before the user completes phone verification.
 * - **Phone verification.** Request a verification code with
 *   [verifyPhoneNumber], then complete sign-in by passing the
 *   returned verification ID and the user-entered code to
 *   [authenticateUser].
 *
 * A default implementation backed by Firebase Authentication is
 * provided automatically.
 */
interface AuthDelegate {
    // MARK: - Methods

    /**
     * Signs in the user with a phone authentication credential.
     *
     * When the current session is anonymous, the phone credential
     * is linked to that session, preserving the existing user
     * identifier. If the phone number already belongs to another
     * account, the method falls back to a standard sign-in and
     * returns the existing account's identifier.
     *
     * @param authID The verification ID returned by a prior call
     *   to [verifyPhoneNumber].
     * @param verificationCode The one-time code the user received
     *   via SMS.
     *
     * @return The user's identifier.
     *
     * @throws us.neotechnica.panther.subsystem.modules.foundation.models.Exception
     *   if sign-in fails.
     */
    suspend fun authenticateUser(
        authID: String,
        verificationCode: String,
    ): String

    /**
     * Establishes an anonymous authentication session, or returns
     * the identifier of the existing session if one is persisted.
     *
     * @return The user's identifier.
     *
     * @throws us.neotechnica.panther.subsystem.modules.foundation.models.Exception
     *   if sign-in fails.
     */
    suspend fun signInAnonymously(): String

    /**
     * Ends the current authentication session and clears the
     * persisted session.
     *
     * @throws us.neotechnica.panther.subsystem.modules.foundation.models.Exception
     *   if sign-out fails.
     */
    fun signOut()

    /**
     * Sends a verification code to the specified phone number via
     * SMS and returns the verification ID.
     *
     * **Note:** Unlike the iOS interface, Android phone
     * verification requires an [Activity] to host the Play
     * Integrity or reCAPTCHA verification flow.
     *
     * @param activity The activity hosting the verification flow.
     * @param internationalNumber The phone number to verify, in
     *   international format (for example, `"15551234567"`).
     * @param languageCode A language code used to localize the
     *   verification SMS.
     *
     * @return The phone-number verification ID.
     *
     * @throws us.neotechnica.panther.subsystem.modules.foundation.models.Exception
     *   if verification fails.
     */
    suspend fun verifyPhoneNumber(
        activity: Activity,
        internationalNumber: String,
        languageCode: String,
    ): String
}
