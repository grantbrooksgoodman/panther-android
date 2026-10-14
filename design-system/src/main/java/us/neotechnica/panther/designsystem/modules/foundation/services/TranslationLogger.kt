//
//  TranslationLogger.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.services

import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.translator.interfaces.TranslationLoggerDelegate

/**
 * The translator's logger delegate, which routes the translation
 * pipeline's diagnostic messages into [Logger].
 *
 * Register the delegate once during app setup:
 *
 * ```kotlin
 * Translator.config.registerLoggerDelegate(TranslationLogger)
 * ```
 *
 * Each message is logged to the translation domain and attributed to
 * the source location the translator reports.
 */
object TranslationLogger : TranslationLoggerDelegate {
    // MARK: - TranslationLoggerDelegate Conformance

    override fun log(
        text: String,
        sender: Any,
        fileName: String,
        function: String,
        line: Int,
    ) {
        Logger.log(
            text,
            domain = LoggerDomain.translation,
            metadata = ExceptionMetadata(sender, fileName, function, line),
        )
    }
}
