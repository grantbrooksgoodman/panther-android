//
//  ExceptionCatalog.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle

import us.neotechnica.panther.modules.common.services.sources.ErrorReportingService
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.ExceptionMetadataDelegate
import us.neotechnica.panther.subsystem.modules.foundation.models.AppException

// MARK: - Types

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

    override fun isReportable(errorCode: String): Boolean {
        if (errorCode == AppException.cannotSendTextMessages.errorCode ||
            errorCode == AppException.observerRegistrationMisuse.errorCode
        ) {
            return false
        }

        return errorCode !in ErrorReportingService.reportedErrorCodes
    }

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

// MARK: - Exceptions

// Catalogs application-specific error codes so that error-handling
// logic can match exceptions by code rather than by descriptor
// string.

/** An exception that indicates the device cannot send text messages. */
val AppException.Companion.cannotSendTextMessages: AppException get() = AppException("56F0")

/** An exception that indicates the user denied access to their contacts. */
val AppException.Companion.contactAccessDenied: AppException get() = AppException("C8DC")

/** An exception that indicates no current user identifier is set. */
val AppException.Companion.currentUserIDNotSet: AppException get() = AppException("EA90")

/** An exception that indicates the device's contact list is empty. */
val AppException.Companion.emptyContactList: AppException get() = AppException("A431")

/** An exception that indicates a media file could not be generated. */
val AppException.Companion.failedToGenerateMediaFile: AppException get() = AppException("D648")

/** An exception that indicates a phone number's hash and calling code do not correspond. */
val AppException.Companion.mismatchedHashAndCallingCode: AppException get() = AppException("D339")

/** An exception that records a reported mistranslation. */
val AppException.Companion.mistranslationReported: AppException get() = AppException("CA45") // NIT: Unused on Android.

/** An exception that indicates the app is not authorized to access contacts. */
val AppException.Companion.notAuthorizedForContacts: AppException get() = AppException("B7FC")

/** An exception that indicates the device is not registered for push notifications. */
val AppException.Companion.notRegisteredForPushNotifications: AppException get() = AppException("FB09")

/** An exception that indicates no user is registered with the given phone number. */
val AppException.Companion.noUsersWithPhoneNumber: AppException get() = AppException("4E4F")

/** An exception that indicates an observer was registered or removed out of order. */
val AppException.Companion.observerRegistrationMisuse: AppException get() = AppException("983A")

/** An exception that indicates database read and write access is disabled. */
val AppException.Companion.readWriteAccessDisabled: AppException get() = AppException("DF6E")

/** An exception that indicates the stored push token is out of date. */
val AppException.Companion.stalePushToken: AppException get() = AppException("28D1")

/** An exception representing a timed-out operation. */
val AppException.Companion.timedOut: AppException get() = AppException("801F")

private const val INVALID_PHONE_NUMBER_DESCRIPTOR =
    "The format of the phone number provided is incorrect. Please enter the phone " +
        "number in a format that can be parsed into E.164 format. E.164 phone numbers " +
        "are written in the format [+][country code][subscriber number including area code]."

private const val INVALID_VERIFICATION_CODE_DESCRIPTOR =
    "The multifactor verification code used to create the auth credential is invalid. " +
        "Re-collect the verification code and be sure to use the verification code provided by the user."
