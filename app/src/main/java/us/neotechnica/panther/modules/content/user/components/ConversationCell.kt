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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
 * A single conversation row: an unread dot, avatar, title with an
 * optional language chip, message preview, and timestamp.
 *
 * The cell data is resolved asynchronously (message previews may resolve
 * translations), keyed on the conversation and [changeToken] so the row
 * refreshes when the store updates.
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
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(start = 12.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Box(modifier = Modifier.width(20.dp), contentAlignment = Alignment.Center) {
            if (data.isShowingUnreadIndicator) {
                Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(colors.accent))
            }
        }

        Avatar(data, colors.background)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Material3Text(
                    data.title,
                    color = colors.titleText,
                    style = Font.systemSemibold().textStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                data.otherLanguageCode?.let { LanguageChip(it) }
                Spacer(modifier = Modifier.weight(1f))
                Components.Text(data.dateLabelText, color = colors.subtitleText, font = Font.system(FontScale.Small))
            }
            Material3Text(
                data.subtitle.ifBlank { " " },
                color = colors.subtitleText,
                style = Font.system(FontScale.Small).textStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun Avatar(
    data: ConversationCellViewData,
    glyphColor: Color,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(48.dp).clip(CircleShape).background(AVATAR_BACKGROUND),
    ) {
        when {
            data.isGroup -> Components.Symbol("person", color = glyphColor, modifier = Modifier.size(24.dp))
            data.hasContactName -> Components.Text(data.initials, color = glyphColor, font = Font.systemSemibold())
            else -> Components.Symbol("person", color = glyphColor, modifier = Modifier.size(24.dp))
        }

        if (data.isGroup) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(glyphColor),
            ) {
                Components.Text(
                    data.participantCount.toString(),
                    color = AVATAR_BACKGROUND,
                    font = Font.systemBold(FontScale.Small),
                )
            }
        }
    }
}

@Composable
private fun LanguageChip(languageCode: String) {
    val colors = LocalPantherColors.current
    val flag = flagFor(languageCode)
    val label = languageCode.uppercase() + if (flag != null) " $flag" else ""
    Box(
        modifier =
            Modifier
                .padding(start = 6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.groupedContentBackground)
                .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Components.Text(label, color = colors.subtitleText, font = Font.systemMedium(FontScale.Small))
    }
}

private fun flagFor(languageCode: String): String? =
    when (languageCode.lowercase()) {
        "en" -> "🇺🇸"
        "es" -> "🇪🇸"
        "de" -> "🇩🇪"
        "fr" -> "🇫🇷"
        "it" -> "🇮🇹"
        "pt" -> "🇵🇹"
        "ru" -> "🇷🇺"
        "uk" -> "🇺🇦"
        "zh" -> "🇨🇳"
        "ja" -> "🇯🇵"
        "ko" -> "🇰🇷"
        "nl" -> "🇳🇱"
        "pl" -> "🇵🇱"
        "tr" -> "🇹🇷"
        "ar" -> "🇸🇦"
        "hi" -> "🇮🇳"
        else -> null
    }

private val AVATAR_BACKGROUND = Color(0xFFC7C7CC)
