//
//  Exception+CommonExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.extensions

import us.neotechnica.panther.networking.modules.storage.models.HostedItemType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

/**
 * A namespace for factory methods that create
 * networking-specific exceptions.
 *
 * Use the methods on [Exception.Companion.Networking] to create
 * exceptions with descriptive context for common networking
 * error conditions:
 *
 * ```kotlin
 * throw Exception.Networking.decodingFailed(
 *     data = rawData,
 *     ExceptionMetadata(this),
 * )
 * ```
 */
object NetworkingExceptionFactories {
    // MARK: - Public

    /**
     * Creates an exception indicating that decoding the
     * specified data failed.
     *
     * @param data The data that could not be decoded.
     * @param metadata The exception metadata.
     */
    fun decodingFailed(
        data: Any,
        metadata: ExceptionMetadata,
    ): Exception =
        Exception(
            "Decoding failed.",
            userInfo = mapOf("Data" to data),
            metadata = metadata,
        )

    /**
     * Creates an exception indicating that a serialized value
     * does not conform to a supported type.
     *
     * @param value The unsupported value.
     * @param metadata The exception metadata.
     */
    fun invalidType(
        value: Any,
        metadata: ExceptionMetadata,
    ): Exception =
        Exception(
            "Serialized values must conform to NSArray, NSDictionary, NSNull, NSNumber, or NSString.",
            userInfo = mapOf("Value" to value),
            metadata = metadata,
        )

    /**
     * Creates an exception indicating that the specified
     * property is not remotely updatable.
     *
     * @param key The serialization key or key path that could
     *   not be updated.
     * @param metadata The exception metadata.
     */
    fun notRemotelyUpdatable(
        key: Any,
        metadata: ExceptionMetadata,
    ): Exception =
        Exception(
            "The specified serialization key is not updatable.",
            userInfo = mapOf("Key" to key),
            metadata = metadata,
        )

    /**
     * Creates an exception indicating that a type value was
     * not serialized before use.
     *
     * @param data The unserialized data.
     * @param metadata The exception metadata.
     */
    fun notSerialized(
        data: Map<String, Any?>,
        metadata: ExceptionMetadata,
    ): Exception =
        Exception(
            "Type value must be serialized.",
            userInfo = mapOf("Data" to data),
            metadata = metadata,
        )

    /**
     * Creates an exception indicating that a typecast
     * operation failed.
     *
     * @param typeName The name of the target type. Pass `null`
     *   for a generic failure message.
     * @param userInfo A map of supplementary information. The
     *   default is `null`.
     * @param metadata The exception metadata.
     */
    fun typecastFailed(
        typeName: String? = null,
        userInfo: Map<String, Any>? = null,
        metadata: ExceptionMetadata,
    ): Exception {
        val resolvedUserInfo = (userInfo ?: emptyMap()) + mapOf("StaticErrorCode" to "3530")

        return Exception(
            "Failed to typecast values ${if (typeName == null) "." else "to $typeName."}",
            userInfo = resolvedUserInfo,
            metadata = metadata,
        )
    }

    /**
     * Creates an exception indicating a type mismatch for the
     * specified serialization key.
     *
     * @param key The key with the mismatched type.
     * @param type The mismatched type.
     * @param metadata The exception metadata.
     */
    fun typeMismatch(
        key: Any,
        type: Any?,
        metadata: ExceptionMetadata,
    ): Exception =
        Exception(
            "Type mismatch for serialization key.",
            userInfo =
                mapOf(
                    "Key" to key,
                    "MisatchedType" to "$type",
                ),
            metadata = metadata,
        )

    // MARK: - Internal

    internal fun hostedItemTypeMismatch(
        path: String,
        type: HostedItemType?,
        metadata: ExceptionMetadata,
    ): Exception {
        fun exception(descriptor: String): Exception =
            Exception(
                descriptor,
                userInfo =
                    mapOf(
                        "Path" to path,
                        "StaticErrorCode" to "9207",
                    ),
                metadata = metadata,
            )

        type ?: return exception("No item exists at the specified key path.")

        val actualType = if (type == HostedItemType.DIRECTORY) "file" else "directory"
        val expectedType = if (type == HostedItemType.DIRECTORY) "directory" else "file"

        return exception(
            "Specified key path points to an existing $actualType, not a $expectedType.",
        )
    }

    internal fun inputsFailValidation(
        userInfo: Map<String, Any>,
        metadata: ExceptionMetadata,
    ): Exception =
        Exception(
            "Input fails validation.",
            userInfo = userInfo,
            metadata = metadata,
        )

    internal fun languagePairFailsValidation(
        userInfo: Map<String, Any>,
        metadata: ExceptionMetadata,
    ): Exception =
        Exception(
            "Language pair fails validation.",
            userInfo = userInfo,
            metadata = metadata,
        )

    internal fun readWriteAccessDisabled(metadata: ExceptionMetadata): Exception =
        Exception(
            "Read/write access has been disabled.",
            isReportable = false,
            metadata = metadata,
        )

    internal fun translationFailsValidation(
        userInfo: Map<String, Any>,
        metadata: ExceptionMetadata,
    ): Exception =
        Exception(
            "Translation fails validation.",
            userInfo = userInfo,
            metadata = metadata,
        )
}

/**
 * A namespace for factory methods that create
 * networking-specific exceptions.
 *
 * Use the methods on [Exception.Companion.Networking] to create
 * exceptions with descriptive context for common networking
 * error conditions:
 *
 * ```kotlin
 * throw Exception.Networking.decodingFailed(
 *     data = rawData,
 *     ExceptionMetadata(this),
 * )
 * ```
 */
val Exception.Companion.Networking: NetworkingExceptionFactories
    get() = NetworkingExceptionFactories
