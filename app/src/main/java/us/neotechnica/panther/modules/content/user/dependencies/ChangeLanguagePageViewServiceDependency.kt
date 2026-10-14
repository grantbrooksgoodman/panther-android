//
//  ChangeLanguagePageViewServiceDependency.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.dependencies

import us.neotechnica.panther.modules.content.user.services.ChangeLanguagePageViewService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

// MARK: - Dependency

private object ChangeLanguagePageViewServiceDependency : DependencyKey<ChangeLanguagePageViewService> {
    override fun resolve(dependencies: DependencyValues): ChangeLanguagePageViewService = ChangeLanguagePageViewService()
}

/** The service that applies the user's language selection from the language change page. */
val DependencyValues.changeLanguagePageViewService: ChangeLanguagePageViewService
    get() = this[ChangeLanguagePageViewServiceDependency]
