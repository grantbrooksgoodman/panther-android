//
//  GalleryView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.demo.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ConfirmationAlert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ProgressAlert
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextInputAlert
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.models.ColoredItemType
import us.neotechnica.panther.designsystem.modules.theming.models.ThemeStyle
import us.neotechnica.panther.designsystem.modules.theming.models.Themes
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.localization.models.LocalizationSource
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.localization.services.LocalizedStringResolver
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.RootRoute
import us.neotechnica.panther.navigation.RootView
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

/**
 * A showcase of the design system: theming, typography, colors,
 * components, alerts, localization, and navigation transitions.
 *
 * [GalleryView] is the Phase 3 acceptance harness. Switching the theme
 * or appearance at the top recomposes every section, demonstrating
 * day/night parity with the iOS app.
 *
 * @param modifier The modifier for this view.
 */
@Composable
fun GalleryView(modifier: Modifier = Modifier) {
    val colors = LocalPantherColors.current

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.background)
                .verticalScroll(rememberScrollState())
                .padding(SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
    ) {
        Header()
        AppearanceSection()
        TypographySection()
        ColorsSection()
        SymbolsSection()
        ButtonsSection()
        AlertsSection()
        LocalizationSection()
        NavigationSection()
    }
}

// MARK: - Header

@Composable
private fun Header() {
    val colors = LocalPantherColors.current

    Column(verticalArrangement = Arrangement.spacedBy(ITEM_SPACING)) {
        Components.Text(
            "Design System",
            color = colors.titleText,
            font = Font.systemBold(FontScale.Large),
        )
        Components.Text(
            "Panther for Android — Phase 3 gallery",
            color = colors.subtitleText,
        )
    }
}

// MARK: - Appearance

@Composable
private fun AppearanceSection() {
    val activeTheme by ThemeService.currentTheme.collectAsState()
    val activeOverride by ThemeService.styleOverride.collectAsState()

    Section("Appearance") {
        WrappingRow {
            Themes.all.forEach { theme ->
                Chip(
                    label = theme.name,
                    isSelected = theme.name == activeTheme.name,
                    onClick = { ThemeService.setTheme(theme) },
                )
            }
        }

        WrappingRow {
            APPEARANCE_OPTIONS.forEach { (label, style) ->
                Chip(
                    label = label,
                    isSelected = activeOverride == style,
                    onClick = { ThemeService.setStyleOverride(style) },
                )
            }
        }
    }
}

// MARK: - Typography

@Composable
private fun TypographySection() {
    val colors = LocalPantherColors.current

    Section("Typography") {
        Components.Text("Large title", color = colors.titleText, font = Font.systemBold(FontScale.Large))
        Components.Text("Semibold body", color = colors.titleText, font = Font.systemSemibold())
        Components.Text("Medium body", color = colors.titleText, font = Font.systemMedium())
        Components.Text("Regular body", color = colors.titleText, font = Font.system)
        Components.Text("Light body", color = colors.titleText, font = Font.systemLight())
        Components.Text("Italic body", color = colors.titleText, font = Font.systemItalic())
        Components.Text("Underlined body", color = colors.titleText, font = Font.systemUnderlined())
        Components.Text("Small caption", color = colors.subtitleText, font = Font.system(FontScale.Small))
    }
}

// MARK: - Colors

@Composable
private fun ColorsSection() {
    Section("Colors") {
        WrappingRow {
            COLORED_ITEM_TYPES.forEach { (label, type) ->
                Swatch(label = label, type = type)
            }
        }
    }
}

@Composable
private fun Swatch(
    label: String,
    type: ColoredItemType,
) {
    val colors = LocalPantherColors.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SWATCH_LABEL_SPACING),
    ) {
        Box(
            modifier =
                Modifier
                    .size(SWATCH_SIZE)
                    .clip(RoundedCornerShape(CORNER_RADIUS))
                    .background(colors.color(type))
                    .border(HAIRLINE, colors.disabled, RoundedCornerShape(CORNER_RADIUS)),
        )
        Components.Text(label, color = colors.subtitleText, font = Font.system(FontScale.Small))
    }
}

// MARK: - Symbols

@Composable
private fun SymbolsSection() {
    val colors = LocalPantherColors.current

    Section("Symbols") {
        WrappingRow {
            SYMBOL_NAMES.forEach { name ->
                Components.Symbol(name, color = colors.accent, modifier = Modifier.size(SYMBOL_SIZE))
            }
        }
    }
}

// MARK: - Buttons

@Composable
private fun ButtonsSection() {
    val colors = LocalPantherColors.current
    var tapCount by remember { mutableIntStateOf(0) }

    Section("Buttons") {
        Row(
            horizontalArrangement = Arrangement.spacedBy(ITEM_SPACING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Components.Button(
                text = "Tap me",
                color = colors.accent,
                onClick = { tapCount += 1 },
                font = Font.systemSemibold(),
            )
            Components.Button(
                symbolName = "heart",
                color = colors.accent,
                onClick = { tapCount += 1 },
                modifier = Modifier.size(SYMBOL_SIZE),
            )
            Components.Text("Tapped $tapCount×", color = colors.subtitleText)
        }
    }
}

// MARK: - Alerts

@Composable
private fun AlertsSection() {
    val colors = LocalPantherColors.current
    val scope = rememberCoroutineScope()
    var lastResult by remember { mutableStateOf("—") }

    Section("Alerts") {
        WrappingRow {
            Chip(label = "Standard", isSelected = false) {
                scope.launch {
                    Alert(
                        title = "Standard alert",
                        message = "A title, a message, and a single action.",
                        actions = listOf(Action("OK", style = ActionStyle.CANCEL) {}),
                    ).present()
                    lastResult = "Standard dismissed"
                }
            }
            Chip(label = "Confirmation", isSelected = false) {
                scope.launch {
                    val confirmed =
                        ConfirmationAlert(
                            title = "Delete item?",
                            message = "This cannot be undone.",
                            confirmButtonTitle = "Delete",
                            confirmButtonStyle = ActionStyle.DESTRUCTIVE_PREFERRED,
                        ).present()
                    lastResult = "Confirmation → $confirmed"
                }
            }
            Chip(label = "Text input", isSelected = false) {
                scope.launch {
                    val text =
                        TextInputAlert(
                            title = "Your name",
                            message = "Enter a display name.",
                            placeholder = "Name",
                        ).present()
                    lastResult = "Text input → ${text ?: "cancelled"}"
                }
            }
            Chip(label = "Progress", isSelected = false) {
                val progress = ProgressAlert(message = "Working…")
                progress.present()
                scope.launch {
                    delay(PROGRESS_DEMO_MILLIS)
                    progress.dismiss()
                    lastResult = "Progress finished"
                }
            }
        }

        Components.Text(
            "Last result: $lastResult",
            color = colors.subtitleText,
            font = Font.system(FontScale.Small),
        )
    }
}

// MARK: - Localization

@Composable
private fun LocalizationSection() {
    val colors = LocalPantherColors.current
    val samples =
        listOf(
            "delete" to LocalizedStringKey.Delete.localized(),
            "copy" to LocalizedStringKey.Copy.localized(),
            "block_user" to LocalizedStringKey.BlockUser.localized(),
            "settings (subsystem)" to LocalizedStringKey.Settings.localized(LocalizationSource.SUBSYSTEM),
        )

    Section("Localization (${LocalizedStringResolver.languageCode})") {
        samples.forEach { (label, value) ->
            Row(horizontalArrangement = Arrangement.spacedBy(ITEM_SPACING)) {
                Components.Text("$label:", color = colors.subtitleText, font = Font.system(FontScale.Small))
                Components.Text(value, color = colors.titleText, font = Font.system(FontScale.Small))
            }
        }
    }
}

// MARK: - Navigation

@Composable
private fun NavigationSection() {
    val colors = LocalPantherColors.current
    val navigation = remember { DependencyValues.current.navigation }

    Section("Navigation transitions") {
        WrappingRow {
            NAVIGATION_DESTINATIONS.forEach { (label, path) ->
                Chip(label = label, isSelected = false) {
                    navigation.navigate(Route.Root(RootRoute.SetModal(path)))
                }
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(NAVIGATION_PREVIEW_HEIGHT)
                    .clip(RoundedCornerShape(CORNER_RADIUS))
                    .border(HAIRLINE, colors.disabled, RoundedCornerShape(CORNER_RADIUS)),
        ) {
            RootView()
        }
    }
}

// MARK: - Building Blocks

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    val colors = LocalPantherColors.current

    Column(verticalArrangement = Arrangement.spacedBy(ITEM_SPACING)) {
        Components.Text(title, color = colors.accent, font = Font.systemSemibold())
        content()
    }
}

@Composable
private fun Chip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalPantherColors.current
    val fill = if (isSelected) colors.accent else colors.groupedContentBackground
    val text = if (isSelected) colors.background else colors.titleText

    Components.Button(
        text = label,
        color = text,
        onClick = onClick,
        font = Font.systemMedium(FontScale.Small),
        modifier =
            Modifier
                .clip(RoundedCornerShape(CORNER_RADIUS))
                .background(fill)
                .border(HAIRLINE, colors.disabled, RoundedCornerShape(CORNER_RADIUS))
                .padding(horizontal = CHIP_PADDING_HORIZONTAL, vertical = CHIP_PADDING_VERTICAL),
    )
}

@Composable
private fun WrappingRow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(ITEM_SPACING),
        verticalArrangement = Arrangement.spacedBy(ITEM_SPACING),
        modifier = Modifier.fillMaxWidth(),
    ) {
        content()
    }
}

// MARK: - Constants

private val APPEARANCE_OPTIONS: List<Pair<String, ThemeStyle?>> =
    listOf(
        "System" to null,
        "Light" to ThemeStyle.LIGHT,
        "Dark" to ThemeStyle.DARK,
    )

private val COLORED_ITEM_TYPES: List<Pair<String, ColoredItemType>> =
    listOf(
        "accent" to ColoredItemType.accent,
        "background" to ColoredItemType.background,
        "titleText" to ColoredItemType.titleText,
        "subtitleText" to ColoredItemType.subtitleText,
        "disabled" to ColoredItemType.disabled,
        "sender" to ColoredItemType.senderBubble,
        "receiver" to ColoredItemType.receiverBubble,
        "grouped" to ColoredItemType.groupedContentBackground,
        "navBar" to ColoredItemType.navigationBarBackground,
    )

private val SYMBOL_NAMES: List<String> =
    listOf(
        "plus",
        "checkmark",
        "xmark",
        "trash",
        "heart",
        "star",
        "gear",
        "paperplane",
    )

private val NAVIGATION_DESTINATIONS: List<Pair<String, RootNavigatorState.ModalPath>> =
    listOf(
        "Splash" to RootNavigatorState.ModalPath.Splash,
        "Onboarding" to RootNavigatorState.ModalPath.Onboarding,
        "User Content" to RootNavigatorState.ModalPath.UserContent,
    )

private const val PROGRESS_DEMO_MILLIS = 1500L

private val SCREEN_PADDING = 20.dp
private val SECTION_SPACING = 28.dp
private val ITEM_SPACING = 8.dp
private val SWATCH_SIZE = 56.dp
private val SWATCH_LABEL_SPACING = 4.dp
private val SYMBOL_SIZE = 28.dp
private val CORNER_RADIUS = 10.dp
private val HAIRLINE = 1.dp
private val CHIP_PADDING_HORIZONTAL = 14.dp
private val CHIP_PADDING_VERTICAL = 8.dp
private val NAVIGATION_PREVIEW_HEIGHT = 220.dp
