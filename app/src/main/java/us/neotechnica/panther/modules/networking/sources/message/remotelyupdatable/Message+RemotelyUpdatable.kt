//
//  Message+RemotelyUpdatable.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.sources.message.remotelyupdatable

import us.neotechnica.panther.bundle.messages
import us.neotechnica.panther.modules.networking.sources.common.interfaces.asList
import us.neotechnica.panther.modules.networking.sources.common.interfaces.encodeForWrite
import us.neotechnica.panther.modules.networking.sources.message.models.Message
import us.neotechnica.panther.modules.networking.sources.message.models.ReadReceipt
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.dependencies.networking
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

// MARK: - Types

/** The remotely updatable keys of a message. */
enum class MessageUpdatableKey(
    val rawValue: String,
) {
    READ_RECEIPTS("readReceipts"),
}

// MARK: - Modify Key

/**
 * Returns a copy of the message with the given key set to the given
 * value, or `null` if the value's type does not match the key.
 *
 * An empty read-receipt list becomes `null`.
 */
fun Message.modifyKey(
    key: MessageUpdatableKey,
    value: Any,
): Message? =
    when (key) {
        MessageUpdatableKey.READ_RECEIPTS ->
            value.asList<ReadReceipt>()?.let { copy(readReceipts = it.ifEmpty { null }) }
    }

// MARK: - Update

/** Writes the value for the given key to the server and returns the updated message. */
suspend fun Message.update(
    key: MessageUpdatableKey,
    to: Any,
): Message {
    val newValue =
        modifyKey(key, to) ?: throw Exception.Networking.typeMismatch(
            key.rawValue,
            to,
            ExceptionMetadata(this),
        )

    val valueKeyPath = "${NetworkPath.messages.rawValue}/$id/${key.rawValue}"

    DependencyValues.current.networking.database
        .setValue(encodeForWrite(this, key.rawValue, to), valueKeyPath)
    return didWrite(newValue, key)
}

// MARK: - Did Write

/** Applies a completed single-field remote update, upserting the updated message into the session store. */
@Suppress("UnusedParameter") // `key` matches the shared updatable-type write signature.
fun Message.didWrite(
    updated: Message,
    key: MessageUpdatableKey,
): Message {
    DependencyValues.current.clientSession.store
        .upsertMessages(setOf(updated))
    return updated
}
