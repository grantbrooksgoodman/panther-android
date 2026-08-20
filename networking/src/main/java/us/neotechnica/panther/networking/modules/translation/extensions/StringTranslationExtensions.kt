//
//  StringTranslationExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.extensions

import android.util.Base64
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHashOf
import java.io.ByteArrayOutputStream

// MARK: - Hashing

/** The SHA-256 encoded hash of this string, matching `encodedHashOf([self])`. */
internal val String.encodedHash: String
    get() = encodedHashOf(listOf(this))

// MARK: - Constants

/** The separator between the input and output halves of an archive value: EN DASH (U+2013). */
internal const val TRANSLATION_COMPONENT_SEPARATOR = "–"

// MARK: - Percent Encoding

/**
 * The string percent-encoded like the iOS
 * `addingPercentEncoding(withAllowedCharacters: .alphanumerics)`.
 *
 * Empirically – and pinned by `translation_reference_vectors.json` –
 * that character set leaves only ASCII `[0-9A-Za-z]` unescaped and
 * percent-encodes every other byte of the UTF-8 representation as an
 * uppercase `%XX` sequence.
 */
internal val String.alphaEncoded: String
    get() {
        val builder = StringBuilder()
        for (byte in toByteArray(Charsets.UTF_8)) {
            val value = byte.toInt() and BYTE_MASK
            if (value.isAsciiAlphanumeric) {
                builder.append(value.toChar())
            } else {
                builder.append('%').append("%02X".format(value))
            }
        }
        return builder.toString()
    }

/**
 * The string with its percent-escapes decoded, like the iOS
 * `removingPercentEncoding`, or `null` if a `%` escape is malformed.
 *
 * Unlike form decoding, `+` is preserved (not turned into a space).
 */
internal val String.percentDecoded: String?
    get() {
        val bytes = ByteArrayOutputStream()
        var index = 0
        while (index < length) {
            val character = this[index]
            if (character == '%') {
                if (index + HEX_DIGIT_COUNT >= length) return null
                val hex = substring(index + 1, index + 1 + HEX_DIGIT_COUNT)
                val byte = hex.toIntOrNull(HEX_RADIX) ?: return null
                bytes.write(byte)
                index += 1 + HEX_DIGIT_COUNT
            } else {
                bytes.write(character.code)
                index += 1
            }
        }
        return bytes.toByteArray().toString(Charsets.UTF_8)
    }

// MARK: - Sanitization

/** The string with the translation processing sentinels removed. */
internal val String.sanitized: String
    get() = replace("⁂", "").replace("⌘", "").replace("※", "")

// MARK: - Base64

/** The string's UTF-8 bytes, Base64-encoded without line wrapping. */
internal val String.base64Encoded: String
    get() = Base64.encodeToString(toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

/** The string decoded from Base64, or itself if it is not valid Base64. */
internal val String.base64Decoded: String
    get() =
        try {
            String(Base64.decode(this, Base64.DEFAULT), Charsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            this
        }

// MARK: - Components

/**
 * The `(input, output)` pair decoded from an archive value string of
 * the form `"<alphaEncoded(input)>–<alphaEncoded(output)>"`, or `null`
 * if the string is not a valid two-component value.
 */
internal val String.decodedTranslationComponents: Pair<String, String>?
    get() {
        val components = split(TRANSLATION_COMPONENT_SEPARATOR)
        if (components.size != 2) return null
        val input = components[0].percentDecoded ?: return null
        val output = components[1].percentDecoded ?: return null
        return input to output
    }

// MARK: - Auxiliary

private const val HEX_RADIX = 16
private const val HEX_DIGIT_COUNT = 2
private const val BYTE_MASK = 0xFF

private val Int.isAsciiAlphanumeric: Boolean
    get() = this in 0x30..0x39 || this in 0x41..0x5A || this in 0x61..0x7A
