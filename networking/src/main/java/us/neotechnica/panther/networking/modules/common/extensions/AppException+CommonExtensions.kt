//
//  AppException+CommonExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.extensions

import us.neotechnica.panther.subsystem.modules.foundation.models.AppException

/**
 * A namespace for catalogued networking exception
 * identifiers.
 *
 * Use these constants to compare an `Exception` against a
 * known networking error condition:
 *
 * ```kotlin
 * if (exception.isEqual(
 *         to = AppException.Networking.Database.noValueExists,
 *     )
 * ) {
 *     // Handle missing value.
 * }
 * ```
 */
object NetworkingAppExceptions {
    // MARK: - Properties

    /** Catalogued database exceptions. */
    val Database = DatabaseAppExceptions

    /** Catalogued serialization exceptions. */
    val Serializable = SerializableAppExceptions

    /** Catalogued storage exceptions. */
    val Storage = StorageAppExceptions

    /** Catalogued translation exceptions. */
    val Translation = TranslationAppExceptions

    // MARK: - Types

    /** Catalogued database exceptions. */
    object DatabaseAppExceptions {
        /** No value exists at the specified path. */
        val noValueExists = AppException("BE3A")
    }

    /** Catalogued serialization exceptions. */
    object SerializableAppExceptions {
        /** Decoding the serialized data failed. */
        val decodingFailed = AppException("20FC")

        /** The serialization key is not updatable. */
        val notRemotelyUpdatable = AppException("6446")

        /** The type value was not serialized. */
        val notSerialized = AppException("7CC2")

        /** A type mismatch occurred for a serialization key. */
        val typeMismatch = AppException("8117")
    }

    /** Catalogued storage exceptions. */
    object StorageAppExceptions {
        /** A generic storage error occurred. */
        val genericStorageError = AppException("C81B")

        /** The specified storage item does not exist. */
        val storageItemDoesNotExist = AppException("9207")
    }

    /** Catalogued translation exceptions. */
    object TranslationAppExceptions {
        /**
         * All available translation platforms have been
         * exhausted without success.
         */
        val exhaustedAvailablePlatforms = AppException("C526")

        /** The translation input and output are identical. */
        val sameTranslationInputOutput = AppException("6CEB")

        /** Derivation of the translation failed. */
        val translationDerivationFailed = AppException("43B4")
    }
}

/**
 * A namespace for catalogued networking exception
 * identifiers.
 *
 * Use these constants to compare an `Exception` against a
 * known networking error condition:
 *
 * ```kotlin
 * if (exception.isEqual(
 *         to = AppException.Networking.Database.noValueExists,
 *     )
 * ) {
 *     // Handle missing value.
 * }
 * ```
 */
val AppException.Companion.Networking: NetworkingAppExceptions
    get() = NetworkingAppExceptions
