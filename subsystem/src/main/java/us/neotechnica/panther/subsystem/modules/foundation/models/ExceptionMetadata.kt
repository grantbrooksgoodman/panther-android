//
//  ExceptionMetadata.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.models

/**
 * Source-location metadata captured at the point an [Exception]
 * is created.
 *
 * Create metadata by passing the throwing instance as the
 * sender; the file name, function, and line are captured
 * automatically from the call site's stack frame:
 *
 * ```kotlin
 * throw Exception(
 *     "Failed to load configuration file.",
 *     metadata = ExceptionMetadata(this),
 * )
 * ```
 */
class ExceptionMetadata(
    sender: Any,
) {
    // MARK: - Properties

    /** The file in which the exception was created. */
    val fileName: String

    /** The function in which the exception was created. */
    val function: String

    /** The line at which the exception was created. */
    val line: Int

    /** A description of the type that created the exception. */
    val sender: String

    // MARK: - Computed Properties

    /**
     * A short, stable identifier derived from the source file name
     * and line.
     */
    val id: String
        get() {
            var hexCharacters =
                fileName
                    .mapNotNull { character -> character.code.takeIf { it < ID_ASCII_LIMIT } }
                    .map { "%02X".format(it) }
            if (hexCharacters.size > ID_HEX_THRESHOLD) {
                hexCharacters = hexCharacters.subList(0, ID_HEX_PREFIX_END) + hexCharacters.last()
            }
            return "${hexCharacters.joinToString("")}x$line".lowercase()
        }

    // MARK: - Init

    init {
        this.sender =
            when (sender) {
                is Class<*> -> sender.simpleName
                is kotlin.reflect.KClass<*> -> sender.simpleName ?: "Unknown"
                else -> sender.javaClass.simpleName
            }

        val frame =
            Thread.currentThread().stackTrace.firstOrNull {
                !it.className.startsWith("java.lang.Thread") &&
                    !it.className.startsWith(
                        "us.neotechnica.panther.subsystem.modules.foundation.models",
                    )
            }

        fileName = frame?.fileName ?: "Unknown"
        function = frame?.methodName ?: "Unknown"
        line = frame?.lineNumber ?: 0
    }

    // MARK: - Equatable Conformance

    override fun equals(other: Any?): Boolean {
        if (other !is ExceptionMetadata) return false
        return fileName == other.fileName &&
            function == other.function &&
            line == other.line &&
            sender == other.sender
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + function.hashCode()
        result = 31 * result + line
        result = 31 * result + sender.hashCode()
        return result
    }
}

private const val ID_ASCII_LIMIT = 128
private const val ID_HEX_THRESHOLD = 3
private const val ID_HEX_PREFIX_END = 4
