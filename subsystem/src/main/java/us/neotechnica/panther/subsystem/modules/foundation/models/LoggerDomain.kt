//
//  LoggerDomain.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.models

/**
 * A named category that groups related log output.
 *
 * Logger domains partition log messages into logical channels so
 * that you can subscribe to only the output you care about. Declare
 * app-specific domains as constants:
 *
 * ```kotlin
 * val CONVERSATION_LOGGER_DOMAIN = LoggerDomain("conversation")
 * ```
 */
@JvmInline
value class LoggerDomain(
    /** The domain's raw string identifier. */
    val rawValue: String,
) {
    // MARK: - Companion

    companion object {
        /** The domain for the alert and toast presentation layer. */
        val alertKit = LoggerDomain("alertKit")

        /** The domain for analytics event logging. */
        val analytics = LoggerDomain("analytics")

        /** The domain for bug-prevention diagnostics. */
        val bugPrevention = LoggerDomain("bugPrevention")

        /** The domain for cache lifecycle output. */
        val caches = LoggerDomain("caches")

        /** The domain for chat-page presentation state. */
        val chatPageState = LoggerDomain("chatPageState")

        /** The domain for the client session's lifecycle. */
        val clientSession = LoggerDomain("clientSession")

        /** The domain for concurrency and task scheduling. */
        val concurrency = LoggerDomain("concurrency")

        /** The domain for device-contact resolution. */
        val contacts = LoggerDomain("contacts")

        /** The domain for conversation-level operations. */
        val conversation = LoggerDomain("conversation")

        /** The domain for the conversation observer. */
        val conversationObserver = LoggerDomain("conversationObserver")

        /** The domain for the conversation archive store. */
        val conversationStore = LoggerDomain("conversationStore")

        /** The domain for conversation synchronization. */
        val conversationSync = LoggerDomain("conversationSync")

        /** The domain for data-integrity checks. */
        val dataIntegrity = LoggerDomain("dataIntegrity")

        /** The domain for data-usage accounting. */
        val dataUsage = LoggerDomain("dataUsage")

        /** The domain for caught exceptions. */
        val exception = LoggerDomain("exception")

        /** The default domain for uncategorized output. */
        val general = LoggerDomain("general")

        /** The domain for localization and translation resolution. */
        val localization = LoggerDomain("localization")

        /** The domain for the message archive store. */
        val messageStore = LoggerDomain("messageStore")

        /** The domain for push and in-app notifications. */
        val notifications = LoggerDomain("notifications")

        /** The domain for reactive observation. */
        val observer = LoggerDomain("observer")

        /** The domain for the message outbox. */
        val outbox = LoggerDomain("outbox")

        /** The domain for PenPals features. */
        val penPals = LoggerDomain("penPals")

        /** The domain for schema migration. */
        val schemaMigration = LoggerDomain("schemaMigration")

        /** The domain for the session store. */
        val sessionStore = LoggerDomain("sessionStore")

        /** The domain for string translation. */
        val translation = LoggerDomain("translation")

        /** The domain for UI cache invalidation. */
        val uiCacheInvalidation = LoggerDomain("uiCacheInvalidation")

        /** The domain for the current-user session. */
        val userSession = LoggerDomain("userSession")

        /** The domain for the user archive store. */
        val userStore = LoggerDomain("userStore")

        // MARK: - Networking

        /** The framework-layer networking domains. */
        val Networking = NetworkingDomains

        /** The framework-layer networking domains. */
        object NetworkingDomains {
            /** The domain for authentication. */
            val auth = LoggerDomain("auth")

            /** The domain for the database layer. */
            val database = LoggerDomain("database")

            /** The domain for network-health probing. */
            val health = LoggerDomain("health")

            /** The domain for hosted translation. */
            val hostedTranslation = LoggerDomain("hostedTranslation")

            /** The domain for the storage layer. */
            val storage = LoggerDomain("storage")
        }
    }
}
