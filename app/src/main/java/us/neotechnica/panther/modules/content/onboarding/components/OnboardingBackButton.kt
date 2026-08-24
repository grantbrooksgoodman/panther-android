//
//  OnboardingBackButton.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.onboarding.constants.OnboardingBackButtonStrings

/**
 * The back affordance shown atop onboarding pages.
 *
 * @param text The button label.
 * @param isEnabled Whether the button responds to taps.
 * @param onClick The action to perform when tapped.
 * @param modifier The modifier for this button.
 */
@Composable
fun OnboardingBackButton(
    text: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPantherColors.current
    Components.Button(
        text = "${OnboardingBackButtonStrings.CHEVRON_PREFIX}$text",
        color = if (isEnabled) colors.accent else colors.disabled,
        onClick = { if (isEnabled) onClick() },
        font = Font.systemMedium(FontScale.Small),
        modifier = modifier,
    )
}
