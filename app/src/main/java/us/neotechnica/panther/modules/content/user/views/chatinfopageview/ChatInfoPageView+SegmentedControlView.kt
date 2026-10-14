//
//  ChatInfoPageView+SegmentedControlView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatinfopageview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.constants.ChatInfoPageViewColors
import us.neotechnica.panther.modules.content.user.constants.ChatInfoPageViewFloats
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

// MARK: - Constants Accessors

private typealias ControlColors = ChatInfoPageViewColors
private typealias ControlFloats = ChatInfoPageViewFloats

// MARK: - Segmented Control View

// The leading-aligned segmented control between the participants and
// media lists, shown only when the conversation has shared media. Its
// segments size to their content up to two-thirds of the screen's
// width, or fill the width for long localized titles.
@Composable
internal fun SegmentedControlView(
    state: ChatInfoPageReducer.State,
    viewModel: ViewModel<ChatInfoPageReducer.State, ChatInfoPageReducer.Action>,
) {
    if (state.mediaItemMetadata.isEmpty()) return
    val shouldElongate = state.shouldElongateSegmentedControl

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = ControlFloats.segmentedControlTopPadding)
                .padding(
                    start = ControlFloats.segmentedControlHorizontalOrLeadingPadding,
                    end = if (shouldElongate) ControlFloats.segmentedControlHorizontalOrLeadingPadding else 0.dp,
                ),
    ) {
        key(state.segmentedControlViewID) {
            SegmentedControl(
                titles = state.segmentedControlOptionTitles,
                selectedIndex = state.segmentedControlSelectionIndex,
                onSelect = { viewModel.send(ChatInfoPageReducer.Action.SegmentedControlSelectionIndexChanged(it)) },
                modifier =
                    if (shouldElongate) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier.widthIn(max = state.segmentedControlMaxWidth).width(IntrinsicSize.Max)
                    },
            )
        }

        if (!shouldElongate) Spacer(modifier = Modifier.weight(1f))
    }
}

// MARK: - Segmented Control

@Composable
private fun SegmentedControl(
    titles: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPantherColors.current
    val isDarkMode = ThemeService.isDarkModeActive(isSystemInDarkTheme())

    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(ControlFloats.segmentedControlCornerRadius))
                .background(ControlColors.segmentedControlTrack)
                .padding(ControlFloats.segmentedControlTrackPadding),
    ) {
        titles.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(ControlFloats.segmentCornerRadius))
                        .background(if (isSelected) (if (isDarkMode) Color.Gray else Color.White) else Color.Transparent)
                        .clickable { onSelect(index) }
                        .padding(
                            horizontal = ControlFloats.segmentHorizontalPadding,
                            vertical = ControlFloats.segmentVerticalPadding,
                        ),
            ) {
                Components.Text(
                    title,
                    foregroundColor = if (isSelected) colors.titleText else colors.subtitleText,
                    font = if (isSelected) Font.systemSemibold(FontScale.Small) else Font.system(FontScale.Small),
                )
            }
        }
    }
}
