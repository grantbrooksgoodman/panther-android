//
//  Exception.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.models

import java.security.MessageDigest

/**
 * A structured error type that captures a human-readable
 * description, a deterministic error code, source-location
 * metadata, and an optional chain of underlying exceptions.
 *
 * Use [Exception] throughout your app as the standard error
 * currency. Each exception records where it was created, what
 * went wrong, and whether it should be reported to
 * crash-reporting infrastructure:
 *
 * ```kotlin
 * throw Exception(
 *     "Failed to load configuration file.",
 *     metadata = ExceptionMetadata(this),
 * )
 * ```
 *
 * ## Error Codes
 *
 * Every exception carries a [code] derived from a SHA-256 hash
 * of its descriptor. This produces a short, deterministic
 * identifier that remains stable across builds for any given
 * descriptor string. You can also supply a static code through
 * the `userInfo` map's `"StaticErrorCode"` key.
 *
 * ## Underlying Exceptions
 *
 * Exceptions can form a chain through [underlyingExceptions].
 * Reading this property recursively traverses the entire chain,
 * returning a flat list of every exception in the hierarchy.
 */
class Exception(
    /** A developer-facing description of what went wrong. */
    val descriptor: String = "An unknown error occurred.",
    /**
     * A Boolean value that indicates whether this exception
     * should be reported to crash-reporting or analytics
     * infrastructure.
     */
    val isReportable: Boolean = true,
    /**
     * An optional map of supplementary information attached to
     * the exception.
     */
    val userInfo: Map<String, Any>? = null,
    underlyingExceptions: List<Exception>? = null,
    /** The source-location metadata captured at creation. */
    val metadata: ExceptionMetadata,
) : kotlin.Exception(descriptor) {
    // MARK: - Companion

    companion object {
        /**
         * Creates an exception from a Kotlin [Throwable].
         *
         * The throwable's type and message are captured into the
         * descriptor and user info.
         *
         * @param throwable The throwable to wrap.
         * @param metadata The source-location metadata for this
         *   exception.
         *
         * @return The wrapping exception.
         */
        fun from(
            throwable: Throwable,
            metadata: ExceptionMetadata,
        ): Exception =
            Exception(
                throwable.message ?: throwable.javaClass.simpleName,
                userInfo =
                    mapOf(
                        "ThrowableType" to throwable.javaClass.name,
                    ),
                metadata = metadata,
            )
    }

    // MARK: - Properties

    /**
     * A short, deterministic error code derived from the
     * descriptor.
     */
    val code: String

    private val storedUnderlyingExceptions: List<Exception>?

    // MARK: - Computed Properties

    /**
     * The full chain of underlying exceptions, recursively
     * traversed.
     *
     * When read, this property walks the entire underlying
     * exception hierarchy and returns a flat list containing
     * every exception in the chain.
     */
    val underlyingExceptions: List<Exception>?
        get() {
            val underlying = storedUnderlyingExceptions ?: return null
            val all = underlying.toMutableList()
            for (exception in underlying) {
                all.addAll(exception.underlyingExceptions ?: emptyList())
            }

            return all
        }

    // MARK: - Init

    init {
        code = (userInfo?.get("StaticErrorCode") as? String) ?: descriptor.errorCode
        storedUnderlyingExceptions =
            underlyingExceptions
                ?.takeIf { it.isNotEmpty() }
                ?.distinct()
                ?.filter { it != this }
    }

    // MARK: - Append

    /**
     * Returns a new exception with the given exception appended
     * to its underlying exception chain.
     *
     * @param underlyingException The exception to append.
     *
     * @return A new exception with the extended chain.
     */
    fun appending(underlyingException: Exception): Exception =
        Exception(
            descriptor,
            isReportable = isReportable,
            userInfo = userInfo,
            underlyingExceptions = (storedUnderlyingExceptions ?: emptyList()) + underlyingException,
            metadata = metadata,
        )

    /**
     * Returns a new exception with the given entries merged into
     * its user info map.
     *
     * Existing keys are overwritten by entries in the provided
     * map.
     *
     * @param userInfo The entries to merge.
     *
     * @return A new exception with the merged user info.
     */
    fun appending(userInfo: Map<String, Any>): Exception {
        if (userInfo.isEmpty()) return this
        return Exception(
            descriptor,
            isReportable = isReportable,
            userInfo = (this.userInfo ?: emptyMap()) + userInfo,
            underlyingExceptions = storedUnderlyingExceptions,
            metadata = metadata,
        )
    }

    // MARK: - Equatable Conformance

    override fun equals(other: Any?): Boolean {
        if (other !is Exception) return false
        return code == other.code &&
            descriptor == other.descriptor &&
            isReportable == other.isReportable &&
            storedUnderlyingExceptions == other.storedUnderlyingExceptions
    }

    override fun hashCode(): Int {
        var result = code.hashCode()
        result = 31 * result + descriptor.hashCode()
        result = 31 * result + isReportable.hashCode()
        return result
    }
}

// Derives a four-character code from the descriptor: stop words
// removed, letters only, lowercased, SHA-256, first two + last
// two hex characters, uppercased. Mirrors the iOS derivation.
private val String.errorCode: String
    get() {
        if (isEmpty()) return "0000"

        val stopWords =
            setOf(
                "a",
                "an",
                "is",
                "that",
                "the",
                "this",
                "was",
            )

        val joinedWords =
            split(" ")
                .filter { it.lowercase() !in stopWords }
                .joinToString("")

        val lettersOnly =
            joinedWords
                .replace(Regex("[^A-Za-z]"), "")
                .lowercase()

        val digest = MessageDigest.getInstance("SHA-256")
        val hexString =
            digest
                .digest(lettersOnly.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }

        return (hexString.take(2) + hexString.takeLast(2)).uppercase()
    }
