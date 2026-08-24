//
//  SettingsPageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.settingspageview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.AvatarImageView
import us.neotechnica.panther.designsystem.modules.componentkit.components.CircleChipButton
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.modules.content.user.constants.SettingsPageViewColors
import us.neotechnica.panther.modules.content.user.constants.SettingsPageViewFloats
import us.neotechnica.panther.modules.content.user.constants.SettingsPageViewStrings
import us.neotechnica.panther.modules.localization.models.LocalizationSource
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.networking.modules.session.services.UserSessionService
import us.neotechnica.panther.subsystem.modules.foundation.models.Milestone
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.BuildInfoOverlay
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

// MARK: - Constants Accessors

private typealias Floats = SettingsPageViewFloats
private typealias Colors = SettingsPageViewColors
private typealias Strings = SettingsPageViewStrings

/**
 * The settings page: the current user's contact header followed by
 * grouped action cards, mirroring the iOS `SettingsPageView`.
 *
 * **Note:** the iOS original also offers invite/review/language/theme/
 * feedback/cache/blocked-users rows; those are deferred with their
 * underlying layers. The contact header's chevron is a no-op for now.
 *
 * @param modifier The modifier for this view.
 */
@Composable
fun SettingsPageView(modifier: Modifier = Modifier) {
    val viewModel = remember { ViewModel(SettingsPageReducer.State(), SettingsPageReducer()) }
    DisposableEffect(Unit) { onDispose { viewModel.close() } }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current

    Box(modifier = modifier.fillMaxSize().background(colors.groupedContentBackground)) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .verticalScroll(rememberScrollState()),
        ) {
            Header(
                onDone = { viewModel.send(SettingsPageReducer.Action.BackTapped) },
                enabled = !state.isBusy,
            )

            ContactDetailCard()

            SettingsCard {
                SettingsIconRow(
                    symbol = "trash.fill",
                    iconColor = Colors.iconOrange,
                    title = Strings.DELETE_ACCOUNT,
                    enabled = !state.isBusy,
                    onClick = { viewModel.send(SettingsPageReducer.Action.DeleteAccountTapped) },
                )
                SettingsRowDivider()
                SettingsIconRow(
                    symbol = "hand.raised.fill",
                    iconColor = Colors.iconRed,
                    title = Strings.SIGN_OUT,
                    enabled = !state.isBusy,
                    onClick = { viewModel.send(SettingsPageReducer.Action.SignOutTapped) },
                )
            }

            // Prerelease-only affordance to restore the build-info overlay after it has been
            // long-press–dismissed (mirrors iOS Developer Mode).
            if (Build.isConfigured && Build.milestone != Milestone.GENERAL_RELEASE) {
                val isOverlayHidden by BuildInfoOverlay.isHidden.collectAsState()
                SettingsCard {
                    SettingsIconRow(
                        symbol = "gearshape.fill",
                        iconColor = Colors.iconGray,
                        title =
                            if (isOverlayHidden) {
                                Strings.SHOW_BUILD_INFO_OVERLAY
                            } else {
                                Strings.HIDE_BUILD_INFO_OVERLAY
                            },
                        enabled = !state.isBusy,
                        onClick = { if (isOverlayHidden) BuildInfoOverlay.show() else BuildInfoOverlay.hide() },
                    )
                }
            }

            if (Build.isConfigured) {
                Components.Text(
                    versionString(),
                    color = colors.subtitleText,
                    font = Font.system(FontScale.Small),
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = Floats.versionTopPadding, bottom = Floats.versionBottomPadding),
                )
            }
        }
    }
}

// MARK: - Header

@Composable
private fun Header(
    onDone: () -> Unit,
    enabled: Boolean,
) {
    val colors = LocalPantherColors.current
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Floats.headerHorizontalPadding, vertical = Floats.headerVerticalPadding),
    ) {
        Components.Text(
            LocalizedStringKey.Settings.localized(LocalizationSource.SUBSYSTEM).dropLast(1),
            color = colors.titleText,
            font = Font.systemBold(FontScale.Large),
            modifier = Modifier.align(Alignment.Center),
        )
        CircleChipButton(
            systemName = "checkmark",
            contentDescription = Strings.DONE,
            onClick = onDone,
            modifier = Modifier.align(Alignment.CenterEnd),
            tint = colors.titleText,
            glyphSize = Floats.doneButtonGlyphSize,
            enabled = enabled,
        )
    }
}

// MARK: - Contact Detail

@Composable
private fun ContactDetailCard() {
    val colors = LocalPantherColors.current
    val currentUser = UserSessionService.currentUser
    val number = currentUser?.phoneNumber?.formattedString()
    val contactName = currentUser?.id?.let { ContactService.match(it)?.fullName }
    val title = contactName ?: number ?: Strings.DEFAULT_TITLE
    val subtitle = if (contactName != null) number else null

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Floats.cardHorizontalMargin, vertical = Floats.cardVerticalMargin)
                .clip(RoundedCornerShape(Floats.contactCornerRadius))
                .background(colors.background.copy(alpha = Floats.CONTACT_BACKGROUND_ALPHA))
                .padding(Floats.cardPadding),
    ) {
        AvatarImageView(modifier = Modifier.size(Floats.avatarSize), glyphSize = Floats.avatarGlyphSize)
        Column(modifier = Modifier.weight(1f).padding(start = Floats.contactNameStartPadding)) {
            Components.Text(title, color = colors.titleText, font = Font.systemSemibold())
            subtitle?.let {
                Components.Text(it, color = colors.subtitleText, font = Font.system(FontScale.Small))
            }
        }
        if (subtitle != null) {
            Components.Symbol(
                "chevron.right",
                color = colors.subtitleText,
                modifier = Modifier.size(Floats.contactChevronSize),
            )
        }
    }
}

// MARK: - Cards

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    val colors = LocalPantherColors.current
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Floats.cardHorizontalMargin, vertical = Floats.cardVerticalMargin)
                .clip(RoundedCornerShape(Floats.cardCornerRadius))
                .background(colors.background),
    ) {
        content()
    }
}

@Composable
private fun SettingsIconRow(
    symbol: String,
    iconColor: Color,
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalPantherColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onClick() }
                .padding(horizontal = Floats.cardPadding, vertical = Floats.iconRowVerticalPadding),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(Floats.iconSize)
                    .clip(RoundedCornerShape(Floats.iconCornerRadius))
                    .background(iconColor),
        ) {
            Components.Symbol(symbol, color = Color.White, modifier = Modifier.size(Floats.iconGlyphSize))
        }
        Components.Text(title, color = colors.titleText, modifier = Modifier.padding(start = Floats.iconTitleStartPadding))
    }
}

@Composable
private fun SettingsRowDivider() {
    val colors = LocalPantherColors.current
    HorizontalDivider(
        color = colors.groupedContentBackground,
        modifier = Modifier.padding(start = Floats.cardPadding + Floats.iconSize + Floats.iconTitleStartPadding),
    )
}

// MARK: - Auxiliary

private fun versionString(): String =
    "${Strings.VERSION_PREFIX}${Build.bundleVersion} " +
        "(${Build.buildNumber}${Build.milestone.shortString}/${Build.bundleRevision.lowercase()})"
