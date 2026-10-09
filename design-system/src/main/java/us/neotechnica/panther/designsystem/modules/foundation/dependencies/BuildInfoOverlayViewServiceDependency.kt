//
//  BuildInfoOverlayViewServiceDependency.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.dependencies

import us.neotechnica.panther.designsystem.modules.foundation.views.buildinfooverlayview.BuildInfoOverlayViewService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

private object BuildInfoOverlayViewServiceDependency : DependencyKey<BuildInfoOverlayViewService> {
    override fun resolve(dependencies: DependencyValues): BuildInfoOverlayViewService = BuildInfoOverlayViewService()
}

internal val DependencyValues.buildInfoOverlayViewService: BuildInfoOverlayViewService
    get() = this[BuildInfoOverlayViewServiceDependency]
