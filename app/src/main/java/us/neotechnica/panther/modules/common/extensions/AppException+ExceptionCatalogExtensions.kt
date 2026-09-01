//
//  AppException+ExceptionCatalogExtensions.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 31/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.extensions

import us.neotechnica.panther.subsystem.modules.foundation.models.AppException

// Catalogs application-specific error codes so that error-handling
// logic can match exceptions by code rather than by descriptor
// string. Codes mirror the iOS ExceptionCatalog verbatim.

val AppException.Companion.audioRecordingFailures: List<AppException>
    get() =
        listOf(
            failedToInitializeRecognizer,
            kAFAssistantError,
            noSpeechDetected,
        )

val AppException.Companion.cannotSendTextMessages: AppException get() = AppException("56F0")
val AppException.Companion.contactAccessDenied: AppException get() = AppException("C8DC")
val AppException.Companion.currentUserIDNotSet: AppException get() = AppException("EA90")
val AppException.Companion.emptyContactList: AppException get() = AppException("A431")
val AppException.Companion.exhaustedAvailablePlatforms: AppException get() = AppException("C526")
val AppException.Companion.failedToGenerateMediaFile: AppException get() = AppException("D648")
val AppException.Companion.failedToInitializeRecognizer: AppException get() = AppException("9E79")
val AppException.Companion.kAFAssistantError: AppException get() = AppException("F59D")
val AppException.Companion.mismatchedHashAndCallingCode: AppException get() = AppException("D339")
val AppException.Companion.mistranslationReported: AppException get() = AppException("CA45")
val AppException.Companion.noAudioRecorderToStop: AppException get() = AppException("E44E")
val AppException.Companion.notAuthorizedForContacts: AppException get() = AppException("B7FC")
val AppException.Companion.notRegisteredForPushNotifications: AppException get() = AppException("FB09")
val AppException.Companion.noSpeechDetected: AppException get() = AppException("24F2")
val AppException.Companion.noUsersWithPhoneNumber: AppException get() = AppException("4E4F")
val AppException.Companion.observerRegistrationMisuse: AppException get() = AppException("983A")
val AppException.Companion.penPalResolutionFailed: AppException get() = AppException("AD6B")
val AppException.Companion.readWriteAccessDisabled: AppException get() = AppException("DF6E")
val AppException.Companion.sameTranslationInputOutput: AppException get() = AppException("6CEB")
val AppException.Companion.stalePushToken: AppException get() = AppException("28D1")

/** An exception representing a timed-out operation. */
val AppException.Companion.timedOut: AppException get() = AppException("801F")

val AppException.Companion.translationDerivationFailed: AppException get() = AppException("43B4")
val AppException.Companion.translationPlatformNotSupported: AppException get() = AppException("B04E")
val AppException.Companion.updateRequired: AppException get() = AppException("B455")
