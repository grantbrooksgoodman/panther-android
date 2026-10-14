//
//  AppConstants+ChatInfoPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.constants

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

// MARK: - Float

object ChatInfoPageViewFloats {
    val addContactButtonCircleFrameMaxHeight: Dp = 40.dp
    val addContactButtonCircleFrameMaxWidth: Dp = 40.dp
    val addContactButtonCircleTrailingPadding: Dp = 2.dp

    val addContactButtonImageHeight: Dp = 24.dp
    val addContactButtonImageWidth: Dp = 24.dp

    /** The height of the add-contact symbol as an SF Symbol, which fills its frame. */
    val addContactButtonSymbolImageHeight: Dp = 15.dp

    /** The width of the add-contact symbol as an SF Symbol, which fills its frame. */
    val addContactButtonSymbolImageWidth: Dp = 15.dp

    val avatarImageViewHorizontalPadding: Dp = 10.dp
    val avatarImageViewSizeHeight: Dp = 100.dp
    val avatarImageViewSizeWidth: Dp = 100.dp
    val avatarImageViewTopPadding: Dp = 20.dp

    val bottomSpacerPadding: Dp = 24.dp

    val cardCornerRadius: Dp = 16.dp
    val cardHorizontalMargin: Dp = 16.dp
    val cardHorizontalPadding: Dp = 16.dp
    val cardPadding: Dp = 16.dp
    val cardVerticalMargin: Dp = 8.dp

    val changeMetadataButtonHorizontalPadding: Dp = 10.dp
    const val CHANGE_METADATA_BUTTON_LABEL_FONT_SIZE = 15f

    const val CHAT_INFO_CELL_SUBTITLE_LABEL_FONT_SIZE = 15f
    val chatTitleLabelHorizontalPadding: Dp = 10.dp

    val chevronBoxBorderWidth: Dp = 1.dp
    val chevronBoxSize: Dp = 24.dp
    val chevronBoxStartPadding: Dp = 8.dp

    /**
     * The size of the participants toggle's circled chevron as an SF
     * Symbol: its size beside body text.
     */
    val chevronCircleSymbolSize: Dp = 16.93.dp
    val chevronGlyphSize: Dp = 16.dp

    val leaveConversationListRowViewHorizontalPadding: Dp = 20.dp
    val listTransitionAnimationDuration: Duration = 200.milliseconds
    val listViewYOffset: Dp = (-8).dp
    val listViewAlternateYOffset: Dp = (-30).dp

    val rowVerticalPadding: Dp = 10.dp

    val segmentCornerRadius: Dp = 7.dp
    val segmentHorizontalPadding: Dp = 10.dp
    val segmentVerticalPadding: Dp = 6.dp
    val segmentedControlCornerRadius: Dp = 9.dp
    val segmentedControlHorizontalOrLeadingPadding: Dp = 20.dp
    val segmentedControlTopPadding: Dp = 20.dp
    val segmentedControlTrackPadding: Dp = 2.dp
    const val SEGMENTED_CONTROL_MAX_WIDTH_FRACTION = 2f / 3f
}

// MARK: - Color

object ChatInfoPageViewColors {
    val addContactButtonCircleDarkForeground = Color(0xFF3A3A3C)
    val addContactButtonCircleLightForeground = Color(0xFFE5E5EA)

    val leaveConversationListRowViewForeground = Color(0xFFFF3B30)

    val segmentedControlTrack = Color(0x1F787880)

    val addContactButtonLabelForeground: Color
        @Composable get() = LocalPantherColors.current.navigationBarButton

    val addContactButtonSymbolForeground: Color
        @Composable get() = LocalPantherColors.current.navigationBarButton

    val changeMetadataButtonForeground: Color
        @Composable get() = LocalPantherColors.current.navigationBarButton
}

// MARK: - String

// Non-translated per-screen constants. The `ChatInfoPageViewStrings`
// name is reserved for the translated-label-strings object.
object ChatInfoPageViewConstants {
    const val ADD_CONTACT_BUTTON_IMAGE_SYSTEM_NAME = "plus"
    const val DONE_BUTTON_IMAGE_SYSTEM_NAME = "checkmark"
}
