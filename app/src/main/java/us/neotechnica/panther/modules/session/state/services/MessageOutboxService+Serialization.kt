//
//  MessageOutboxService+Serialization.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.state.services

import us.neotechnica.panther.modules.common.models.MediaFileExtension
import us.neotechnica.panther.modules.networking.sources.common.interfaces.asList
import us.neotechnica.panther.modules.session.state.models.OutboxEntry
import java.util.Date

// MARK: - Archive Codec

internal fun encodeEntry(entry: OutboxEntry): Map<String, Any?> =
    buildMap {
        put(KEY_ID, entry.id)
        put(KEY_CONVERSATION_ID_KEY, entry.conversationIDKey)
        put(KEY_FROM_ACCOUNT_ID, entry.fromAccountID)
        put(KEY_RECIPIENT_USER_IDS, entry.recipientUserIDs)
        put(KEY_IS_PEN_PALS, entry.isPenPalsConversation)
        put(KEY_CREATED_DATE, entry.createdDate.time)
        put(KEY_ATTEMPT_COUNT, entry.attemptCount)
        put(KEY_LAST_ATTEMPT_DATE, entry.lastAttemptDate?.time)
        put(KEY_RESERVED_REMOTE_ID, entry.reservedRemoteID)
        put(KEY_STATE, entry.state.rawValue)
        put(KEY_TRANSCRIPTION, entry.transcription)
        put(KEY_PAYLOAD, encodePayload(entry.payload))
    }

// An entry missing a required field is dropped.
internal fun decodeEntry(map: Map<String, Any?>): OutboxEntry? {
    val id = map[KEY_ID] as? String ?: return null
    val state = OutboxEntry.State.from(map[KEY_STATE] as? String ?: return null) ?: return null
    val payload = decodePayload(map) ?: return null

    return OutboxEntry(
        conversationIDKey = map[KEY_CONVERSATION_ID_KEY] as? String ?: return null,
        createdDate = Date((map[KEY_CREATED_DATE] as? Number ?: return null).toLong()),
        fromAccountID = map[KEY_FROM_ACCOUNT_ID] as? String ?: return null,
        id = id,
        isPenPalsConversation = map[KEY_IS_PEN_PALS] as? Boolean ?: return null,
        payload = payload,
        recipientUserIDs = map[KEY_RECIPIENT_USER_IDS].asList<String>() ?: return null,
        attemptCount = (map[KEY_ATTEMPT_COUNT] as? Number ?: return null).toInt(),
        lastAttemptDate = (map[KEY_LAST_ATTEMPT_DATE] as? Number)?.let { Date(it.toLong()) },
        reservedRemoteID = map[KEY_RESERVED_REMOTE_ID] as? String,
        state = state,
        transcription = map[KEY_TRANSCRIPTION] as? String,
    )
}

private fun encodePayload(payload: OutboxEntry.Payload): Map<String, Any?> =
    when (payload) {
        is OutboxEntry.Payload.Audio ->
            mapOf(KEY_PAYLOAD_TYPE to PAYLOAD_AUDIO, KEY_INPUT_FILE_NAME to payload.inputFileName)

        is OutboxEntry.Payload.Media ->
            mapOf(
                KEY_PAYLOAD_TYPE to PAYLOAD_MEDIA,
                KEY_FILE_NAME to payload.fileName,
                KEY_FILE_EXTENSION to payload.fileExtension.rawValue,
            )

        is OutboxEntry.Payload.Text ->
            mapOf(KEY_PAYLOAD_TYPE to PAYLOAD_TEXT, KEY_VALUE to payload.value)
    }

private fun decodePayload(map: Map<String, Any?>): OutboxEntry.Payload? {
    @Suppress("UNCHECKED_CAST")
    val payloadMap = map[KEY_PAYLOAD] as? Map<String, Any?> ?: return null
    return when (payloadMap[KEY_PAYLOAD_TYPE] as? String) {
        PAYLOAD_AUDIO -> (payloadMap[KEY_INPUT_FILE_NAME] as? String)?.let { OutboxEntry.Payload.Audio(it) }

        PAYLOAD_MEDIA -> {
            val fileName = payloadMap[KEY_FILE_NAME] as? String ?: return null
            val fileExtension = MediaFileExtension.from(payloadMap[KEY_FILE_EXTENSION] as? String ?: return null) ?: return null
            OutboxEntry.Payload.Media(fileName, fileExtension)
        }

        PAYLOAD_TEXT -> (payloadMap[KEY_VALUE] as? String)?.let { OutboxEntry.Payload.Text(it) }

        else -> null
    }
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
