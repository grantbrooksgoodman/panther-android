//
//  SplashPageViewServiceDependency.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 27/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.dependencies

import us.neotechnica.panther.modules.content.shared.services.SplashPageViewService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

// MARK: - Dependency

private object SplashPageViewServiceDependency : DependencyKey<SplashPageViewService> {
    override fun resolve(dependencies: DependencyValues): SplashPageViewService = SplashPageViewService
}

/** The service that initializes the app's data bundle behind the splash page. */
val DependencyValues.splashPageViewService: SplashPageViewService
    get() = this[SplashPageViewServiceDependency]
