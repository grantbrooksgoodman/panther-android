//
//  SettingsPageViewServiceDependency.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.dependencies

import us.neotechnica.panther.modules.content.user.services.SettingsPageViewService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

// MARK: - Dependency

// Internal so the reducer tests can scope in a service with injected
// collaborators.
internal object SettingsPageViewServiceDependency : DependencyKey<SettingsPageViewService> {
    override fun resolve(dependencies: DependencyValues): SettingsPageViewService = SettingsPageViewService()
}

/** The service that handles the settings page's user interactions. */
val DependencyValues.settingsPageViewService: SettingsPageViewService
    get() = this[SettingsPageViewServiceDependency]
