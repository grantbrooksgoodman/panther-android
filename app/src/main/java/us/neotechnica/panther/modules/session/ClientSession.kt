//
//  ClientSession.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session

import us.neotechnica.panther.modules.session.entity.interfaces.DeliveryProgressIndicator
import us.neotechnica.panther.modules.session.entity.models.EntitySession
import us.neotechnica.panther.modules.session.state.services.MessageOutboxService
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.modules.session.sync.models.SyncSession
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage

/**
 * The container for the current client's session.
 *
 * Use [ClientSession] to reach the services that make up an active
 * session: the entity services that read and write conversations,
 * messages, and users; the outbox that queues outgoing messages; the
 * in-memory store of resolved content; and the synchronization
 * services that keep a conversation current. Resolve it through the
 * [DependencyValues.clientSession] dependency.
 */
object ClientSession {
    // MARK: - Properties

    /** The container for the session's entity services. */
    val entity = EntitySession

    /** The service that queues messages for delivery and retries failed sends. */
    val outbox = MessageOutboxService

    /** The in-memory store of the session's conversations, messages, and users. */
    val store = SessionStore

    /** The container for the session's conversation synchronization services. */
    val sync = SyncSession

    private val deliveryProgressIndicatorValue = LockIsolated<DeliveryProgressIndicator?>(null)

    // MARK: - Computed Properties

    /**
     * The indicator that displays the progress of the current message
     * delivery, if one is registered.
     *
     * **Important:** [DeliveryProgressIndicator] is a main-thread
     * type. Access its members only from the main thread.
     */
    val deliveryProgressIndicator: DeliveryProgressIndicator?
        get() = deliveryProgressIndicatorValue.wrappedValue

    // MARK: - Methods

    /**
     * Registers the indicator that displays the progress of the
     * current message delivery.
     *
     * @param deliveryProgressIndicator The indicator to register.
     */
    fun registerDeliveryProgressIndicator(deliveryProgressIndicator: DeliveryProgressIndicator) {
        deliveryProgressIndicatorValue.wrappedValue = deliveryProgressIndicator
    }

    /**
     * Resolves the current user's language code from the database and
     * applies it to the app.
     */
    suspend fun resolveAndSetLanguageCode() {
        val currentUserID =
            Persistent.string(PersistentStorageKey.currentUserID)
                ?: throw Exception("Current user ID has not been set.", metadata = ExceptionMetadata(this))

        val languageCode: String =
            Networking.config.databaseDelegate.getValues(
                path = listOf(NetworkPath.users.rawValue, currentUserID, LANGUAGE_CODE_KEY).joinToString("/"),
                cacheStrategy = CacheStrategy.ADAPTIVE,
            )

        Logger.log("Setting language code to ${languageCode.uppercase()}.", domain = LoggerDomain.clientSession)
        RuntimeStorage.languageCode = languageCode
    }

    // MARK: - Companion

    private const val LANGUAGE_CODE_KEY = "languageCode"
}

// MARK: - Dependency

private object ClientSessionDependency : DependencyKey<ClientSession> {
    override fun resolve(dependencies: DependencyValues): ClientSession = ClientSession
}

/** The current client's session. */
val DependencyValues.clientSession: ClientSession
    get() = this[ClientSessionDependency]
