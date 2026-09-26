//
//  MessageOutboxService+Serialization.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.state.services

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import us.neotechnica.panther.modules.common.models.MediaFileExtension
import us.neotechnica.panther.modules.session.state.models.OutboxEntry
import java.util.Date

internal fun encodeOutboxArchive(entries: List<OutboxEntry>): String =
    buildJsonArray {
        for (entry in entries) {
            add(
                buildJsonObject {
                    put(KEY_ID, JsonPrimitive(entry.id))
                    put(KEY_CONVERSATION_ID_KEY, JsonPrimitive(entry.conversationIDKey))
                    put(KEY_FROM_ACCOUNT_ID, JsonPrimitive(entry.fromAccountID))
                    put(KEY_RECIPIENT_USER_IDS, JsonArray(entry.recipientUserIDs.map { JsonPrimitive(it) }))
                    put(KEY_IS_PEN_PALS, JsonPrimitive(entry.isPenPalsConversation))
                    put(KEY_CREATED_DATE, JsonPrimitive(entry.createdDate.time))
                    put(KEY_ATTEMPT_COUNT, JsonPrimitive(entry.attemptCount))
                    put(KEY_LAST_ATTEMPT_DATE, entry.lastAttemptDate?.let { JsonPrimitive(it.time) } ?: JsonNull)
                    put(KEY_RESERVED_REMOTE_ID, entry.reservedRemoteID?.let { JsonPrimitive(it) } ?: JsonNull)
                    put(KEY_STATE, JsonPrimitive(entry.state.rawValue))
                    put(KEY_TRANSCRIPTION, entry.transcription?.let { JsonPrimitive(it) } ?: JsonNull)
                    put(KEY_PAYLOAD, encodePayload(entry.payload))
                },
            )
        }
    }.toString()

internal fun decodeOutboxArchive(archive: String): List<OutboxEntry> {
    val array = Json.parseToJsonElement(archive) as? JsonArray ?: return emptyList()
    return array.mapNotNull { element -> runCatching { decodeEntry(element.jsonObject) }.getOrNull() }
}

private fun encodePayload(payload: OutboxEntry.Payload): JsonObject =
    buildJsonObject {
        when (payload) {
            is OutboxEntry.Payload.Audio -> {
                put(KEY_PAYLOAD_TYPE, JsonPrimitive(PAYLOAD_AUDIO))
                put(KEY_INPUT_FILE_NAME, JsonPrimitive(payload.inputFileName))
            }
            is OutboxEntry.Payload.Media -> {
                put(KEY_PAYLOAD_TYPE, JsonPrimitive(PAYLOAD_MEDIA))
                put(KEY_FILE_NAME, JsonPrimitive(payload.fileName))
                put(KEY_FILE_EXTENSION, JsonPrimitive(payload.fileExtension.rawValue))
            }
            is OutboxEntry.Payload.Text -> {
                put(KEY_PAYLOAD_TYPE, JsonPrimitive(PAYLOAD_TEXT))
                put(KEY_VALUE, JsonPrimitive(payload.value))
            }
        }
    }

private fun decodeEntry(obj: JsonObject): OutboxEntry? {
    val id = obj[KEY_ID]?.jsonPrimitive?.content ?: return null
    val state = OutboxEntry.State.from(obj[KEY_STATE]?.jsonPrimitive?.content ?: return null) ?: return null
    val payload = decodePayload(obj) ?: return null

    return OutboxEntry(
        conversationIDKey = obj[KEY_CONVERSATION_ID_KEY]?.jsonPrimitive?.content ?: return null,
        createdDate = Date(obj[KEY_CREATED_DATE]?.jsonPrimitive?.longOrNull ?: return null),
        fromAccountID = obj[KEY_FROM_ACCOUNT_ID]?.jsonPrimitive?.content ?: return null,
        id = id,
        isPenPalsConversation = obj[KEY_IS_PEN_PALS]?.jsonPrimitive?.booleanOrNull ?: false,
        payload = payload,
        recipientUserIDs = obj[KEY_RECIPIENT_USER_IDS]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
        attemptCount = obj[KEY_ATTEMPT_COUNT]?.jsonPrimitive?.content?.toIntOrNull() ?: 1,
        lastAttemptDate = obj[KEY_LAST_ATTEMPT_DATE]?.jsonPrimitive?.longOrNull?.let { Date(it) },
        reservedRemoteID = (obj[KEY_RESERVED_REMOTE_ID] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content,
        state = state,
        transcription = (obj[KEY_TRANSCRIPTION] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content,
    )
}

private fun decodePayload(obj: JsonObject): OutboxEntry.Payload? {
    val payloadObject = obj[KEY_PAYLOAD]?.jsonObject
    if (payloadObject != null) {
        return when (payloadObject[KEY_PAYLOAD_TYPE]?.jsonPrimitive?.content) {
            PAYLOAD_AUDIO ->
                payloadObject[KEY_INPUT_FILE_NAME]?.jsonPrimitive?.content?.let { OutboxEntry.Payload.Audio(it) }
            PAYLOAD_MEDIA -> {
                val fileName = payloadObject[KEY_FILE_NAME]?.jsonPrimitive?.content ?: return null
                val fileExtension =
                    MediaFileExtension.from(payloadObject[KEY_FILE_EXTENSION]?.jsonPrimitive?.content ?: return null)
                        ?: return null
                OutboxEntry.Payload.Media(fileName, fileExtension)
            }
            PAYLOAD_TEXT ->
                payloadObject[KEY_VALUE]?.jsonPrimitive?.content?.let { OutboxEntry.Payload.Text(it) }
            else -> null
        }
    }

    // Legacy shape: mediaRelativePath -> Media, otherwise text -> Text.
    val mediaRelativePath = (obj[KEY_MEDIA_RELATIVE_PATH] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content
    if (mediaRelativePath != null) {
        val fileName = mediaRelativePath.substringAfterLast("/")
        val fileExtension = MediaFileExtension.from(fileName.substringAfterLast(".")) ?: return null
        return OutboxEntry.Payload.Media(fileName, fileExtension)
    }

    return OutboxEntry.Payload.Text(obj[KEY_TEXT]?.jsonPrimitive?.content ?: "")
}

// MARK: - Keys

private const val KEY_ID = "id"
private const val KEY_CONVERSATION_ID_KEY = "conversationIDKey"
private const val KEY_FROM_ACCOUNT_ID = "fromAccountID"
private const val KEY_RECIPIENT_USER_IDS = "recipientUserIDs"
private const val KEY_IS_PEN_PALS = "isPenPalsConversation"
private const val KEY_CREATED_DATE = "createdDate"
private const val KEY_ATTEMPT_COUNT = "attemptCount"
private const val KEY_LAST_ATTEMPT_DATE = "lastAttemptDate"
private const val KEY_RESERVED_REMOTE_ID = "reservedRemoteID"
private const val KEY_STATE = "state"
private const val KEY_TRANSCRIPTION = "transcription"
private const val KEY_PAYLOAD = "payload"

private const val KEY_PAYLOAD_TYPE = "type"
private const val KEY_INPUT_FILE_NAME = "inputFileName"
private const val KEY_FILE_NAME = "fileName"
private const val KEY_FILE_EXTENSION = "fileExtension"
private const val KEY_VALUE = "value"

private const val PAYLOAD_AUDIO = "audio"
private const val PAYLOAD_MEDIA = "media"
private const val PAYLOAD_TEXT = "text"

// Legacy keys.
private const val KEY_TEXT = "text"
private const val KEY_MEDIA_RELATIVE_PATH = "mediaRelativePath"
