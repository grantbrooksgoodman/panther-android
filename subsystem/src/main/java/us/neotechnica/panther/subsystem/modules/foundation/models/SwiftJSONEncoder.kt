//
//  SwiftJSONEncoder.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.models

/**
 * Reproduces Foundation's `JSONEncoder` byte output for string
 * arrays.
 *
 * Identity hashes are computed over the JSON encoding of a
 * string array (see [EncodedHashable][us.neotechnica.panther.subsystem.modules.foundation.interfaces.EncodedHashable]),
 * so the encoded bytes must match the `JSONEncoder` output
 * exactly – a one-byte difference changes every identity hash.
 *
 * The escaping rules are those of an unconfigured `JSONEncoder`:
 *
 * - Compact output – no whitespace.
 * - Short escapes: `\"`, `\\`, `\/`, `\b`, `\f`, `\n`, `\r`,
 *   `\t`.
 * - Other control characters below U+0020 escape as `\uXXXX`
 *   with lowercase hexadecimal digits.
 * - All other characters – including U+007F and non-ASCII –
 *   emit as raw UTF-8.
 *
 * **Important:** General-purpose JSON libraries do not reproduce
 * this escaping (notably `\/`) by default. Use this encoder for
 * every hash computation.
 */
object SwiftJSONEncoder {
    // MARK: - Methods

    /**
     * Returns the JSON encoding of the given string array as
     * UTF-8 bytes.
     *
     * @param strings The strings to encode.
     *
     * @return The encoded bytes.
     */
    fun encode(strings: List<String>): ByteArray = encodeToString(strings).toByteArray(Charsets.UTF_8)

    /**
     * Returns the JSON encoding of the given string array.
     *
     * @param strings The strings to encode.
     *
     * @return The encoded JSON string.
     */
    fun encodeToString(strings: List<String>): String =
        strings.joinToString(
            prefix = "[",
            postfix = "]",
            separator = ",",
        ) { "\"${escape(it)}\"" }

    // MARK: - Auxiliary

    private fun escape(string: String): String {
        val builder = StringBuilder(string.length)
        for (character in string) {
            when (character) {
                '"' -> builder.append("\\\"")

                '\\' -> builder.append("\\\\")

                '/' -> builder.append("\\/")

                '\b' -> builder.append("\\b")

                '\u000C' -> builder.append("\\f")

                '\n' -> builder.append("\\n")

                '\r' -> builder.append("\\r")

                '\t' -> builder.append("\\t")

                else ->
                    if (character < ' ') {
                        builder.append("\\u%04x".format(character.code))
                    } else {
                        builder.append(character)
                    }
            }
        }

        return builder.toString()
    }
}
