//
//  AlertLabelView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import us.neotechnica.panther.designsystem.modules.alertkit.constants.AlertLabelViewFloats

// MARK: - Constants Accessors

private typealias LabelFloats = AlertLabelViewFloats

// A centered, multiline label for an alert's or action sheet's title
// or message. When a paragraph would end with a short lone word on its
// own line, the label moves the last word of the line above down to
// join it.
@Composable
internal fun AlertLabelView(
    text: AnnotatedString,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(modifier = modifier) {
        val maximumWidth = constraints.maxWidth
        val displayedText =
            remember(text, style, maximumWidth) {
                text.pushingOutOrphans(
                    textMeasurer.measure(
                        text,
                        constraints = Constraints(maxWidth = maximumWidth),
                        style = style,
                    ),
                )
            }

        Text(
            displayedText,
            color = color,
            style = style,
            textAlign = TextAlign.Center,
        )
    }
}

// MARK: - Auxiliary

// Breaks the line before the last word of each paragraph's second-to-
// last line when the paragraph's last line holds a single word that
// spans less than a set fraction of the label's width.
private fun AnnotatedString.pushingOutOrphans(layout: TextLayoutResult): AnnotatedString {
    val breakIndices =
        (1 until layout.lineCount).mapNotNull { line ->
            val lineStart = layout.getLineStart(line)
            val lineText = text.substring(lineStart, layout.getLineEnd(line, visibleEnd = true))
            val previousLineStart = layout.getLineStart(line - 1)
            val previousLineText = text.substring(previousLineStart, layout.getLineEnd(line - 1, visibleEnd = true))
            val endsParagraph = layout.getLineEnd(line) == text.length || text[layout.getLineEnd(line) - 1] == '\n'
            val followsLineBreak = text.substring(previousLineStart, lineStart).contains('\n')
            val lastSpaceIndex = previousLineText.lastIndexOf(' ')
            val isShort =
                layout.getLineRight(line) - layout.getLineLeft(line) <
                    layout.layoutInput.constraints.maxWidth * LabelFloats.ORPHAN_MAXIMUM_WIDTH_FRACTION

            if (lineText.isEmpty() ||
                lineText.contains(' ') ||
                !endsParagraph ||
                !isShort ||
                followsLineBreak ||
                lastSpaceIndex < 0
            ) {
                return@mapNotNull null
            }

            previousLineStart + lastSpaceIndex
        }

    if (breakIndices.isEmpty()) return this
    return buildAnnotatedString {
        var start = 0
        breakIndices.forEach { index ->
            append(this@pushingOutOrphans.subSequence(start, index))
            append('\n')
            start = index + 1
        }

        append(this@pushingOutOrphans.subSequence(start, this@pushingOutOrphans.length))
    }
}
