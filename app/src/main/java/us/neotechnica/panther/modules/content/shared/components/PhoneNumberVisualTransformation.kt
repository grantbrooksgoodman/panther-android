//
//  PhoneNumberVisualTransformation.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 23/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.google.i18n.phonenumbers.PhoneNumberUtil

/**
 * A [VisualTransformation] that displays a raw-digit phone number
 * formatted for [regionCode] while keeping the underlying edit buffer as
 * plain digits.
 *
 * Because the buffer never contains the inserted separators, the caret
 * advances one step per digit and is never displaced when formatting
 * adds a `-`, `)`, or space; the [OffsetMapping] translates between the
 * raw-digit offsets and the formatted display offsets by counting
 * digits.
 *
 * @param regionCode The region whose formatting conventions to apply.
 */
class PhoneNumberVisualTransformation(
    private val regionCode: String,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        if (digits.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val formatter = PhoneNumberUtil.getInstance().getAsYouTypeFormatter(regionCode.uppercase())
        var formatted = ""
        for (character in digits) if (character.isDigit()) formatted = formatter.inputDigit(character)

        // The display index of each raw digit, so offsets can map both ways.
        val digitDisplayOffsets = formatted.indices.filter { formatted[it].isDigit() }

        val offsetMapping =
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    if (offset <= 0) return 0
                    if (offset > digitDisplayOffsets.size) return formatted.length
                    return digitDisplayOffsets[offset - 1] + 1
                }

                override fun transformedToOriginal(offset: Int): Int {
                    val clamped = offset.coerceIn(0, formatted.length)
                    return (0 until clamped).count { formatted[it].isDigit() }
                }
            }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}
