//
//  Effect+OnboardingExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.extensions

import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.networking.modules.auth.interfaces.AuthDelegate
import us.neotechnica.panther.networking.modules.common.dependencies.networking
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.translator.Translator

/**
 * Returns an effect that sends a verification code to the given phone
 * number.
 *
 * Phone verification needs the foreground activity to host reCAPTCHA
 * or app-check challenges; without one, the effect fails without a
 * report.
 *
 * @param phoneNumber The phone number to verify.
 * @param sender The object requesting the verification, to which a
 *   failure is attributed.
 * @param returned Maps the verification result – the identifier
 *   issued for the authentication attempt, or the signed-in user –
 *   to the action sent on success.
 * @param failed Maps the exception to the action sent on failure.
 *
 * @return The verification effect.
 */
fun <Action> Effect.Companion.verifyPhoneNumber(
    phoneNumber: PhoneNumber,
    sender: Any,
    returned: (AuthDelegate.PhoneVerificationResult) -> Action,
    failed: (Exception) -> Action,
): Effect<Action> =
    task {
        val activity =
            Translator.config.currentActivityProvider?.invoke()
                ?: return@task failed(
                    Exception(
                        "No current activity for phone verification.",
                        isReportable = false,
                        metadata = ExceptionMetadata(sender),
                    ),
                )

        try {
            returned(
                DependencyValues.current.networking.auth.verifyPhoneNumber(
                    activity = activity,
                    internationalNumber = phoneNumber.compiledNumberString,
                    languageCode = RuntimeStorage.languageCode,
                ),
            )
        } catch (exception: Exception) {
            failed(exception)
        }
    }
