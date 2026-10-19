//
//  UICacheInvalidationServiceDependency.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.dependencies

import us.neotechnica.panther.modules.content.user.services.UICacheInvalidationService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

// MARK: - Dependency

private object UICacheInvalidationServiceDependency : DependencyKey<UICacheInvalidationService> {
    override fun resolve(dependencies: DependencyValues): UICacheInvalidationService = UICacheInvalidationService()
}

/**
 * The service that invalidates display caches in response to
 * session store changes.
 */
val DependencyValues.uiCacheInvalidationService: UICacheInvalidationService
    get() = this[UICacheInvalidationServiceDependency]
