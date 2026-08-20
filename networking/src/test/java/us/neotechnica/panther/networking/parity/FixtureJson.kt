//
//  FixtureJson.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.parity

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Loads a parity fixture as a Kotlin value tree whose types match
 * what Firebase Realtime Database yields at runtime (`String`,
 * `Boolean`, `Long`, `Double`, `Map`, `List`).
 */
internal object FixtureJson {
    // MARK: - Methods

    fun loadObject(name: String): Map<String, Any?> {
        @Suppress("UNCHECKED_CAST")
        return convert(parse(name)) as Map<String, Any?>
    }

    // MARK: - Auxiliary

    private fun convert(element: JsonElement): Any? =
        when (element) {
            is JsonNull -> null
            is JsonObject -> element.mapValues { convert(it.value) }
            is JsonArray -> element.map { convert(it) }
            is JsonPrimitive -> primitive(element)
        }

    private fun parse(name: String): JsonElement {
        val resource =
            checkNotNull(javaClass.classLoader?.getResource("parity/$name")) {
                "Missing fixture parity/$name"
            }

        return Json.parseToJsonElement(resource.readText())
    }

    private fun primitive(element: JsonPrimitive): Any? {
        if (element.isString) return element.content
        element.booleanOrNull?.let { return it }
        element.longOrNull?.let { return it }
        return element.content.toDouble()
    }
}
