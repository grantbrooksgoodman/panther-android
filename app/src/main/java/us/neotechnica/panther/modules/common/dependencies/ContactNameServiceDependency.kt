//
//  ContactNameServiceDependency.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.dependencies

import us.neotechnica.panther.modules.common.contacts.services.ContactNameService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

// MARK: - Dependency

private object ContactNameServiceDependency : DependencyKey<ContactNameService> {
    override fun resolve(dependencies: DependencyValues): ContactNameService = ContactNameService
}

/** The service that derives display names from device contacts. */
internal val DependencyValues.contactNameService: ContactNameService
    get() = this[ContactNameServiceDependency]
