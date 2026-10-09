//
//  AttributedStringConfig.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

import androidx.compose.ui.text.SpanStyle

/**
 * A configuration that describes how to style an alert's title or
 * message as an attributed string.
 *
 * Use [AttributedStringConfig] to customize the appearance of an
 * alert's text beyond what the system provides by default. Apply a
 * configuration by calling `setTitleAttributes` or
 * `setMessageAttributes` on the alert before presenting it:
 *
 * ```kotlin
 * val alert = Alert(message = "Operation complete.")
 *
 * alert.setMessageAttributes(
 *     AttributedStringConfig(SpanStyle(fontWeight = FontWeight.Bold)),
 * )
 *
 * alert.present()
 * ```
 *
 * To apply different attributes to specific substrings, provide one
 * or more [StringAttributes] as secondary attributes:
 *
 * ```kotlin
 * val config = AttributedStringConfig(
 *     SpanStyle(fontSize = 15.sp),
 *     secondaryAttributes = listOf(
 *         AttributedStringConfig.StringAttributes(
 *             SpanStyle(color = Color.Red),
 *             stringRanges = listOf("important"),
 *         ),
 *     ),
 * )
 * ```
 */
class AttributedStringConfig(
    /** The text attributes applied to the entire string. */
    val primaryAttributes: SpanStyle,
    /**
     * An optional list of [StringAttributes] applied to specific
     * substrings.
     */
    val secondaryAttributes: List<StringAttributes>? = null,
) {
    // MARK: - Types

    /**
     * A set of attributes to apply to specific substrings within an
     * attributed string.
     */
    class StringAttributes(
        /** The text attributes to apply. */
        val attributes: SpanStyle,
        stringRanges: List<String>,
    ) {
        /**
         * The substrings to apply the attributes to. Empty strings
         * are filtered out, and duplicates are removed.
         */
        val stringRanges: List<String> = stringRanges.filter { it.isNotEmpty() }.distinct()
    }
}
