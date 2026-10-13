//
//  ExceptionMetadata.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.models

import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

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
class ExceptionMetadata private constructor(
    sender: Any?,
    frame: StackTraceElement?,
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

    /**
     * Creates metadata for the current source location.
     *
     * The file name, function, and line are captured automatically
     * from the call site's stack frame.
     *
     * @param sender The object or type creating the exception.
     */
    constructor(sender: Any) : this(sender, callSiteFrame())

    // Derives the sender from the type that declares the call site,
    // for log entries that are not given a sender of their own.
    internal constructor() : this(null, callSiteFrame())

    init {
        this.sender =
            when (sender) {
                null -> frame?.senderName ?: "Unknown"
                is Class<*> -> sender.senderName
                is kotlin.reflect.KClass<*> -> sender.java.senderName
                else -> sender.javaClass.senderName
            }

        fileName = (frame?.fileName ?: "Unknown").substringBefore('.')
        function = frame?.functionName ?: "Unknown"
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

// The classes whose frames are never a call site: the metadata
// itself, and the logger that creates metadata on its callers'
// behalf.
private val internalClassNames =
    setOf(
        ExceptionMetadata::class.java.name,
        Logger::class.java.name,
    )

// The type's simple name, with companion objects and anonymous
// classes resolved to the type that encloses them.
private val Class<*>.senderName: String
    get() =
        if (simpleName.isEmpty() || simpleName == COMPANION_OBJECT_NAME) {
            enclosingClass?.senderName ?: "Unknown"
        } else {
            simpleName
        }

// The number of leading class name segments that name source-level
// types: a file facade, or an outermost class followed by any nested
// types. The segments after them name the function that declares a
// lambda, coroutine body, or anonymous object.
private val List<String>.typeSegmentCount: Int
    get() =
        when {
            isEmpty() -> 0
            first().endsWith(FILE_FACADE_SUFFIX) -> 1
            else -> 1 + drop(1).takeWhile { it.first().isUpperCase() }.size
        }

// The `$`-separated segments of the frame's binary class name.
private val StackTraceElement.classNameSegments: List<String>
    get() =
        className
            .substringAfterLast('.')
            .split('$')
            .filter { it.isNotEmpty() }

// The name of the source-level function that contains the frame's
// code, resolving the names the compiler synthesizes for lambdas,
// coroutine bodies, and constructors.
private val StackTraceElement.functionName: String
    get() {
        if (methodName.startsWith('<')) return INITIALIZER_NAME
        val segments = classNameSegments
        val enclosingFunctionName =
            segments
                .drop(segments.typeSegmentCount)
                .firstOrNull { segment -> !segment.all(Char::isDigit) }

        return (enclosingFunctionName ?: methodName)
            .substringBefore('$')
            .substringBefore('-')
    }

private val StackTraceElement.isInternal: Boolean
    get() = className.substringBefore('$') in internalClassNames

// The innermost source-level type that declares the frame's code,
// with companion objects resolved to the type that encloses them.
private val StackTraceElement.senderName: String
    get() {
        val segments = classNameSegments
        return segments
            .take(segments.typeSegmentCount)
            .lastOrNull { it != COMPANION_OBJECT_NAME }
            ?.removeSuffix(FILE_FACADE_SUFFIX)
            ?: "Unknown"
    }

// Resolves the frame that created the metadata or called into the
// logger. The runtime's own stack-walking frames (`VMStack` on
// Android, `Thread` on the JVM) and this function precede the first
// internal frame, so the walk skips everything up to and including
// the internal frames rather than matching runtime class names.
private fun callSiteFrame(): StackTraceElement? =
    Thread
        .currentThread()
        .stackTrace
        .dropWhile { !it.isInternal }
        .firstOrNull { !it.isInternal }

private const val COMPANION_OBJECT_NAME = "Companion"
private const val FILE_FACADE_SUFFIX = "Kt"
private const val ID_ASCII_LIMIT = 128
private const val ID_HEX_THRESHOLD = 3
private const val ID_HEX_PREFIX_END = 4
private const val INITIALIZER_NAME = "init"
