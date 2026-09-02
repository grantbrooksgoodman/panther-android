//
//  MessageService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.message.services

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.schema.message.models.HostedContentType
import us.neotechnica.panther.networking.modules.schema.message.models.MediaFile
import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.schema.message.models.TranslationReference
import us.neotechnica.panther.networking.modules.session.services.SessionStore
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.translator.models.Translation
import java.util.Date
import us.neotechnica.panther.networking.modules.translation.models.TranslationReference as HostedTranslationReference

/**
 * Reads [Message] records from the database, upserting each into the
 * [SessionStore]. Ported from the iOS `MessageService` read path.
 */
object MessageService {
    // MARK: - Properties

    private val database get() = Networking.config.databaseDelegate

    private const val MEDIA_ID_LENGTH = 32

    // MARK: - Methods

    /** Returns the messages with the given IDs, upserting them into the store. */
    suspend fun getMessages(ids: List<String>): List<Message> =
        coroutineScope {
            ids
                .map { id -> async { runCatching { getMessage(id) }.getOrNull() } }
                .awaitAll()
                .filterNotNull()
        }

    /** Returns the message with the given ID, upserting it into the store. */
    suspend fun getMessage(id: String): Message {
        val data: Map<String, Any?> = database.getValues("${NetworkPath.messages.rawValue}/$id")
        val childData = data.toMutableMap().apply { put(ID_KEY, id) }

        if (!Message.canDecode(childData)) {
            throw Exception(
                "Failed to decode message.",
                userInfo = mapOf("MessageID" to id),
                metadata = ExceptionMetadata(this),
            )
        }

        return Message.decode(childData).also { SessionStore.upsertMessages(setOf(it)) }
    }

    // MARK: - Message Creation

    /**
     * Builds a text message (generating an ID) without writing its node
     * to the database.
     *
     * The send path uses this so the message node write joins the atomic
     * fan-out in [ConversationSessionService][us.neotechnica.panther.networking.modules.session.services.ConversationSessionService].
     *
     * @param fromAccountID The sender's account identifier.
     * @param presetID A preset message identifier, or `null` to generate one.
     * @param translations The message's resolved translations.
     *
     * @return The built message, carrying its translations inline.
     *
     * @throws Exception if the arguments fail validation or an ID cannot
     *   be generated.
     */
    suspend fun buildTextMessage(
        fromAccountID: String,
        presetID: String?,
        translations: List<Translation>,
    ): Message {
        if (fromAccountID.isBangQualifiedEmpty ||
            translations.isEmpty() ||
            !translations.all { it.isWellFormed }
        ) {
            throw Exception("Passed arguments fail validation.", metadata = ExceptionMetadata(this))
        }

        val id =
            presetID ?: database.generateKey(NetworkPath.messages.rawValue)
                ?: throw Exception(
                    "Failed to generate key for new message.",
                    metadata = ExceptionMetadata(this),
                )

        return Message(
            id = id,
            fromAccountID = fromAccountID,
            contentType = HostedContentType.Text,
            translationReferences =
                translations.map { TranslationReference(HostedTranslationReference.from(it).hostingKey) },
            readReceipts = null,
            sentDate = Date(),
            translations = translations,
        )
    }

    /**
     * Builds a media message for [mediaFile] from the given account.
     *
     * The message's content type carries the media's content-hash
     * identifier and file extension; the media itself is uploaded
     * separately.
     *
     * @param fromAccountID The identifier of the sending account.
     * @param mediaFile The media file to send.
     *
     * @return The built media message.
     *
     * @throws Exception if the account identifier is empty or a message
     *   key cannot be generated.
     */
    suspend fun buildMediaMessage(
        fromAccountID: String,
        mediaFile: MediaFile,
    ): Message {
        if (fromAccountID.isBangQualifiedEmpty) {
            throw Exception("Passed arguments fail validation.", metadata = ExceptionMetadata(this))
        }

        val id =
            database.generateKey(NetworkPath.messages.rawValue)
                ?: throw Exception("Failed to generate key for new message.", metadata = ExceptionMetadata(this))

        return Message(
            id = id,
            fromAccountID = fromAccountID,
            contentType =
                HostedContentType.Media(
                    id = mediaFile.encodedHash.take(MEDIA_ID_LENGTH),
                    fileExtension = mediaFile.fileExtension,
                ),
            translationReferences = null,
            readReceipts = null,
            sentDate = Date(),
            translations = null,
        )
    }

    // MARK: - Companion

    private const val ID_KEY = "id"
}
