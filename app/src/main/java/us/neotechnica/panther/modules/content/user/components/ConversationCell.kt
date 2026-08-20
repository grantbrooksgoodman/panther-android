//
//  ConversationCell.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewData
import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation
import androidx.compose.material3.Text as Material3Text

/**
 * A single conversation row: avatar, title, message preview,
 * timestamp, and unread indicator.
 *
 * The cell data is resolved asynchronously (message previews may
 * resolve translations), keyed on the conversation and [changeToken]
 * so the row refreshes when the store updates.
 *
 * @param conversation The conversation to render.
 * @param languageCode The language to resolve previews into.
 * @param changeToken A token that changes when the store updates.
 * @param modifier The modifier for this cell.
 */
@Composable
fun ConversationCell(
    conversation: Conversation,
    languageCode: String,
    changeToken: Any,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPantherColors.current
    val cellData by produceState<ConversationCellViewData?>(
        initialValue = null,
        conversation,
        changeToken,
    ) {
        value = ConversationCellViewData.build(conversation, languageCode)
    }
    val data = cellData ?: return

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
        ) {
            Components.Text(data.initials, color = colors.background, font = Font.systemSemibold())
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Components.Text(data.title, color = colors.titleText, font = Font.systemSemibold())
            Material3Text(
                data.subtitle.ifBlank { " " },
                color = colors.subtitleText,
                style = Font.system(FontScale.Small).textStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Components.Text(data.dateLabelText, color = colors.subtitleText, font = Font.system(FontScale.Small))
            if (data.isShowingUnreadIndicator) {
                Box(
                    modifier =
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors.accent),
                )
            }
        }
    }
}
