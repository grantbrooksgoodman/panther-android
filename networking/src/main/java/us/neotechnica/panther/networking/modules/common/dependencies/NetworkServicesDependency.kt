//
//  NetworkServicesDependency.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.dependencies

import us.neotechnica.panther.networking.modules.common.models.NetworkServices
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

/** The dependency key for resolving the current [NetworkServices] instance. */
private object NetworkServicesDependency : DependencyKey<NetworkServices> {
    override fun resolve(dependencies: DependencyValues): NetworkServices = NetworkServices
}

/**
 * The current networking services, accessible through the
 * dependency injection system.
 *
 * ```kotlin
 * private val networking: NetworkServices by Dependency { it.networking }
 * ```
 */
val DependencyValues.networking: NetworkServices
    get() = this[NetworkServicesDependency]
