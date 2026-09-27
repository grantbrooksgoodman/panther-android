//
//  NetworkServices.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking

import us.neotechnica.panther.modules.networking.conversation.services.ConversationService
import us.neotechnica.panther.modules.networking.message.services.MessageService
import us.neotechnica.panther.modules.networking.user.services.UserService
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

/**
 * The container that exposes the app's networking services.
 *
 * Reach a networking service through [DependencyValues.networking]
 * rather than referencing the service singletons or framework
 * delegates directly.
 *
 * **Note:** the integrity and schema-migration accessors are omitted;
 * those services are deferred to a separate plan (D-II-2).
 */
object NetworkServices {
    /** The authentication delegate. */
    val auth get() = Networking.config.authDelegate

    /** The conversation service. */
    val conversationService get() = ConversationService

    /** The database delegate. */
    val database get() = Networking.config.databaseDelegate

    /** The network health delegate. */
    val health get() = Networking.health

    /** The message service. */
    val messageService get() = MessageService

    /** The storage delegate. */
    val storage get() = Networking.config.storageDelegate

    /** The user service. */
    val userService get() = UserService
}

// MARK: - Dependency

private object NetworkServicesDependency : DependencyKey<NetworkServices> {
    override fun resolve(dependencies: DependencyValues): NetworkServices = NetworkServices
}

/** The container that exposes the app's networking services. */
val DependencyValues.networking: NetworkServices
    get() = this[NetworkServicesDependency]
