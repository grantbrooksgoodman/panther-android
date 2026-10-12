//
//  ConversationsPageViewServiceDependency.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.dependencies

import us.neotechnica.panther.modules.content.user.services.ConversationsPageViewService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

// MARK: - Dependency

private object ConversationsPageViewServiceDependency : DependencyKey<ConversationsPageViewService> {
    override fun resolve(dependencies: DependencyValues): ConversationsPageViewService = ConversationsPageViewService()
}

/** The service that drives the conversations page's lifecycle, prompts, and data reloads. */
val DependencyValues.conversationsPageViewService: ConversationsPageViewService
    get() = this[ConversationsPageViewServiceDependency]
