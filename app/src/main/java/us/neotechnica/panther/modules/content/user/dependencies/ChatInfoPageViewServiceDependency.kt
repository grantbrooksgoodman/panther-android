//
//  ChatInfoPageViewServiceDependency.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.dependencies

import us.neotechnica.panther.modules.content.user.services.chatinfopageview.ChatInfoPageViewService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

// MARK: - Dependency

private object ChatInfoPageViewServiceDependency : DependencyKey<ChatInfoPageViewService> {
    override fun resolve(dependencies: DependencyValues): ChatInfoPageViewService = ChatInfoPageViewService()
}

/** The service that handles the chat info page's user interactions. */
val DependencyValues.chatInfoPageViewService: ChatInfoPageViewService
    get() = this[ChatInfoPageViewServiceDependency]
