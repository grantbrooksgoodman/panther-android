//
//  HostedContentType.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.schema.message.models

/**
 * The kind of content a message carries.
 *
 * **Note:** This Phase 2 port models [Text] as a first-class
 * case and preserves audio and media content types as opaque
 * [Other] wire strings so text messages round-trip fully. The
 * media (Phase 7) and audio phases will replace [Other] with
 * first-class cases carrying file identifiers and extensions.
 */
sealed interface HostedContentType {
    // MARK: - Properties

    /**
     * The content type's complete wire-format string.
     *
     * For text this is `"text"`; for media it is
     * `"<mime> – <id> – <ext>"` joined by `" – "` (a space, an
     * en dash, and a space).
     */
    val hostedValue: String

    /**
     * The content type's raw string. For text this is `"text"`;
     * for other content it is the MIME type – the first `" – "`
     * component of [hostedValue].
     */
    val rawValue: String

    // MARK: - Cases

    /** Text content. */
    data object Text : HostedContentType {
        override val hostedValue: String get() = "text"
        override val rawValue: String get() = "text"
    }

    /** Audio or media content, preserved as its opaque wire string. */
    data class Other(
        override val hostedValue: String,
    ) : HostedContentType {
        override val rawValue: String get() = hostedValue.split(" – ").first()
    }

    // MARK: - Companion

    companion object {
        /**
         * Creates a content type from its wire-format string.
         *
         * @param hostedValue The wire-format content type string.
         *
         * @return The content type, or `null` if the string is
         *   blank.
         */
        fun from(hostedValue: String): HostedContentType? =
            when {
                hostedValue == Text.hostedValue -> Text
                hostedValue.isBlank() -> null
                else -> Other(hostedValue)
            }
    }
}
