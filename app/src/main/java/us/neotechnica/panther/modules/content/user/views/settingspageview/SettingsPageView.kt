//
//  SettingsPageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.settingspageview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.localization.models.LocalizationSource
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.foundation.models.Milestone
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.BuildInfoOverlay
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

/**
 * The settings page: sign out and delete account.
 *
 * @param modifier The modifier for this view.
 */
@Composable
fun SettingsPageView(modifier: Modifier = Modifier) {
    val viewModel = remember { ViewModel(SettingsPageReducer.State(), SettingsPageReducer()) }
    DisposableEffect(Unit) { onDispose { viewModel.close() } }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
        ) {
            IconButton(
                onClick = { viewModel.send(SettingsPageReducer.Action.BackTapped) },
                enabled = !state.isBusy,
            ) {
                Components.Symbol("chevron.left", color = colors.accent, modifier = Modifier.size(24.dp))
            }
            Components.Text(
                LocalizedStringKey.Settings.localized(LocalizationSource.SUBSYSTEM),
                color = colors.titleText,
                font = Font.systemBold(FontScale.Large),
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        SettingsRow("Sign Out", colors.titleText, enabled = !state.isBusy) {
            viewModel.send(SettingsPageReducer.Action.SignOutTapped)
        }
        HorizontalDivider(color = colors.groupedContentBackground)
        SettingsRow("Delete Account", DESTRUCTIVE_COLOR, enabled = !state.isBusy) {
            viewModel.send(SettingsPageReducer.Action.DeleteAccountTapped)
        }

        // Prerelease-only affordance to restore the build-info overlay after
        // it has been long-press–dismissed (mirrors iOS Developer Mode).
        if (Build.isConfigured && Build.milestone != Milestone.GENERAL_RELEASE) {
            val isOverlayHidden by BuildInfoOverlay.isHidden.collectAsState()
            HorizontalDivider(color = colors.groupedContentBackground)
            SettingsRow(
                if (isOverlayHidden) "Show Build Info Overlay" else "Hide Build Info Overlay",
                colors.titleText,
                enabled = !state.isBusy,
            ) {
                if (isOverlayHidden) BuildInfoOverlay.show() else BuildInfoOverlay.hide()
            }
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onClick() }
                .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Components.Text(title, color = color)
    }
}

private val DESTRUCTIVE_COLOR = Color(0xFFFF3B30)
