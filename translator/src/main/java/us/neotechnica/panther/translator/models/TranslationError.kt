//
//  TranslationError.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.models

/**
 * An error thrown by the translation pipeline.
 *
 * Each case carries a human-readable [message], and the exception is
 * thrown by the translators and
 * [TranslationService][us.neotechnica.panther.translator.services.TranslationService]
 * when a translation cannot be produced.
 */
sealed class TranslationError(
    override val message: String,
) : Exception(message) {
    /** JavaScript evaluation in the web-view harness failed. */
    class EvaluateJavaScriptFailed(
        description: String? = null,
    ) : TranslationError(
            "Failed to evaluate JavaScript: ${description ?: "An unknown error occurred."}",
        )

    /** A request URL could not be generated for the platform. */
    data object FailedToGenerateRequestURL : TranslationError("Failed to generate request URL.")

    /** The input or language pair failed validation. */
    data object InvalidArguments : TranslationError("The arguments are invalid.")

    /** A JavaScript error occurred during extraction. */
    class JavaScriptError(
        description: String,
    ) : TranslationError("JavaScript error occurred: $description")

    /** The extracted translation result was malformed. */
    data object MalformedTranslationResult : TranslationError("Malformed translation result.")

    /** The translation operation timed out. */
    data object TimedOut : TranslationError("The operation timed out.")

    /** The web view failed to navigate to the translation page. */
    class WebViewNavigationFailed(
        description: String,
    ) : TranslationError("Web view navigation failed: $description")

    /** An unspecified error occurred. */
    class Unknown(
        description: String? = null,
    ) : TranslationError(description ?: "An unknown error occurred.")
}
