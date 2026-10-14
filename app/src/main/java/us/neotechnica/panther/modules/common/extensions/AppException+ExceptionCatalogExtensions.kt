//
//  AppException+ExceptionCatalogExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.extensions

import us.neotechnica.panther.subsystem.modules.foundation.models.AppException

// Catalogs application-specific error codes so that error-handling
// logic can match exceptions by code rather than by descriptor
// string.

/** The exceptions that indicate an audio recording failure. */
val AppException.Companion.audioRecordingFailures: List<AppException> // NIT: Unused on Android.
    get() =
        listOf(
            failedToInitializeRecognizer,
            kAFAssistantError,
            noSpeechDetected,
        )

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

/** An exception that indicates the speech recognizer could not be initialized. */
val AppException.Companion.failedToInitializeRecognizer: AppException get() = AppException("9E79")

/** An exception that indicates the speech recognition service failed. */
val AppException.Companion.kAFAssistantError: AppException get() = AppException("F59D")

/** An exception that indicates a phone number's hash and calling code do not correspond. */
val AppException.Companion.mismatchedHashAndCallingCode: AppException get() = AppException("D339")

/** An exception that records a reported mistranslation. */
val AppException.Companion.mistranslationReported: AppException get() = AppException("CA45") // NIT: Unused on Android.

/** An exception that indicates no audio recording is in progress. */
val AppException.Companion.noAudioRecorderToStop: AppException get() = AppException("E44E") // NIT: Unused on Android.

/** An exception that indicates the app is not authorized to access contacts. */
val AppException.Companion.notAuthorizedForContacts: AppException get() = AppException("B7FC")

/** An exception that indicates the device is not registered for push notifications. */
val AppException.Companion.notRegisteredForPushNotifications: AppException get() = AppException("FB09")

/** An exception that indicates no speech was detected in a recording. */
val AppException.Companion.noSpeechDetected: AppException get() = AppException("24F2")

/** An exception that indicates no user is registered with the given phone number. */
val AppException.Companion.noUsersWithPhoneNumber: AppException get() = AppException("4E4F")

/** An exception that indicates an observer was registered or removed out of order. */
val AppException.Companion.observerRegistrationMisuse: AppException get() = AppException("983A")

/** An exception that indicates a random participant could not be resolved. */
val AppException.Companion.penPalResolutionFailed: AppException get() = AppException("AD6B") // NIT: Unused on Android.

/** An exception that indicates database read and write access is disabled. */
val AppException.Companion.readWriteAccessDisabled: AppException get() = AppException("DF6E")

/** An exception that indicates the stored push token is out of date. */
val AppException.Companion.stalePushToken: AppException get() = AppException("28D1")

/** An exception representing a timed-out operation. */
val AppException.Companion.timedOut: AppException get() = AppException("801F")

/** An exception that indicates the translation platform is not supported. */
val AppException.Companion.translationPlatformNotSupported: AppException get() = AppException("B04E") // NIT: Unused on Android.

/** An exception that indicates the app must be updated before continuing. */
val AppException.Companion.updateRequired: AppException get() = AppException("B455") // NIT: Unused on Android.
