//
//  Any+DatabaseExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.database.extensions

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

// Whether this value is an instance of the given type, checking the
// runtime key and element types of maps and collections against the
// type's arguments, which the JVM's erased casts cannot.
@PublishedApi
internal fun Any?.conformsTo(type: KType): Boolean {
    if (this == null) return type.isMarkedNullable
    val classifier = type.classifier as? KClass<*> ?: return true
    if (!classifier.isInstance(this)) return false

    val arguments = type.arguments
    if (arguments.isEmpty()) return true

    return when (this) {
        is Map<*, *> ->
            arguments.size == 2 &&
                entries.all { (key, value) -> key.conformsTo(arguments[0]) && value.conformsTo(arguments[1]) }

        is Iterable<*> -> arguments.size == 1 && all { it.conformsTo(arguments[0]) }

        else -> true
    }
}

private fun Any?.conformsTo(projection: KTypeProjection): Boolean = projection.type?.let { conformsTo(it) } ?: true
