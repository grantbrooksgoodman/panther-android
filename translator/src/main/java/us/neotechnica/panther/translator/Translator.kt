//
//  Translator.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator

import android.app.Activity
import us.neotechnica.panther.translator.interfaces.TranslationArchiverDelegate
import us.neotechnica.panther.translator.interfaces.TranslationLoggerDelegate

/**
 * The translator library's namespace and configuration entry point.
 *
 * Register the archiver and logger delegates through [config] during
 * app setup:
 *
 * ```kotlin
 * Translator.config.registerArchiverDelegate(MyArchiver())
 * Translator.config.registerLoggerDelegate(MyLogger())
 * ```
 */
object Translator {
    // MARK: - Properties

    /** The shared library configuration. */
    val config = Config()

    // MARK: - Config

    /**
     * The registration point for the translator's optional
     * archiver and logger delegates.
     */
    class Config internal constructor() {
        // MARK: - Properties

        /** The registered archiver delegate, or `null`. */
        @Volatile
        var archiverDelegate: TranslationArchiverDelegate? = null
            private set

        /** The registered logger delegate, or `null`. */
        @Volatile
        var loggerDelegate: TranslationLoggerDelegate? = null
            private set

        /**
         * A provider of the current foreground [Activity].
         *
         * The web-view harness attaches an off-screen web view to the
         * current activity's window, since a window-less web view does
         * not reliably run timers or observers. When this is `null` or
         * returns `null`, the harness is unavailable and only the HTTP
         * API fast paths (Google, Reverso) can produce translations.
         */
        @Volatile
        var currentActivityProvider: (() -> Activity?)? = null
            private set

        // MARK: - Registration

        /** Registers the archiver delegate the service caches through. */
        fun registerArchiverDelegate(archiverDelegate: TranslationArchiverDelegate) {
            this.archiverDelegate = archiverDelegate
        }

        /** Registers the logger delegate the pipeline logs through. */
        fun registerLoggerDelegate(loggerDelegate: TranslationLoggerDelegate) {
            this.loggerDelegate = loggerDelegate
        }

        /** Registers the provider of the current foreground activity. */
        fun registerCurrentActivityProvider(provider: () -> Activity?) {
            this.currentActivityProvider = provider
        }
    }

    // MARK: - Constants

    internal object Constants {
        const val GOOGLE_CONSENT_JAVA_SCRIPT_STRING =
            "document.getElementsByClassName('VfPpkd-RLmnJb')[3].click();"
        const val GOOGLE_CONSENT_URL_STRING = "https://consent.google.com/"
        const val PROCESSING_DELIMITER = "⌘"
        const val PROCESSING_TOKEN = "⁂"
        const val RESULT_OBSERVER_MESSAGE_HANDLER_NAME = "translatorResultObserver"

        const val DEEP_L_JAVA_SCRIPT_STRING =
            "var result = document.querySelectorAll('[aria-labelledby=\"translation-results-heading\"]'); " +
                "result[result.length - 1].innerText;"
        const val DEEP_L_ALTERNATE_JAVA_SCRIPT_STRING =
            "var result = document.querySelectorAll('[aria-labelledby=\"translation-target-heading\"]'); " +
                "result[result.length - 1].innerText;"
        const val GOOGLE_JAVA_SCRIPT_STRING = "document.getElementsByClassName('lRu31')[0].innerText;"
        const val GOOGLE_ALTERNATE_JAVA_SCRIPT_STRING = "document.getElementsByClassName('lRu31')[1].innerText;"
        const val LARA_JAVA_SCRIPT_STRING = "document.getElementsByClassName('knownFragmentElementNode')[0].innerText;"
        const val REVERSO_JAVA_SCRIPT_STRING =
            "document.getElementsByClassName(" +
                "'textarea translation-box__translated-text translation-box__translated-text_favorite'" +
                ")[0].innerText;"
        const val REVERSO_ALTERNATE_JAVA_SCRIPT_STRING =
            "document.getElementsByClassName('translation-input__main translation-input__result')[0].innerText;"
    }

    // MARK: - Methods

    /** A concise description of a throwable for logging. */
    internal fun descriptor(error: Throwable): String = error.message ?: error.toString()
}
