//
//  LoggerDomainSubscriptionDelegate.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.interfaces

import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain

/**
 * A delegate that specifies which logger domains the app subscribes
 * to and which domains are excluded from the session record.
 *
 * Register an implementation once at launch to control the initial
 * logging configuration. The subsystem reads the delegate's values
 * during setup and subscribes to the returned domains automatically.
 * When no delegate is registered, output for every domain is
 * produced.
 */
interface LoggerDomainSubscriptionDelegate {
    // MARK: - Properties

    /**
     * The domains whose output is omitted from the on-disk session
     * record.
     *
     * Messages logged to these domains still appear in the console,
     * but are not written to the session record file.
     */
    val domainsExcludedFromSessionRecord: List<LoggerDomain>

    /**
     * The domains the logger subscribes to at launch.
     *
     * Only messages logged to a subscribed domain produce output.
     * Domains not in this list are silently ignored unless
     * subscribed to later at runtime.
     */
    val subscribedDomains: List<LoggerDomain>
}
