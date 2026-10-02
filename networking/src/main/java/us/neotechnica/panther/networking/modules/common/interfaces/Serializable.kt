//
//  Serializable.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.interfaces

/**
 * A type that can convert itself to and from a serialized
 * representation suitable for remote storage.
 *
 * Implement [Serializable] when a model needs to be written to
 * or read from the network database. Each conformer specifies
 * the format used for serialization through [encoded] –
 * typically a `Map<String, Any?>` or a `String`:
 *
 * ```kotlin
 * data class MyModel(val name: String) {
 *     companion object : SerializableDecoder<MyModel, Map<String, Any?>> {
 *         override fun canDecode(data: Map<String, Any?>): Boolean = …
 *         override fun decode(data: Map<String, Any?>): MyModel = …
 *     }
 * }
 *
 * class MyModelSerializer : Serializable<Map<String, Any?>> {
 *     override val encoded: Map<String, Any?> get() = …
 * }
 * ```
 *
 * Use [SerializableDecoder.canDecode] to check whether a given
 * payload is structurally valid before attempting decoding.
 *
 * **Note:** In Kotlin the decode side lives on a companion
 * [SerializableDecoder] rather than an initializer, because
 * interfaces cannot declare constructors.
 */
interface Serializable<out Representation> {
    // MARK: - Properties

    /**
     * The serialized representation of this instance, suitable
     * for writing to the database.
     */
    val encoded: Representation
}

/**
 * The decode half of the [Serializable] contract.
 *
 * Implement this on a model's companion object to provide
 * structural validation and decoding from a serialized
 * representation.
 */
interface SerializableDecoder<out Model, in Representation> {
    // MARK: - Methods

    /**
     * Returns a Boolean value that indicates whether the
     * specified data can be decoded into an instance of the
     * model.
     *
     * Call this method to perform a lightweight structural
     * check before committing to a full decode. The method does
     * not perform network requests.
     *
     * @param data The serialized data to evaluate.
     *
     * @return `true` if the data can be decoded; otherwise,
     *   `false`.
     */
    fun canDecode(data: Representation): Boolean

    /**
     * Creates a new instance by decoding from the specified
     * serialized data.
     *
     * Decoding parses the serialized representation, delegating
     * any nested references to their own decoders.
     *
     * @param data The serialized data to decode.
     *
     * @return The decoded model.
     *
     * @throws us.neotechnica.panther.subsystem.modules.foundation.models.Exception
     *   if decoding fails.
     */
    fun decode(data: Representation): Model
}
