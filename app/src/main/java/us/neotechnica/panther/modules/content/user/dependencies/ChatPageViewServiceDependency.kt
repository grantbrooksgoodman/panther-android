//
//  ChatPageViewServiceDependency.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.dependencies

import us.neotechnica.panther.modules.content.user.services.chatpageview.ChatPageViewService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

// MARK: - Dependency

private object ChatPageViewServiceDependency : DependencyKey<ChatPageViewService> {
    override fun resolve(dependencies: DependencyValues): ChatPageViewService = ChatPageViewService
}

/** The service that drives the chat page and owns its sub-services. */
val DependencyValues.chatPageViewService: ChatPageViewService
    get() = this[ChatPageViewServiceDependency]
