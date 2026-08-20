//
//  TranslationReference.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.models

import us.neotechnica.panther.networking.modules.translation.extensions.TRANSLATION_COMPONENT_SEPARATOR
import us.neotechnica.panther.networking.modules.translation.extensions.alphaEncoded
import us.neotechnica.panther.networking.modules.translation.extensions.base64Encoded
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHashOf
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation

/**
 * A compact, storable reference to a hosted translation.
 *
 * References are [Type.Archived] for translations between different
 * languages – a hash-keyed reference to an archived value – or
 * [Type.Idempotent] for same-language translations, which carry the
 * Base64-encoded input inline and are never written to the archive.
 *
 * The wire format is pinned by `translation_reference_vectors.json`.
 */
data class TranslationReference(
    /** The language pair for this reference. */
    val languagePair: LanguagePair,
    /** The kind of reference. */
    val type: Type,
) {
    // MARK: - Type

    /** The kind of translation reference. */
    sealed interface Type {
        /** The identifying key: the hash, or the Base64 value. */
        val key: String

        /** The inline encoded value, present only for [Archived]. */
        val value: String?

        /** A hash-keyed reference to an archived translation, with an optional inline value. */
        data class Archived(
            val hash: String,
            override val value: String? = null,
        ) : Type {
            override val key: String get() = hash
        }

        /** A Base64-encoded same-language reference. */
        data class Idempotent(
            val encodedValue: String,
        ) : Type {
            override val key: String get() = encodedValue
            override val value: String? get() = null
        }
    }

    // MARK: - Computed Properties

    /** The key used to store and retrieve this reference in the hosted archive. */
    val hostingKey: String
        get() {
            val pairComponent =
                if (languagePair.isIdempotent) {
                    "${TranslationConstants.IDEMPOTENT_PREFIX}${languagePair.from}"
                } else {
                    languagePair.string
                }
            return "$pairComponent | ${type.key}"
        }

    // MARK: - Companion

    companion object {
        /** Creates a reference from a completed [translation]. */
        fun from(translation: Translation): TranslationReference {
            val input = translation.input.value

            return if (translation.languagePair.isIdempotent) {
                TranslationReference(
                    languagePair = translation.languagePair,
                    type = Type.Idempotent(input.base64Encoded),
                )
            } else {
                val value = "${input.alphaEncoded}$TRANSLATION_COMPONENT_SEPARATOR${translation.output.alphaEncoded}"
                TranslationReference(
                    languagePair = translation.languagePair,
                    type = Type.Archived(hash = encodedHashOf(listOf(input)), value = value),
                )
            }
        }

        /**
         * Parses a reference from its string form, or returns `null`
         * if the string is not a valid reference.
         */
        fun fromString(string: String): TranslationReference? {
            val isIdempotent = string.contains(TranslationConstants.IDEMPOTENT_PREFIX)
            val components = string.split(" ")

            val expectedCount = if (isIdempotent) IDEMPOTENT_COMPONENT_COUNT else ARCHIVED_COMPONENT_COUNT
            if (components.size != expectedCount) return null

            val pairIndex = if (isIdempotent) 1 else 0
            val languagePair = LanguagePair.fromString(components[pairIndex]) ?: return null
            val reference = components.lastOrNull() ?: return null

            return TranslationReference(
                languagePair = languagePair,
                type = if (isIdempotent) Type.Idempotent(reference) else Type.Archived(reference),
            )
        }

        private const val ARCHIVED_COMPONENT_COUNT = 3
        private const val IDEMPOTENT_COMPONENT_COUNT = 4
    }
}
