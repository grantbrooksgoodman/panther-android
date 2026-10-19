//
//  InviteQRCodePageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.inviteqrcodepageview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.components.HeaderView
import us.neotechnica.panther.designsystem.modules.foundation.extensions.doneButton
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.constants.InviteQRCodePageViewFloats
import us.neotechnica.panther.modules.content.user.dependencies.inviteQRCodePageViewService
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

// MARK: - Constants Accessors

private typealias Floats = InviteQRCodePageViewFloats

/**
 * The invite QR code page: a QR code others can scan to be invited to
 * the app.
 *
 * @param modifier The modifier for this view.
 */
@Composable
fun InviteQRCodePageView(modifier: Modifier = Modifier) {
    val viewModel = remember { ViewModel(InviteQRCodePageReducer.State(), InviteQRCodePageReducer()) }
    DisposableEffect(Unit) { onDispose { viewModel.close() } }
    LaunchedEffect(Unit) { viewModel.send(InviteQRCodePageReducer.Action.ViewAppeared) }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current
    val qrCode = remember { DependencyValues.current.inviteQRCodePageViewService.appShareQRCodeImage }

    Box(modifier = modifier.fillMaxSize().background(colors.groupedContentBackground)) {
        StatefulView(state = state.viewState) {
            Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                HeaderView(
                    rightItem =
                        HeaderView.PeripheralButtonType.doneButton(foregroundColor = colors.navigationBarButton) {
                            viewModel.send(InviteQRCodePageReducer.Action.DoneButtonTapped)
                        },
                    attributes = HeaderView.Attributes(sizeClass = HeaderView.SizeClass.Sheet),
                )

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Components.Text(
                        state.strings.value(InviteQRCodePageViewStrings.instructionLabelText, InviteQRCodePageViewStrings),
                        foregroundColor = colors.titleText,
                        font = Font.systemSemibold(FontScale.Large),
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    bottom = Floats.imageBottomPadding,
                                    start = Floats.imageHorizontalPadding,
                                    end = Floats.imageHorizontalPadding,
                                ),
                    )

                    qrCode?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Floats.imageHorizontalPadding),
                        )
                    }
                }
            }
        }
    }
}
