//
//  Reaction.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.schema.conversation.models

import us.neotechnica.panther.networking.modules.common.extensions.decodingFailure
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.networking.modules.common.interfaces.SerializableDecoder

/**
 * A reaction applied to a message by a user.
 */
data class Reaction(
    /** The reaction's style. */
    val style: Style,
    /** The identifier of the user who applied the reaction. */
    val userID: String,
) : Serializable<Map<String, Any?>> {
    // MARK: - Types

    /**
     * The visual style of a [Reaction].
     *
     * The style serializes as its uppercased name (for example,
     * `LOVE`).
     */
    enum class Style(
        private val lowercaseValue: String,
    ) {
        DISLIKE("dislike"),
        EMPHASIS("emphasis"),
        LAUGH("laugh"),
        LIKE("like"),
        LOVE("love"),
        QUESTION("question"),
        ;

        /** The emoji that represents the style. */
        val emojiValue: String
            get() =
                when (this) {
                    DISLIKE -> "👎"
                    EMPHASIS -> "‼️"
                    LAUGH -> "😂"
                    LIKE -> "👍"
                    LOVE -> "❤️"
                    QUESTION -> "❓"
                }

        /** The serialized representation of the style. */
        val encodedValue: String
            get() = lowercaseValue.uppercase()

        /** The value that determines the style's position in display order. */
        val orderValue: Int
            get() =
                when (this) {
                    LOVE -> 0
                    LIKE -> 1
                    DISLIKE -> 2
                    LAUGH -> 3
                    EMPHASIS -> 4
                    QUESTION -> 5
                }

        companion object {
            /** The reaction styles, sorted by display order. */
            val orderedCases: List<Style> = entries.sortedBy { it.orderValue }

            /**
             * Creates a style from its serialized representation,
             * or `null` if no style matches.
             */
            fun from(encodedValue: String): Style? =
                entries.firstOrNull {
                    it.encodedValue == encodedValue
                }
        }
    }

    // MARK: - Type Aliases

    private enum class Keys(
        val rawValue: String,
    ) {
        STYLE("style"),
        USER_ID("userID"),
    }

    // MARK: - Computed Properties

    /** The serialized representation of the reaction. */
    override val encoded: Map<String, Any?>
        get() =
            mapOf(
                Keys.STYLE.rawValue to style.encodedValue,
                Keys.USER_ID.rawValue to userID,
            )

    // MARK: - Companion

    companion object : SerializableDecoder<Reaction, Map<String, Any?>> {
        override fun canDecode(data: Map<String, Any?>): Boolean {
            val encodedStyle = data[Keys.STYLE.rawValue] as? String ?: return false
            return Style.from(encodedStyle) != null && data[Keys.USER_ID.rawValue] is String
        }

        override suspend fun decode(data: Map<String, Any?>): Reaction {
            val encodedStyle = data[Keys.STYLE.rawValue] as? String
            val style = encodedStyle?.let { Style.from(it) }
            val userID = data[Keys.USER_ID.rawValue] as? String

            if (style == null || userID == null) throw decodingFailure(this, data)

            return Reaction(
                style = style,
                userID = userID,
            )
        }
    }
}
