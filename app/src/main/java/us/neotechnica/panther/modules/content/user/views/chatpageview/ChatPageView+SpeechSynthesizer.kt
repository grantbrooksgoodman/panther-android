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
import us.neotechnica.panther.modules.common.services.TextToSpeechService
import us.neotechnica.panther.modules.content.user.services.ContextMenuActionHandlerService

@Composable
internal fun SpeechSynthesizerDidFinishOrCancel() {
    val controller = LocalContextMenuController.current
    LaunchedEffect(controller) {
        var wasSpeaking = false
        snapshotFlow { TextToSpeechService.isSpeaking }.collect { isSpeaking ->
            if (wasSpeaking && !isSpeaking) {
                controller?.dismiss()
                ContextMenuActionHandlerService.resetSpeakingMessage()
            }
            wasSpeaking = isSpeaking
        }
    }
}
