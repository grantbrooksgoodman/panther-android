//
//  SplashView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.RootRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

/**
 * The launch splash. Routes to the signed-in content flow when a user
 * is persisted, or to onboarding otherwise.
 *
 * **Note:** the full iOS splash resolves the session, caches, and
 * metadata; that initialization lands with the session layer in a
 * later phase. This is the routing-only variant.
 *
 * @param modifier The modifier for this view.
 */
@Composable
fun SplashView(modifier: Modifier = Modifier) {
    val colors = LocalPantherColors.current
    val navigation = remember { DependencyValues.current.navigation }

    LaunchedEffect(Unit) {
        delay(SPLASH_DELAY_MILLIS)
        val destination =
            if (Persistent.string(PersistentStorageKey.currentUserID) != null) {
                RootNavigatorState.ModalPath.UserContent
            } else {
                RootNavigatorState.ModalPath.Onboarding
            }
        navigation.navigate(Route.Root(RootRoute.SetModal(destination)))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.background),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Components.Symbol("message", color = colors.accent, modifier = Modifier.size(96.dp))
        CircularProgressIndicator(color = colors.accent)
    }
}

private const val SPLASH_DELAY_MILLIS = 400L
