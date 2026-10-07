//
//  GroupedListView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.constants.GroupedListViewFloats
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors

// MARK: - Constants Accessors

private typealias GroupedListFloats = GroupedListViewFloats

/**
 * A rounded group of list rows separated by dividers.
 *
 * Pass the rows' configurations in display order. Header and footer
 * captions on the individual rows are concatenated into a single
 * caption above and below the group:
 *
 * ```kotlin
 * GroupedListView(
 *     listOf(
 *         ListRowView.Configuration(interaction, innerText = "Send feedback"),
 *         ListRowView.Configuration(interaction, innerText = "Report a bug"),
 *     ),
 * )
 * ```
 */
@Composable
fun GroupedListView(
    rows: List<ListRowView.Configuration>,
    headerText: String? = null,
    footerText: String? = null,
) {
    val resolvedHeaderText = headerText ?: rows.concatenatedHeaderText()
    val resolvedFooterText = footerText ?: rows.concatenatedFooterText()
    val strippedRows = rows.map { it.strippingMetadata() }

    if (resolvedHeaderText != null || resolvedFooterText != null) {
        Column {
            resolvedHeaderText?.let { header ->
                Components.Text(
                    header.uppercase(),
                    foregroundColor = LocalPantherColors.current.subtitleText,
                    font = Font.system(FontScale.Custom(GroupedListFloats.HEADER_LABEL_SYSTEM_FONT_SCALE)),
                    modifier = Modifier.padding(horizontal = GroupedListFloats.HEADER_LABEL_HORIZONTAL_PADDING.dp),
                )
            }

            ListView(strippedRows)

            resolvedFooterText?.let { footer ->
                Components.Text(
                    footer,
                    foregroundColor = LocalPantherColors.current.subtitleText,
                    font = Font.system(FontScale.Custom(GroupedListFloats.FOOTER_LABEL_SYSTEM_FONT_SCALE)),
                    modifier =
                        Modifier
                            .padding(horizontal = GroupedListFloats.FOOTER_LABEL_HORIZONTAL_PADDING.dp)
                            .padding(top = 1.dp),
                )
            }
        }
    } else {
        ListView(strippedRows)
    }
}

// MARK: - List

@Composable
private fun ListView(rows: List<ListRowView.Configuration>) {
    Column(modifier = Modifier.clip(RoundedCornerShape(GroupedListFloats.CORNER_RADIUS.dp))) {
        rows.forEachIndexed { index, row ->
            ListRowView(row)
            if (index != rows.lastIndex) {
                HorizontalDivider(
                    modifier =
                        Modifier.padding(
                            start =
                                if (row.imageView == null) {
                                    GroupedListFloats.DIVIDER_LEADING_PADDING.dp
                                } else {
                                    GroupedListFloats.DIVIDER_ALTERNATE_LEADING_PADDING.dp
                                },
                        ),
                )
            }
        }
    }
}

// MARK: - Auxiliary

private fun List<ListRowView.Configuration>.concatenatedFooterText(): String? {
    val countGreaterThanOne = size > 1
    val concatenated =
        mapNotNull { configuration ->
            configuration.footerText?.let { footerText ->
                if (countGreaterThanOne) "${configuration.innerText.uppercase()}\n$footerText" else footerText
            }
        }.joinToString("\n\n").trim()

    return concatenated.ifBlank { null }
}

private fun List<ListRowView.Configuration>.concatenatedHeaderText(): String? {
    val concatenated = mapNotNull { it.headerText }.joinToString(" / ").trim()
    return concatenated.ifBlank { null }
}
