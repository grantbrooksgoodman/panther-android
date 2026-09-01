//
//  ExceptionMetadataService.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 31/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import us.neotechnica.panther.modules.common.extensions.cannotSendTextMessages
import us.neotechnica.panther.modules.common.extensions.observerRegistrationMisuse
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.ExceptionMetadataDelegate
import us.neotechnica.panther.subsystem.modules.foundation.models.AppException

/**
 * Provides app-specific metadata for exception handling.
 *
 * The subsystem consults this delegate to decide which exceptions
 * are reportable and to translate developer-facing descriptors
 * into user-appropriate messages. Register the service once at
 * launch with
 * `AppSubsystem.delegates.registerExceptionMetadataDelegate(ExceptionMetadataService)`.
 */
object ExceptionMetadataService : ExceptionMetadataDelegate {
    // MARK: - ExceptionMetadataDelegate Conformance

    override fun isReportable(errorCode: String): Boolean =
        errorCode != AppException.cannotSendTextMessages.errorCode &&
            errorCode != AppException.observerRegistrationMisuse.errorCode

    override fun userFacingDescriptor(descriptor: String): String? =
        when (descriptor) {
            "Attempted to select contact pair containing blocked user." ->
                "You have blocked this user."

            "Attempted to select contact pair containing current user." ->
                "Unable to start a conversation with yourself."

            "Failed to resolve random PenPals participant." ->
                "Looks like there's nobody available to connect with right now. Try again later!"

            INVALID_PHONE_NUMBER_DESCRIPTOR ->
                "The format of the phone number is incorrect. Please verify that you haven't included the country code."

            INVALID_VERIFICATION_CODE_DESCRIPTOR ->
                "The verification code is incorrect. Please try again."

            "The SMS code has expired. Please re-send the verification code to try again." ->
                "The verification code has expired. Please try again."

            else -> null
        }
}

private const val INVALID_PHONE_NUMBER_DESCRIPTOR =
    "The format of the phone number provided is incorrect. Please enter the phone " +
        "number in a format that can be parsed into E.164 format. E.164 phone numbers " +
        "are written in the format [+][country code][subscriber number including area code]."

private const val INVALID_VERIFICATION_CODE_DESCRIPTOR =
    "The multifactor verification code used to create the auth credential is invalid. " +
        "Re-collect the verification code and be sure to use the verification code provided by the user."
