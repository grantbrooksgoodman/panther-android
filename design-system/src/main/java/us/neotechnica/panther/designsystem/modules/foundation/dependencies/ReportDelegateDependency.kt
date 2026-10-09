//
//  ReportDelegateDependency.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.dependencies

import us.neotechnica.panther.designsystem.modules.foundation.services.ReportDelegate
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

private object ReportDelegateDependency : DependencyKey<ReportDelegate> {
    override fun resolve(dependencies: DependencyValues): ReportDelegate = ReportDelegate
}

/** The shared [ReportDelegate] instance. */
val DependencyValues.reportDelegate: ReportDelegate
    get() = this[ReportDelegateDependency]
