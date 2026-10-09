//
//  ListRowView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.constants.ListRowViewColors
import us.neotechnica.panther.designsystem.modules.foundation.constants.ListRowViewFloats
import us.neotechnica.panther.designsystem.modules.foundation.constants.ListRowViewStrings
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import java.util.UUID

// MARK: - Constants Accessors

private typealias ListRowColors = ListRowViewColors
private typealias ListRowFloats = ListRowViewFloats
private typealias ListRowStrings = ListRowViewStrings

/**
 * A single row for a grouped settings-style list.
 *
 * Create a [Configuration] describing the row's interaction, text,
 * and decorations, then render it:
 *
 * ```kotlin
 * ListRowView(
 *     ListRowView.Configuration(
 *         ListRowView.Interaction.Button(showsChevron = true) { open() },
 *         innerText = "Change language",
 *     ),
 * )
 * ```
 *
 * Group related rows with
 * [GroupedListView][us.neotechnica.panther.designsystem.modules.foundation.components.GroupedListView].
 */
object ListRowView {
    // MARK: - Types

    /** How a list row responds to user interaction. */
    sealed interface Interaction {
        /**
         * The row acts as a button, optionally trailing a chevron.
         */
        data class Button(
            val id: UUID = UUID.randomUUID(),
            val showsChevron: Boolean = false,
            val action: () -> Unit,
        ) : Interaction

        /**
         * The row navigates to another page, trailing a chevron.
         */
        data class Destination(
            val id: UUID = UUID.randomUUID(),
            val action: () -> Unit,
        ) : Interaction

        /** The row hosts a toggle switch. */
        data class Switch(
            val id: UUID = UUID.randomUUID(),
            val isToggled: Boolean,
            val onToggle: (Boolean) -> Unit,
        ) : Interaction
    }

    /**
     * The content and behavior of a list row.
     */
    class Configuration(
        /** How the row responds to user interaction. */
        val interaction: Interaction,
        /** An uppercased caption above the row, or `null`. */
        val headerText: String? = null,
        /** The row's title text. */
        val innerText: String,
        /** A caption below the row, or `null`. */
        val footerText: String? = null,
        /**
         * The title's color, or `null` for the theme's title color.
         */
        val innerTextColor: Color? = null,
        /** Whether the row responds to interaction. */
        val isEnabled: Boolean = true,
        /** The row's corner radius. */
        val cornerRadius: Float = CORNER_RADIUS,
        /** A leading image for the row, or `null`. */
        val imageView: (@Composable () -> Unit)? = null,
    ) {
        internal fun strippingMetadata(): Configuration =
            Configuration(
                interaction = interaction,
                innerText = innerText,
                innerTextColor = innerTextColor,
                isEnabled = isEnabled,
                cornerRadius = cornerRadius,
                imageView = imageView,
            )
    }

    // MARK: - Body

    @Composable
    operator fun invoke(configuration: Configuration) {
        if (configuration.headerText != null || configuration.footerText != null) {
            Column {
                configuration.headerText?.let { headerText ->
                    Components.Text(
                        headerText.uppercase(),
                        foregroundColor = LocalPantherColors.current.subtitleText,
                        font = Font.system(FontScale.Custom(ListRowFloats.HEADER_LABEL_SYSTEM_FONT_SCALE)),
                        modifier = Modifier.padding(horizontal = ListRowFloats.HEADER_LABEL_HORIZONTAL_PADDING.dp),
                    )
                }

                ContentView(configuration)

                configuration.footerText?.let { footerText ->
                    Components.Text(
                        footerText,
                        foregroundColor = LocalPantherColors.current.subtitleText,
                        font = Font.system(FontScale.Custom(ListRowFloats.FOOTER_LABEL_SYSTEM_FONT_SCALE)),
                        modifier =
                            Modifier
                                .padding(horizontal = ListRowFloats.FOOTER_LABEL_HORIZONTAL_PADDING.dp)
                                .padding(top = 1.dp),
                    )
                }
            }
        } else {
            ContentView(configuration)
        }
    }
}

private const val CORNER_RADIUS = 10f

// MARK: - Content

@Composable
private fun ContentView(configuration: ListRowView.Configuration) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDark = isSystemInDarkTheme()

    val background =
        when (configuration.interaction) {
            is ListRowView.Interaction.Switch ->
                if (isDark) ListRowColors.DARK_BACKGROUND else ListRowColors.LIGHT_BACKGROUND

            else ->
                when {
                    isPressed && isDark -> ListRowColors.BUTTON_STYLE_DARK_PRESSED_BACKGROUND
                    isPressed -> ListRowColors.BUTTON_STYLE_LIGHT_PRESSED_BACKGROUND
                    isDark -> ListRowColors.BUTTON_STYLE_DARK_NOT_PRESSED_BACKGROUND
                    else -> ListRowColors.BUTTON_STYLE_LIGHT_NOT_PRESSED_BACKGROUND
                }
        }

    val action =
        when (val interaction = configuration.interaction) {
            is ListRowView.Interaction.Button -> interaction.action
            is ListRowView.Interaction.Destination -> interaction.action
            is ListRowView.Interaction.Switch -> null
        }

    LabelView(
        configuration = configuration,
        modifier =
            Modifier
                .clip(RoundedCornerShape(configuration.cornerRadius.dp))
                .background(background)
                .then(
                    if (action != null) {
                        Modifier.clickable(
                            enabled = configuration.isEnabled,
                            indication = null,
                            interactionSource = interactionSource,
                            onClick = action,
                        )
                    } else {
                        Modifier
                    },
                ),
    )
}

// MARK: - Label

@Composable
private fun LabelView(
    configuration: ListRowView.Configuration,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPantherColors.current
    val showsChevron =
        when (val interaction = configuration.interaction) {
            is ListRowView.Interaction.Button -> interaction.showsChevron
            is ListRowView.Interaction.Destination -> true
            is ListRowView.Interaction.Switch -> false
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = ListRowFloats.FRAME_MIN_HEIGHT.dp)
                .padding(horizontal = ListRowFloats.HEADER_LABEL_HORIZONTAL_PADDING.dp)
                .padding(vertical = ListRowFloats.VERTICAL_PADDING.dp),
    ) {
        configuration.imageView?.let { imageView ->
            Box(
                modifier =
                    Modifier
                        .padding(start = ListRowFloats.IMAGE_LEADING_PADDING.dp)
                        .size(ListRowFloats.IMAGE_FRAME_WIDTH.dp, ListRowFloats.IMAGE_FRAME_HEIGHT.dp),
            ) {
                imageView()
            }
        }

        Components.Text(
            configuration.innerText,
            foregroundColor =
                if (configuration.isEnabled) {
                    configuration.innerTextColor ?: colors.titleText
                } else {
                    ListRowColors.TITLE_LABEL_DISABLED_FOREGROUND
                },
            modifier =
                Modifier.padding(
                    start = if (configuration.imageView == null) 0.dp else ListRowFloats.TITLE_LABEL_LEADING_PADDING.dp,
                ),
        )

        Spacer(modifier = Modifier.weight(1f))

        when (val interaction = configuration.interaction) {
            is ListRowView.Interaction.Switch ->
                Switch(
                    checked = interaction.isToggled,
                    enabled = configuration.isEnabled,
                    onCheckedChange = interaction.onToggle,
                )

            else ->
                if (showsChevron) {
                    Components.Symbol(
                        ListRowStrings.CHEVRON_IMAGE_SYSTEM_NAME,
                        foregroundColor = if (configuration.isEnabled) colors.subtitleText else colors.disabled,
                        usesIntrinsicSize = false,
                        modifier =
                            Modifier.sizeIn(
                                maxWidth = ListRowFloats.CHEVRON_IMAGE_FRAME_MAX_WIDTH.dp,
                                maxHeight = ListRowFloats.CHEVRON_IMAGE_FRAME_MAX_HEIGHT.dp,
                            ),
                    )
                }
        }
    }
}
