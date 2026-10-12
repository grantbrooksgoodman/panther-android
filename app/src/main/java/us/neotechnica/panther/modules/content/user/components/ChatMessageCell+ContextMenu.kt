//
//  ChatMessageCell+ContextMenu.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAction
import us.neotechnica.panther.modules.content.user.dependencies.contextMenuActionHandlerService
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

/**
 * The context-menu actions for the given message, as assembled by the
 * context menu action handler service, or empty when the message offers
 * no menu.
 *
 * @param message The message to build the menu for.
 */
internal fun contextMenuActions(message: Message): List<ContextMenuAction> {
    val service = DependencyValues.current.contextMenuActionHandlerService
    return service.menuForMessage(message).orEmpty()
}
