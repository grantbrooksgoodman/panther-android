//
//  TranslationLoggerDelegate.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.interfaces

/**
 * A protocol you adopt to receive diagnostic log messages from the
 * translation pipeline.
 *
 * Register an implementation through
 * [Translator.config][us.neotechnica.panther.translator.Translator.config]
 * to route the translator's internal logging into the host app's
 * logging system.
 */
interface TranslationLoggerDelegate {
    /**
     * Logs a diagnostic message.
     *
     * @param text The message to log.
     * @param sender The object that produced the message.
     * @param fileName The source file the message originated from.
     * @param function The function the message originated from.
     * @param line The line the message originated from.
     */
    fun log(
        text: String,
        sender: Any,
        fileName: String,
        function: String,
        line: Int,
    )
}
