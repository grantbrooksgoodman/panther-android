//
//  LoggerDomains.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
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
            LoggerDomain.general,
            LoggerDomain.Networking.auth,
            LoggerDomain.Networking.health,
            LoggerDomain.Networking.hostedTranslation,
            LoggerDomain.localization,
            LoggerDomain.notifications,
            LoggerDomain.outbox,
            LoggerDomain.translation,
            LoggerDomain.userSession,
            LoggerDomain.userStore,
        )
}

// MARK: - App Logger Domains

/** The domain for analytics event logging. */
val LoggerDomain.Companion.analytics: LoggerDomain
    get() = LoggerDomain("analytics")

/** The domain for bug-prevention diagnostics. */
val LoggerDomain.Companion.bugPrevention: LoggerDomain
    get() = LoggerDomain("bugPrevention")

/** The domain for chat-page presentation state. */
val LoggerDomain.Companion.chatPageState: LoggerDomain
    get() = LoggerDomain("chatPageState")

/** The domain for the client session's lifecycle. */
val LoggerDomain.Companion.clientSession: LoggerDomain
    get() = LoggerDomain("clientSession")

/** The domain for device-contact resolution. */
val LoggerDomain.Companion.contacts: LoggerDomain
    get() = LoggerDomain("contacts")

/** The domain for conversation-level operations. */
val LoggerDomain.Companion.conversation: LoggerDomain
    get() = LoggerDomain("conversation")

/** The domain for the conversation observer. */
val LoggerDomain.Companion.conversationObserver: LoggerDomain
    get() = LoggerDomain("conversationObserver")

/** The domain for the conversation archive store. */
val LoggerDomain.Companion.conversationStore: LoggerDomain
    get() = LoggerDomain("conversationStore")

/** The domain for conversation synchronization. */
val LoggerDomain.Companion.conversationSync: LoggerDomain
    get() = LoggerDomain("conversationSync")

/** The domain for the message archive store. */
val LoggerDomain.Companion.messageStore: LoggerDomain
    get() = LoggerDomain("messageStore")

/** The domain for push and in-app notifications. */
val LoggerDomain.Companion.notifications: LoggerDomain
    get() = LoggerDomain("notifications")

/** The domain for the message outbox. */
val LoggerDomain.Companion.outbox: LoggerDomain
    get() = LoggerDomain("outbox")

/** The domain for the session store. */
val LoggerDomain.Companion.sessionStore: LoggerDomain
    get() = LoggerDomain("sessionStore")

/** The domain for UI cache invalidation. */
val LoggerDomain.Companion.uiCacheInvalidation: LoggerDomain
    get() = LoggerDomain("uiCacheInvalidation")

/** The domain for the current-user session. */
val LoggerDomain.Companion.userSession: LoggerDomain
    get() = LoggerDomain("userSession")

/** The domain for the user archive store. */
val LoggerDomain.Companion.userStore: LoggerDomain
    get() = LoggerDomain("userStore")
