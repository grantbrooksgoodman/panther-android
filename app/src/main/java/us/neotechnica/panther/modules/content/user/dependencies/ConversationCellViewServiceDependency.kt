//
//  ConversationCellViewServiceDependency.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.dependencies

import us.neotechnica.panther.modules.content.user.services.ConversationCellViewService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

// MARK: - Dependency

private object ConversationCellViewServiceDependency : DependencyKey<ConversationCellViewService> {
    override fun resolve(dependencies: DependencyValues): ConversationCellViewService = ConversationCellViewService()
}

/**
 * The service that handles conversation cell interactions
 * requiring presentation or session work.
 */
val DependencyValues.conversationCellViewService: ConversationCellViewService
    get() = this[ConversationCellViewServiceDependency]
