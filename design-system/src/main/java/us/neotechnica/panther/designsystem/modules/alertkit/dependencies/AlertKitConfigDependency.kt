//
//  AlertKitConfigDependency.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.dependencies

import us.neotechnica.panther.designsystem.modules.alertkit.AlertKitConfig
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

private object AlertKitConfigDependency : DependencyKey<AlertKitConfig> {
    override fun resolve(dependencies: DependencyValues): AlertKitConfig = AlertKitConfig
}

/** The shared AlertKit configuration. */
val DependencyValues.alertKitConfig: AlertKitConfig
    get() = this[AlertKitConfigDependency]
