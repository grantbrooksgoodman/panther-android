//
//  SearchBar.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.TextFit
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.shared.constants.SearchBarColors
import us.neotechnica.panther.modules.content.shared.constants.SearchBarFloats
import us.neotechnica.panther.modules.content.shared.constants.SearchBarStrings
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.subsystem.modules.localization.models.localized

// MARK: - Constants Accessors

private typealias Colors = SearchBarColors
private typealias Floats = SearchBarFloats
private typealias Strings = SearchBarStrings

/**
 * A search field: a magnifying-glass glyph, a placeholder, a
 * single-line input, and a clear button that appears while the query
 * is non-empty.
 *
 * @param query The current query text.
 * @param onQueryChange Invoked as the query changes.
 * @param modifier The modifier for this bar (for outer positioning).
 * @param bottomPadding The padding below the field.
 * @param keyboardType The soft-keyboard type for the input field, or
 *   `null` for the system default.
 * @param placeholderText The placeholder shown while the query is
 *   empty. Defaults to the localized search prompt.
 * @param onSubmit The handler to run with the current query when the
 *   user submits, if any.
 */
@Composable
@Suppress("LongParameterList")
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Float = Floats.DEFAULT_BOTTOM_PADDING,
    keyboardType: KeyboardType? = null,
    placeholderText: String = LocalizedStringKey.Search.localized(),
    onSubmit: ((String) -> Unit)? = null,
) {
    val colors = LocalPantherColors.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.navigationBarBackground)
                .padding(bottom = bottomPadding.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Floats.INNER_ROW_CORNER_RADIUS.dp))
                    .background(
                        if (isSystemInDarkTheme()) {
                            Colors.INNER_ROW_DARK_BACKGROUND
                        } else {
                            Colors.INNER_ROW_LIGHT_BACKGROUND
                        },
                    ).padding(horizontal = Floats.INNER_ROW_HORIZONTAL_PADDING.dp),
        ) {
            Components.Symbol(
                Strings.SEARCH_IMAGE_SYSTEM_NAME,
                foregroundColor = colors.subtitleText,
            )

            Box(
                contentAlignment = Alignment.CenterStart,
                modifier =
                    Modifier
                        .weight(1f)
                        .height(Floats.TEXT_FIELD_FRAME_HEIGHT.dp)
                        .padding(start = Floats.INNER_ROW_HORIZONTAL_PADDING.dp),
            ) {
                if (query.isEmpty()) {
                    Components.FittedText(
                        placeholderText,
                        foregroundColor = colors.subtitleText,
                        fit =
                            TextFit(
                                maxLines = 1,
                                minimumScaleFactor = Floats.TEXT_FIELD_MINIMUM_SCALE_FACTOR,
                                overflow = TextOverflow.Ellipsis,
                            ),
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    keyboardActions = KeyboardActions(onDone = { onSubmit?.invoke(query) }),
                    keyboardOptions =
                        KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = keyboardType ?: KeyboardType.Text,
                        ),
                    textStyle = Font.system.textStyle.copy(color = colors.titleText),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Components.Button(
                symbolName = Strings.CLEAR_BUTTON_IMAGE_SYSTEM_NAME,
                foregroundColor = colors.subtitleText,
                onClick = { onQueryChange("") },
                modifier = Modifier.alpha(if (query.isEmpty()) 0f else Floats.CLEAR_BUTTON_IMAGE_OPACITY),
            )
        }
    }
}

// MARK: - View Builder

/**
 * Creates a view that combines a search bar with the given content.
 *
 * The bar sits above the content.
 *
 * @param query The search query the bar displays and edits.
 * @param onQueryChange Invoked as the query changes.
 * @param keyboardType The keyboard type to display during editing,
 *   or `null` for the system default.
 * @param placeholderText The placeholder string to display while the
 *   query is empty. Defaults to the localized search prompt.
 * @param onSubmit The handler to run with the current query when the
 *   user submits, if any.
 * @param content The content the search bar filters.
 */
@Composable
@Suppress("LongParameterList")
fun SearchBarInView(
    query: String,
    onQueryChange: (String) -> Unit,
    keyboardType: KeyboardType? = null,
    placeholderText: String = LocalizedStringKey.Search.localized(),
    onSubmit: ((String) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column {
        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            keyboardType = keyboardType,
            placeholderText = placeholderText,
            onSubmit = onSubmit,
        )

        content()
    }
}
