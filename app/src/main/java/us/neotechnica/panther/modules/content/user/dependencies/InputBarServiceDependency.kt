//
//  InputBarServiceDependency.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.dependencies

import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.inputbar.InputBarService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

// MARK: - Dependency

private object InputBarServiceDependency : DependencyKey<InputBarService> {
    override fun resolve(dependencies: DependencyValues): InputBarService = InputBarService()
}

/** The service that manages the chat page's message input bar. */
val DependencyValues.inputBarService: InputBarService
    get() = this[InputBarServiceDependency]
