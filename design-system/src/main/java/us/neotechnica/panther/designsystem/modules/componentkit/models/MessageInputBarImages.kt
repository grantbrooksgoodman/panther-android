//
//  MessageInputBarImages.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.models

import androidx.compose.ui.graphics.painter.Painter

/**
 * The images a message input bar's attach and send buttons display,
 * bundling each button's image with the image it shows while pressed.
 *
 * Pass a value to the input bar to replace the buttons' default
 * symbols. Each image is drawn untinted at the button's full size, and
 * drawn lighter while its button is disabled.
 *
 * @param attachMediaButton The attach button's image.
 * @param attachMediaButtonHighlighted The image the attach button shows
 *   while pressed.
 * @param sendButton The send button's image.
 * @param sendButtonHighlighted The image the send button shows while
 *   pressed.
 */
class MessageInputBarImages(
    val attachMediaButton: Painter,
    val attachMediaButtonHighlighted: Painter,
    val sendButton: Painter,
    val sendButtonHighlighted: Painter,
)
