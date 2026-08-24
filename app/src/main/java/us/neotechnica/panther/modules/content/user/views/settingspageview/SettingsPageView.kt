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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.modules.localization.models.LocalizationSource
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.networking.modules.session.services.UserSessionService
import us.neotechnica.panther.subsystem.modules.foundation.models.Milestone
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.BuildInfoOverlay
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

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
                    iconColor = ICON_ORANGE,
                    title = "Delete account",
                    enabled = !state.isBusy,
                    onClick = { viewModel.send(SettingsPageReducer.Action.DeleteAccountTapped) },
                )
                SettingsRowDivider()
                SettingsIconRow(
                    symbol = "hand.raised.fill",
                    iconColor = ICON_RED,
                    title = "Sign out",
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
                        iconColor = ICON_GRAY,
                        title = if (isOverlayHidden) "Show Build Info Overlay" else "Hide Build Info Overlay",
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
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 24.dp),
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
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Components.Text(
            LocalizedStringKey.Settings.localized(LocalizationSource.SUBSYSTEM).dropLast(1),
            color = colors.titleText,
            font = Font.systemBold(FontScale.Large),
            modifier = Modifier.align(Alignment.Center),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .size(40.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(colors.background)
                    .clickable(enabled = enabled, onClick = onDone),
        ) {
            Components.Symbol("checkmark", color = colors.titleText, modifier = Modifier.size(20.dp))
        }
    }
}

// MARK: - Contact Detail

@Composable
private fun ContactDetailCard() {
    val colors = LocalPantherColors.current
    val currentUser = UserSessionService.currentUser
    val number = currentUser?.phoneNumber?.formattedString()
    val contactName = currentUser?.id?.let { ContactService.match(it)?.fullName }
    val title = contactName ?: number ?: "You"
    val subtitle = if (contactName != null) number else null

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = CARD_MARGIN, vertical = 8.dp)
                .clip(RoundedCornerShape(CONTACT_CORNER))
                .background(colors.background.copy(alpha = CONTACT_BACKGROUND_ALPHA))
                .padding(CARD_PADDING),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(AVATAR_SIZE).clip(CircleShape).background(AVATAR_BACKGROUND),
        ) {
            Components.Symbol("person", color = colors.background, modifier = Modifier.size(AVATAR_GLYPH_SIZE))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Components.Text(title, color = colors.titleText, font = Font.systemSemibold())
            subtitle?.let {
                Components.Text(it, color = colors.subtitleText, font = Font.system(FontScale.Small))
            }
        }
        if (subtitle != null) {
            Components.Symbol("chevron.right", color = colors.subtitleText, modifier = Modifier.size(18.dp))
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
                .padding(horizontal = CARD_MARGIN, vertical = 8.dp)
                .clip(RoundedCornerShape(CARD_CORNER))
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
                .padding(horizontal = CARD_PADDING, vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(ICON_SIZE).clip(RoundedCornerShape(ICON_CORNER)).background(iconColor),
        ) {
            Components.Symbol(symbol, color = Color.White, modifier = Modifier.size(ICON_GLYPH_SIZE))
        }
        Components.Text(title, color = colors.titleText, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun SettingsRowDivider() {
    val colors = LocalPantherColors.current
    HorizontalDivider(
        color = colors.groupedContentBackground,
        modifier = Modifier.padding(start = CARD_PADDING + ICON_SIZE + 12.dp),
    )
}

// MARK: - Auxiliary

private fun versionString(): String =
    "Version ${Build.bundleVersion} " +
        "(${Build.buildNumber}${Build.milestone.shortString}/${Build.bundleRevision.lowercase()})"

private val CARD_MARGIN = 16.dp
private val CARD_CORNER = 16.dp
private val CARD_PADDING = 16.dp
private val CONTACT_CORNER = 28.dp
private const val CONTACT_BACKGROUND_ALPHA = 0.55f
private val AVATAR_SIZE = 44.dp
private val AVATAR_GLYPH_SIZE = 24.dp
private val ICON_SIZE = 30.dp
private val ICON_CORNER = 7.dp
private val ICON_GLYPH_SIZE = 17.dp
private val AVATAR_BACKGROUND = Color(0xFFC7C7CC)
private val ICON_ORANGE = Color(0xFFFF9500)
private val ICON_RED = Color(0xFFFF3B30)
private val ICON_GRAY = Color(0xFF8E8E93)
