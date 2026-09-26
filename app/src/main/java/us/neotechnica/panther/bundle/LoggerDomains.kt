//
//  LoggerDomains.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 25/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle

import us.neotechnica.panther.subsystem.modules.foundation.interfaces.LoggerDomainSubscriptionDelegate
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain

/**
 * The app's logger domain subscription.
 *
 * Register this delegate at launch to control the initial logging
 * configuration. Only messages logged to a subscribed domain
 * produce output; domains not in [subscribedDomains] are silently
 * ignored unless subscribed to later at runtime.
 */
object LoggerDomainSubscription : LoggerDomainSubscriptionDelegate {
    // MARK: - Properties

    override val domainsExcludedFromSessionRecord: List<LoggerDomain> =
        listOf(
            LoggerDomain.caches,
            LoggerDomain.concurrency,
        )

    override val subscribedDomains: List<LoggerDomain> =
        listOf(
            LoggerDomain.alertKit,
            LoggerDomain.analytics,
            LoggerDomain.bugPrevention,
            LoggerDomain.chatPageState,
            LoggerDomain.conversation,
            LoggerDomain.conversationObserver,
            LoggerDomain.conversationStore,
            LoggerDomain.conversationSync,
            LoggerDomain.dataIntegrity,
            LoggerDomain.dataUsage,
            LoggerDomain.exception,
            LoggerDomain.general,
            LoggerDomain.Networking.auth,
            LoggerDomain.Networking.health,
            LoggerDomain.Networking.hostedTranslation,
            LoggerDomain.localization,
            LoggerDomain.notifications,
            LoggerDomain.outbox,
            LoggerDomain.penPals,
            LoggerDomain.schemaMigration,
            LoggerDomain.translation,
            LoggerDomain.userSession,
            LoggerDomain.userStore,
        )
}
