//
//  EncodedHashable.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.interfaces

import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.SwiftJSONEncoder
import java.security.MessageDigest

/**
 * A type that can produce a deterministic, SHA-256-based
 * identifier from an array of string factors.
 *
 * Implement [EncodedHashable] to give a type a stable,
 * content-derived identifier that is suitable for equality
 * checks, persistent storage keys, or cache lookups. The
 * interface requires a single property, [hashFactors], which
 * supplies the strings that feed into the hash computation:
 *
 * ```kotlin
 * data class Document(
 *     val title: String,
 *     val version: Int,
 * ) : EncodedHashable {
 *     override val hashFactors: List<String>
 *         get() = listOf(title, version.toString())
 * }
 * ```
 *
 * The [encodedHash] property JSON-encodes the factors using
 * [SwiftJSONEncoder], computes a SHA-256 digest, and returns the
 * result as a lowercase hexadecimal string. Computed hashes are
 * cached in memory, so repeated access for the same factors is
 * inexpensive.
 *
 * **Note:** Because the hash is derived from the content of
 * [hashFactors], two instances with the same factors always
 * produce the same [encodedHash], regardless of type. The
 * computation is stable across runs.
 */
interface EncodedHashable {
    /**
     * The strings that collectively define this instance's
     * identity for hashing purposes.
     *
     * The order and content of the returned list directly affect
     * the resulting [encodedHash]. Changing the factors – or
     * their order – produces a different hash.
     */
    val hashFactors: List<String>
}

/**
 * A deterministic, SHA-256-based hexadecimal string derived from
 * [EncodedHashable.hashFactors].
 *
 * The factors are JSON-encoded and then hashed using SHA-256.
 * The result is a 64-character lowercase hexadecimal string.
 * Computed hashes are cached in memory for the lifetime of the
 * process, so accessing this property repeatedly for the same
 * factors does not recompute the digest.
 */
val EncodedHashable.encodedHash: String
    get() = encodedHashOf(hashFactors)

/**
 * Returns the deterministic, SHA-256-based hexadecimal string
 * for the given hash factors.
 *
 * Use this function when computing an identity hash for factors
 * that do not belong to an [EncodedHashable] instance.
 *
 * @param hashFactors The strings that define the identity.
 *
 * @return A 64-character lowercase hexadecimal string.
 */
fun encodedHashOf(hashFactors: List<String>): String {
    // Unit separator prevents boundary collisions:
    // ["ab", "c"] vs ["a", "bc"] produce distinct keys.
    // The size prefix disambiguates [] from [""], whose
    // joined forms are identical.
    val compiledString =
        "${hashFactors.size}\u001F" +
            hashFactors.joinToString("\u001F")

    val storedValue =
        EncodedHashStore.storedHashes.withValue {
            it.value[compiledString]
        }

    if (storedValue != null) return storedValue

    val digest = MessageDigest.getInstance("SHA-256")
    val encodedHash =
        digest
            .digest(SwiftJSONEncoder.encode(hashFactors))
            .toHexString()

    // Stored in place: copying the store on each insertion would make
    // hashing many values quadratic in the number of stored hashes.
    EncodedHashStore.storedHashes.withValue {
        it.value[compiledString] = encodedHash
    }

    return encodedHash
}

internal object EncodedHashStore {
    // MARK: - Properties

    val storedHashes = LockIsolated(mutableMapOf<String, String>())

    // MARK: - Methods

    fun clearStore() {
        storedHashes.wrappedValue = mutableMapOf()
    }
}
