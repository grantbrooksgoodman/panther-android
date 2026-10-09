//
//  LoggerDomainSubscriptionDelegate.kt
//  Panther Android
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
 * When no delegate is registered,
 * [DefaultLoggerDomainSubscriptionDelegate] supplies the built-in
 * subsystem domains.
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

/**
 * The default logger domain subscription, which subscribes to all
 * built-in subsystem domains and excludes none from the session
 * record.
 *
 * The logger uses this delegate whenever no app-level
 * [LoggerDomainSubscriptionDelegate] is registered.
 */
object DefaultLoggerDomainSubscriptionDelegate : LoggerDomainSubscriptionDelegate {
    override val domainsExcludedFromSessionRecord: List<LoggerDomain> = emptyList()

    override val subscribedDomains: List<LoggerDomain> =
        listOf(
            LoggerDomain.alertKit,
            LoggerDomain.caches,
            LoggerDomain.concurrency,
            LoggerDomain.general,
            LoggerDomain.localization,
            LoggerDomain.translation,
        )
}
