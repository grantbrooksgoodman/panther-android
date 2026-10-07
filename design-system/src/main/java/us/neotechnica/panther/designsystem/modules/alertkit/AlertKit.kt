//
//  AlertKit.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit

/**
 * A namespace for AlertKit's shared constants and errors.
 */
object AlertKit {
    // MARK: - Types

    /** Constants used by the AlertKit alert types. */
    object Constants {
        /** The default title for a standard alert action (`"OK"`). */
        const val DEFAULT_ACTION_TITLE = "OK"

        /** The default title for a cancel button (`"Cancel"`). */
        const val DEFAULT_CANCEL_BUTTON_TITLE = "Cancel"

        /** The default title for a confirm button (`"Confirm"`). */
        const val DEFAULT_CONFIRM_BUTTON_TITLE = "Confirm"

        /** The default title for a dismiss button (`"Dismiss"`). */
        const val DEFAULT_DISMISS_BUTTON_TITLE = "Dismiss"

        /**
         * The default title for the send error report button
         * (`"Send Error Report"`).
         */
        const val DEFAULT_SEND_ERROR_REPORT_BUTTON_TITLE = "Send Error Report"
    }

    /** Errors that AlertKit methods can produce. */
    sealed class Error(
        message: String,
        cause: Throwable? = null,
    ) : kotlin.Exception(message, cause) {
        /**
         * A translation operation failed.
         *
         * [errorDescription] contains a description of the
         * underlying failure.
         */
        class TranslationFailed(
            val errorDescription: String,
            cause: Throwable? = null,
        ) : Error("Translation failed: $errorDescription", cause)
    }
}
