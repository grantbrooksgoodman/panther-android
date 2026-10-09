//
//  Reaction.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.conversation.models

import androidx.compose.ui.graphics.Color
import us.neotechnica.panther.modules.content.user.extensions.currentUserID
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.networking.modules.common.interfaces.SerializableDecoder
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

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

        /** The background color of the style's square icon. */
        val squareIconBackgroundColor: Color
            get() =
                when (this) {
                    DISLIKE -> Color(0xFFFF5252)
                    EMPHASIS -> Color(0xFF0FB9B1)
                    LAUGH -> Color(0xFFC56CF0)
                    LIKE -> Color(0xFF27AE60)
                    LOVE -> Color(0xFF30AAF2)
                    QUESTION -> Color(0xFFFFB142)
                }

        companion object {
            /** The reaction styles, sorted by display order. */
            val orderedCases: List<Style> = entries.sortedBy { it.orderValue }

            private val emojiCaseMap: Map<String, Style> = entries.associateBy { it.emojiValue }

            /**
             * Creates a style from its serialized representation.
             *
             * @param encodedValue The serialized representation of the
             *   style.
             *
             * @return The matching style, or `null` if the value does
             *   not represent a known style.
             */
            fun from(encodedValue: String): Style? =
                entries.firstOrNull {
                    it.encodedValue == encodedValue
                }

            /**
             * Creates a style from the given emoji.
             *
             * @param emojiValue The emoji that represents the style.
             *
             * @return The matching style, or `null` if no style uses
             *   the emoji.
             */
            fun fromEmojiValue(emojiValue: String): Style? = emojiCaseMap[emojiValue]
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
        /**
         * Creates a reaction with the given style, applied by the
         * current user.
         *
         * @param style The reaction's style.
         *
         * @return The reaction, or `null` if the current user
         *   identifier has not been set.
         */
        fun from(style: Style): Reaction? {
            val currentUserID = User.currentUserID ?: return null
            return Reaction(style, userID = currentUserID)
        }

        override fun canDecode(data: Map<String, Any?>): Boolean {
            val encodedStyle = data[Keys.STYLE.rawValue] as? String ?: return false
            return Style.from(encodedStyle) != null && data[Keys.USER_ID.rawValue] is String
        }

        override fun decode(data: Map<String, Any?>): Reaction {
            val encodedStyle = data[Keys.STYLE.rawValue] as? String
            val style = encodedStyle?.let { Style.from(it) }
            val userID = data[Keys.USER_ID.rawValue] as? String

            if (style == null || userID == null) {
                throw Exception.Networking.decodingFailed(
                    data,
                    ExceptionMetadata(this),
                )
            }

            return Reaction(
                style = style,
                userID = userID,
            )
        }
    }
}
