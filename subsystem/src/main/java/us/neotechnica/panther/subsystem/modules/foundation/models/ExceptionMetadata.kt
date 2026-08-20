//
//  ExceptionMetadata.kt
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
