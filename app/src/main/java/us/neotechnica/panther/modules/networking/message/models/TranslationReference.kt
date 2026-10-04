//
//  TranslationReference.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.message.models

/**
 * A reference to a translation stored in the hosted archive.
 *
 * A reference serializes as its [hostingKey], the composite
 * archive key `"<languagePair> | <referenceKey>"`.
 *
 * **Note:** This carries the raw hosting key so messages round-trip
 * on the wire without the full translation stack.
 */
@JvmInline
value class TranslationReference(
    /**
     * The key used to store and retrieve this translation in the
     * hosted archive.
     */
    val hostingKey: String,
) {
    // MARK: - Companion

    companion object {
        /**
         * Creates a translation reference from its serialized
         * hosting key, or `null` if the key is blank.
         *
         * @param string The serialized hosting key.
         *
         * @return The reference, or `null`.
         */
        fun from(string: String): TranslationReference? = if (string.isBlank()) null else TranslationReference(string)
    }
}
