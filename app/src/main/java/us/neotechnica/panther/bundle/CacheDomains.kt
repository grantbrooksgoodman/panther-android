//
//  CacheDomains.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 25/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle

import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.CacheDomainListDelegate
import us.neotechnica.panther.subsystem.modules.foundation.models.CacheDomain

/**
 * The delegate that supplies the app's cache domains to the
 * subsystem.
 *
 * Additional domains are registered as their backing caches land in
 * later phases; the caches without a counterpart yet are tracked in
 * the parity progress record rather than stubbed.
 */
object CacheDomainList : CacheDomainListDelegate {
    override val appCacheDomains: List<CacheDomain> =
        listOf(
            CacheDomain.Networking.database,
        )
}
