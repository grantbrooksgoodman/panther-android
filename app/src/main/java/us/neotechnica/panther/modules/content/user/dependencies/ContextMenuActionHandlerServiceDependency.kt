//
//  ContextMenuActionHandlerServiceDependency.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.dependencies

import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.contextmenu.sources.ContextMenuActionHandlerService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

// MARK: - Dependency

private object ContextMenuActionHandlerServiceDependency : DependencyKey<ContextMenuActionHandlerService> {
    override fun resolve(dependencies: DependencyValues): ContextMenuActionHandlerService = ContextMenuActionHandlerService()
}

/** The service that builds message context menus and handles their actions. */
val DependencyValues.contextMenuActionHandlerService: ContextMenuActionHandlerService
    get() = this[ContextMenuActionHandlerServiceDependency]
