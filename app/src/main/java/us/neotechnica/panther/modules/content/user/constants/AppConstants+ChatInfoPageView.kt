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

/** The layout and timing values of the chat info page view. */
object ChatInfoPageViewFloats {
    /** The maximum frame height of the add contact button circle. */
    val addContactButtonCircleFrameMaxHeight: Dp = 40.dp

    /** The maximum frame width of the add contact button circle. */
    val addContactButtonCircleFrameMaxWidth: Dp = 40.dp

    /** The trailing padding of the add contact button circle. */
    val addContactButtonCircleTrailingPadding: Dp = 2.dp

    /** The height of the add contact button image. */
    val addContactButtonImageHeight: Dp = 24.dp

    /** The width of the add contact button image. */
    val addContactButtonImageWidth: Dp = 24.dp

    /** The height of the add-contact symbol as an SF Symbol, which fills its frame. */
    val addContactButtonSymbolImageHeight: Dp = 15.dp

    /** The width of the add-contact symbol as an SF Symbol, which fills its frame. */
    val addContactButtonSymbolImageWidth: Dp = 15.dp

    /** The horizontal padding of the avatar image view. */
    val avatarImageViewHorizontalPadding: Dp = 10.dp

    /** The height of the avatar image view. */
    val avatarImageViewSizeHeight: Dp = 100.dp

    /** The width of the avatar image view. */
    val avatarImageViewSizeWidth: Dp = 100.dp

    /** The top padding of the avatar image view. */
    val avatarImageViewTopPadding: Dp = 20.dp

    /** The padding of the bottom spacer. */
    val bottomSpacerPadding: Dp = 24.dp

    /** The corner radius of the card. */
    val cardCornerRadius: Dp = 16.dp

    /** The horizontal margin of the card. */
    val cardHorizontalMargin: Dp = 16.dp

    /** The horizontal padding of the card. */
    val cardHorizontalPadding: Dp = 16.dp

    /** The padding of the card. */
    val cardPadding: Dp = 16.dp

    /** The vertical margin of the card. */
    val cardVerticalMargin: Dp = 8.dp

    /** The horizontal padding of the change metadata button. */
    val changeMetadataButtonHorizontalPadding: Dp = 10.dp

    /** The font size of the change metadata button label. */
    const val CHANGE_METADATA_BUTTON_LABEL_FONT_SIZE = 15f

    /** The font size of the chat info cell subtitle label. */
    const val CHAT_INFO_CELL_SUBTITLE_LABEL_FONT_SIZE = 15f

    /** The horizontal padding of the chat title label. */
    val chatTitleLabelHorizontalPadding: Dp = 10.dp

    /** The width of the chevron box border. */
    val chevronBoxBorderWidth: Dp = 1.dp

    /** The size of the chevron box. */
    val chevronBoxSize: Dp = 24.dp

    /** The start padding of the chevron box. */
    val chevronBoxStartPadding: Dp = 8.dp

    /**
     * The size of the participants toggle's circled chevron as an SF
     * Symbol: its size beside body text.
     */
    val chevronCircleSymbolSize: Dp = 16.93.dp

    /** The size of the chevron glyph. */
    val chevronGlyphSize: Dp = 16.dp

    /** The horizontal padding of the leave conversation list row view. */
    val leaveConversationListRowViewHorizontalPadding: Dp = 20.dp

    /** The duration of the list transition animation. */
    val listTransitionAnimationDuration: Duration = 200.milliseconds

    /** The vertical offset of the list view. */
    val listViewYOffset: Dp = (-8).dp

    /** The alternate vertical offset of the list view. */
    val listViewAlternateYOffset: Dp = (-30).dp

    /** The vertical padding of the row. */
    val rowVerticalPadding: Dp = 10.dp

    /** The corner radius of the segment. */
    val segmentCornerRadius: Dp = 7.dp

    /** The horizontal padding of the segment. */
    val segmentHorizontalPadding: Dp = 10.dp

    /** The vertical padding of the segment. */
    val segmentVerticalPadding: Dp = 6.dp

    /** The corner radius of the segmented control. */
    val segmentedControlCornerRadius: Dp = 9.dp

    /**
     * The horizontal padding of the segmented control, or its leading
     * padding when it is not elongated.
     */
    val segmentedControlHorizontalOrLeadingPadding: Dp = 20.dp

    /** The top padding of the segmented control. */
    val segmentedControlTopPadding: Dp = 20.dp

    /** The padding of the segmented control track. */
    val segmentedControlTrackPadding: Dp = 2.dp

    /** The fraction of the screen width the segmented control may span. */
    const val SEGMENTED_CONTROL_MAX_WIDTH_FRACTION = 2f / 3f
}

// MARK: - Color

/** The colors of the chat info page view. */
object ChatInfoPageViewColors {
    /** The foreground color of the add contact button circle in dark mode. */
    val addContactButtonCircleDarkForeground = Color(0xFF3A3A3C)

    /** The foreground color of the add contact button circle in light mode. */
    val addContactButtonCircleLightForeground = Color(0xFFE5E5EA)

    /** The foreground color of the leave conversation list row view. */
    val leaveConversationListRowViewForeground = Color(0xFFFF3B30)

    /** The track color of the segmented control. */
    val segmentedControlTrack = Color(0x1F787880)

    /** The foreground color of the add contact button label. */
    val addContactButtonLabelForeground: Color
        @Composable get() = LocalPantherColors.current.navigationBarButton

    /** The foreground color of the add contact button symbol. */
    val addContactButtonSymbolForeground: Color
        @Composable get() = LocalPantherColors.current.navigationBarButton

    /** The foreground color of the change metadata button. */
    val changeMetadataButtonForeground: Color
        @Composable get() = LocalPantherColors.current.navigationBarButton
}

// MARK: - String

// Non-translated per-screen constants. The `ChatInfoPageViewStrings`
// name is reserved for the translated-label-strings object.

/** The string constants of the chat info page view. */
object ChatInfoPageViewConstants {
    /** The system name of the add contact button image. */
    const val ADD_CONTACT_BUTTON_IMAGE_SYSTEM_NAME = "plus"

    /** The system name of the done button image. */
    const val DONE_BUTTON_IMAGE_SYSTEM_NAME = "checkmark"
}
