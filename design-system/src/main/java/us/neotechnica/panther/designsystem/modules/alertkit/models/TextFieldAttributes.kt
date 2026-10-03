//
//  TextFieldAttributes.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 02/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign

/**
 * A configuration that describes the appearance and behavior of the
 * text field in a [TextInputAlert].
 *
 * Use [TextFieldAttributes] to customize properties such as the
 * keyboard type, capitalization, and placeholder text. When you omit
 * parameters, the field uses sentence capitalization, the standard
 * keyboard, and center-aligned text.
 *
 * @property capitalizationType The autocapitalization style.
 * @property clearButtonMode When the clear button appears.
 * @property correctionType The autocorrection behavior.
 * @property isSecureTextEntry Whether the field hides its input.
 * @property keyboardType The type of keyboard to display.
 * @property placeholderText The placeholder shown when the field is
 *   empty.
 * @property sampleText The text prepopulated in the field.
 * @property textAlignment The alignment of the text.
 */
data class TextFieldAttributes(
    val capitalizationType: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    val clearButtonMode: ClearButtonMode = ClearButtonMode.NEVER,
    val correctionType: CorrectionType = CorrectionType.DEFAULT,
    val isSecureTextEntry: Boolean = false,
    val keyboardType: KeyboardType = KeyboardType.Text,
    val placeholderText: String? = null,
    val sampleText: String? = null,
    val textAlignment: TextAlign = TextAlign.Center,
) {
    // MARK: - Types

    /** When the text field's clear button is shown. */
    enum class ClearButtonMode {
        NEVER,
        WHILE_EDITING,
        UNLESS_EDITING,
        ALWAYS,
    }

    /** The text field's autocorrection behavior. */
    enum class CorrectionType {
        DEFAULT,
        NO,
        YES,
    }

    // MARK: - Methods

    /** Returns a copy with the placeholder text replaced. */
    fun replacingPlaceholderText(placeholderText: String): TextFieldAttributes = copy(placeholderText = placeholderText)

    /** Returns a copy with the sample text replaced. */
    fun replacingSampleText(sampleText: String): TextFieldAttributes = copy(sampleText = sampleText)
}
