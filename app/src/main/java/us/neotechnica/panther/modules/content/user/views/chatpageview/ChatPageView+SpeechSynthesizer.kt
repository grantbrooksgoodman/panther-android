//
//  ChatPageView+SpeechSynthesizer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatpageview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import us.neotechnica.panther.designsystem.modules.componentkit.components.LocalContextMenuController
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.content.user.dependencies.contextMenuActionHandlerService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

/**
 * Dismisses any presented context menu and clears the speaking message
 * when speech synthesis finishes or is cancelled.
 */
@Composable
internal fun SpeechSynthesizerDidFinishOrCancel() {
    val controller = LocalContextMenuController.current
    LaunchedEffect(controller) {
        var wasSpeaking = false
        snapshotFlow { DependencyValues.current.commonServices.audio.textToSpeech.isSpeaking }.collect { isSpeaking ->
            if (wasSpeaking && !isSpeaking) {
                controller?.dismiss()
                DependencyValues.current.contextMenuActionHandlerService.resetSpeakingMessage()
            }
            wasSpeaking = isSpeaking
        }
    }
}
