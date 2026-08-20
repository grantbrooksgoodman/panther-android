//
//  EventStream.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.shared.models

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import java.util.UUID

/**
 * A broadcaster for occurrences that carry a typed payload.
 *
 * Use [EventStream] when only the occurrence of something
 * matters – a signal to refresh, a captured screenshot, or a
 * delta describing what changed. An [EventStream] stores
 * nothing; subscribers receive only the events sent after they
 * subscribe. For values with a meaningful current state, use
 * [StateStream] instead.
 *
 * Declare events as extension properties on [SharedEvents]. Use
 * `EventStream<Unit>` when the event carries no payload:
 *
 * ```kotlin
 * val SharedEvents.sessionDidExpire: EventStream<Unit>
 *     get() = event("sessionDidExpire")
 * ```
 *
 * ## Subscribing to Events
 *
 * The [events] property vends an independent flow for each
 * collection. Payloads are buffered without bound, so no event
 * is dropped while a subscriber is suspended.
 *
 * ## Thread Safety
 *
 * All stored state is protected by [LockIsolated]. Events can be
 * sent from any thread and are delivered to every subscriber in
 * send order.
 */
class EventStream<Payload> {
    // MARK: - Properties

    private val continuations =
        LockIsolated(
            mapOf<UUID, SendChannel<Payload>>(),
        )

    // MARK: - Computed Properties

    /**
     * An asynchronous sequence of event payloads.
     *
     * Each collection creates an independent flow that yields
     * the payload of each event sent after subscription; events
     * sent beforehand are not replayed. Payloads are buffered
     * without bound, so every event is delivered even if the
     * consumer suspends while multiple events occur.
     */
    val events: Flow<Payload>
        get() =
            callbackFlow {
                val id = UUID.randomUUID()

                continuations.withValue {
                    it.value = it.value + (id to channel)
                }

                awaitClose {
                    continuations.withValue {
                        it.value = it.value - id
                    }
                }
            }.buffer(Channel.UNLIMITED)

    // MARK: - Methods

    /**
     * Delivers the given payload to every active [events]
     * subscriber.
     *
     * The yields to all subscribers are performed as a single
     * atomic operation, so concurrent senders cannot interleave
     * and every subscriber observes events in the same order.
     *
     * @param payload The payload to deliver.
     */
    fun send(payload: Payload) {
        continuations.withValue {
            for (continuation in it.value.values) {
                continuation.trySend(payload)
            }
        }
    }
}

/**
 * Notifies every active [EventStream.events] subscriber that
 * this event occurred.
 *
 * Use this convenience for signal-style events that carry no
 * payload:
 *
 * ```kotlin
 * sessionDidExpire.send()
 * ```
 */
fun EventStream<Unit>.send() {
    send(Unit)
}
