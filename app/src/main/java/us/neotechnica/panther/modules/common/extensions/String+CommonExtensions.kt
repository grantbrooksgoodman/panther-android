//
//  String+CommonExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.extensions

import us.neotechnica.panther.networking.modules.translation.extensions.sanitized
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHashOf
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.translator.Translator
import us.neotechnica.panther.translator.models.LanguagePair
import java.text.Normalizer

// NIT: Unused on Android.

/**
 * A localized representation of the string, resolved from the
 * translation archive.
 *
 * Looks up an archived translation of the string targeting the
 * current language and returns its output. If no archived translation
 * exists, the string is returned unchanged.
 */
val String.localized: String
    get() =
        Translator.config.archiverDelegate
            ?.getValue(
                inputValueEncodedHash = encodedHashOf(listOf(this)),
                languagePair = LanguagePair(from = "en", to = RuntimeStorage.languageCode),
            )?.output
            ?.sanitized
            ?: this

/**
 * A four-character code composed of the string's first two and last
 * two characters, uppercased.
 *
 * Use this property to display a compact reference to a long
 * identifier.
 */
val String.shortCode: String
    get() = "${take(2)}${takeLast(2)}".uppercase()

/**
 * The first 32 characters of the string.
 *
 * If the string contains 32 or fewer characters, this property
 * returns the string unchanged.
 */
val String.shortened: String
    get() = take(32)

// MARK: - Ordering

/**
 * Orders strings by the Unicode scalar values of their canonical
 * (NFC) form, so names sort the way a scalar-based comparison does
 * rather than by UTF-16 code unit.
 */
val codePointOrder: Comparator<String> =
    Comparator { left, right ->
        val leftPoints = Normalizer.normalize(left, Normalizer.Form.NFC).codePoints().toArray()
        val rightPoints = Normalizer.normalize(right, Normalizer.Form.NFC).codePoints().toArray()
        for (index in 0 until minOf(leftPoints.size, rightPoints.size)) {
            val difference = leftPoints[index].compareTo(rightPoints[index])
            if (difference != 0) return@Comparator difference
        }
        leftPoints.size.compareTo(rightPoints.size)
    }
