//
//  OnboardingServiceDependency.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.dependencies

import us.neotechnica.panther.modules.content.onboarding.services.OnboardingService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

// MARK: - Dependency

private object OnboardingServiceDependency : DependencyKey<OnboardingService> {
    override fun resolve(dependencies: DependencyValues): OnboardingService = OnboardingService()
}

/** The service that carries state through the onboarding flow. */
val DependencyValues.onboardingService: OnboardingService
    get() = this[OnboardingServiceDependency]
