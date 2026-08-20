//
//  StringExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.extensions

import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHashOf
import us.neotechnica.panther.translator.Translator

// MARK: - Hashing

/**
 * The SHA-256 encoded hash of this string, matching the iOS
 * `String.encodedHash` (`encodedHashOf([self])`). Used as the archive
 * key for a translation input value.
 */
internal val String.encodedHash: String
    get() = encodedHashOf(listOf(this))

// MARK: - Trimming

/**
 * The string lowercased with all spaces (including non-breaking
 * spaces) and newlines removed.
 */
internal fun String.lowercasedTrimmingWhitespaceAndNewlines(): String = lowercase().trimmingWhitespace().trimmingNewlines()

/**
 * The string with trailing spaces (including non-breaking spaces)
 * and newlines removed.
 */
internal fun String.trimmingTrailingWhitespaceAndNewlines(): String = trimmingTrailingWhitespace().trimmingTrailingNewlines()

/** The string with trailing spaces and non-breaking spaces removed. */
internal fun String.trimmingTrailingWhitespace(): String {
    var string = this
    while (string.endsWith(" ") || string.endsWith(" ")) {
        string = string.dropLast(1)
    }
    return string
}

private fun String.trimmingTrailingNewlines(): String {
    var string = this
    while (string.endsWith("\n")) {
        string = string.dropLast(1)
    }
    return string
}

private fun String.trimmingNewlines(): String = replace("\n", "")

private fun String.trimmingWhitespace(): String = replace(" ", "").replace(" ", "")

// MARK: - Letters

/** The index of the first letter character, or `null` if none. */
internal val String.indexOfFirstLetter: Int?
    get() {
        for ((index, character) in withIndex()) {
            if (character.isLetter()) return index
        }
        return null
    }

/**
 * A Boolean value indicating whether the string contains at least
 * one non-control, assigned letter character.
 */
fun String.containsLetters(): Boolean =
    codePoints().anyMatch { codePoint ->
        Character.isLetter(codePoint) &&
            !Character.isISOControl(codePoint) &&
            Character.isDefined(codePoint)
    }

/**
 * Returns the string with the case of its first letter matched to
 * the first letter of [comparator], when both share the same first-
 * letter index.
 */
internal fun String.capitalized(relativeTo: String): String {
    val firstLetterIndex = indexOfFirstLetter
    if (firstLetterIndex == null || firstLetterIndex != relativeTo.indexOfFirstLetter) {
        return this
    }

    val firstLetterInComparator = relativeTo[firstLetterIndex]
    val characters = toCharArray()

    when {
        firstLetterInComparator.isUpperCase() ->
            characters[firstLetterIndex] = characters[firstLetterIndex].uppercaseChar()

        firstLetterInComparator.isLowerCase() ->
            characters[firstLetterIndex] = characters[firstLetterIndex].lowercaseChar()
    }

    return String(characters)
}

// MARK: - Tokenization

/**
 * Replaces occurrences of [token] with successive [slices],
 * reversing [tokenized].
 *
 * Returns the string unchanged if the token count and slice count
 * do not correspond.
 */
internal fun String.replacing(
    token: String,
    with: List<String>,
): String {
    val components = split(token)
    if (components.size - 1 <= 0 || with.size != components.size - 1) return this

    val result = StringBuilder()
    for (index in 0..components.size - 2) {
        result.append(components[index] + with[index].replace(token, ""))
    }
    return result.toString() + components.last()
}

/**
 * Splits the string on [delimiter]-bounded tokens, replacing each
 * with the processing token and returning the processed string
 * alongside the extracted, canonized token slices.
 */
internal fun String.tokenized(delimiter: String): Pair<String, List<String>> {
    val processingToken = Translator.Constants.PROCESSING_TOKEN
    val components = split(delimiter)

    val extractedTokens = mutableListOf<String>()
    var index = 0
    while (index < components.size - 1) {
        if (components.size > index + 1) {
            extractedTokens.add(components[index + 1])
        }
        index += 2
    }

    var processedString = this
    val validTokens = mutableListOf<String>()

    for (token in extractedTokens) {
        val canonizedToken = "$delimiter$token$delimiter"
        val tokensAreUnique = extractedTokens.toSet().size == extractedTokens.size
        val isAbsentUniqueToken = tokensAreUnique && processedString.split(canonizedToken).size - 1 <= 0
        val isEmptyToken = canonizedToken == "$delimiter$delimiter"

        if (isAbsentUniqueToken || isEmptyToken) continue

        validTokens.add(canonizedToken)
        processedString = processedString.replace(canonizedToken, processingToken)
    }

    return processedString to validTokens
}
