//
//  InviteQRCodePageViewServiceDependency.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.dependencies

import us.neotechnica.panther.modules.content.user.services.InviteQRCodePageViewService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

// MARK: - Dependency

private object InviteQRCodePageViewServiceDependency : DependencyKey<InviteQRCodePageViewService> {
    override fun resolve(dependencies: DependencyValues): InviteQRCodePageViewService = InviteQRCodePageViewService()
}

/** The service that generates the invite QR code page's content. */
val DependencyValues.inviteQRCodePageViewService: InviteQRCodePageViewService
    get() = this[InviteQRCodePageViewServiceDependency]
