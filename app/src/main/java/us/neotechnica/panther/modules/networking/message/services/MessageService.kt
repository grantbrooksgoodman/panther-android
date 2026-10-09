//
//  MessageService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.message.services

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import us.neotechnica.panther.bundle.media
import us.neotechnica.panther.bundle.messages
import us.neotechnica.panther.modules.common.extensions.shortened
import us.neotechnica.panther.modules.common.models.AudioFileExtension
import us.neotechnica.panther.modules.networking.message.models.HostedContentType
import us.neotechnica.panther.modules.networking.message.models.MediaFile
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.message.models.RichMessageContent
import us.neotechnica.panther.modules.networking.message.serializable.decode
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.database.interfaces.getValues
import us.neotechnica.panther.networking.modules.translation.extensions.reference
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.Coalescer
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.translator.models.Translation
import java.util.Date

/**
 * The service that creates and retrieves messages.
 *
 * [MessageService] builds and writes message nodes and fetches
 * messages by identifier. It delegates content transfer to its
 * [audio] and [media] sub-services.
 */
object MessageService {
    // MARK: - Properties

    /** The service that uploads, downloads, and deletes audio message content. */
    val audio: AudioMessageService
        get() = AudioMessageService

    /** The service that uploads, downloads, and deletes media message content. */
    val media: MediaMessageService
        get() = MediaMessageService

    private val coalescer = Coalescer<String, Message>()

    private val database get() = Networking.config.databaseDelegate

    // MARK: - Message Creation

    /**
     * Builds a message (generating an ID, uploading media to
     * Storage) without writing the message node to the database.
     *
     * Callers on the send path use this so the message node write
     * joins the atomic fan-out in
     * [ConversationSessionService][us.neotechnica.panther.modules.session.entity.services.ConversationSessionService].
     *
     * @param fromAccountID The identifier of the sending account.
     * @param presetID A preset message identifier, or `null` to
     *   generate one.
     * @param richContent The message's rich content, or `null` for a
     *   text message.
     * @param sentDate The date the message was sent.
     * @param translations The message's translations, or `null` for
     *   a message without text.
     *
     * @return The built message.
     *
     * @throws Exception if the arguments fail validation, an ID cannot
     *   be generated, or an upload fails.
     */
    suspend fun buildMessage(
        fromAccountID: String,
        presetID: String? = null,
        richContent: RichMessageContent?,
        sentDate: Date = Date(),
        translations: List<Translation>?,
    ): Message {
        if (fromAccountID.isBangQualifiedEmpty ||
            (richContent == null && translations == null) ||
            !(translations?.all { it.isWellFormed } ?: true)
        ) {
            throw Exception(
                "Passed arguments fail validation.",
                metadata = ExceptionMetadata(this),
            )
        }

        val id =
            presetID ?: database.generateKey(NetworkPath.messages.rawValue)
                ?: throw Exception(
                    "Failed to generate key for new message.",
                    metadata = ExceptionMetadata(this),
                )

        var contentType: HostedContentType = HostedContentType.Text
        val mediaComponent = richContent?.mediaComponent
        if (richContent?.audioComponents != null) {
            contentType = HostedContentType.Audio(AudioFileExtension.M4A)
        } else if (mediaComponent != null) {
            contentType =
                HostedContentType.Media(
                    id = mediaComponent.encodedHash.shortened,
                    fileExtension = mediaComponent.fileExtension,
                )
        }

        val mockMessage =
            Message(
                id = id,
                fromAccountID = fromAccountID,
                contentType = contentType,
                richContent = richContent,
                translationReferences = translations?.map { it.reference },
                translations = translations,
                readReceipts = null,
                sentDate = sentDate,
            )

        return when (mockMessage.contentType) {
            is HostedContentType.Audio -> {
                mockMessage.audioComponents
                    ?: throw Exception(
                        "Failed to find audio components for audio message creation.",
                        metadata = ExceptionMetadata(this),
                    )

                mockMessage
            }

            is HostedContentType.Media -> {
                mediaComponent
                    ?: throw Exception(
                        "Failed to find media component for media message creation.",
                        metadata = ExceptionMetadata(this),
                    )

                val mediaFileID = mediaComponent.encodedHash.shortened
                media.uploadMediaComponent(
                    mediaComponent,
                    mockMessage,
                )

                mockMessage.copy(
                    richContent =
                        RichMessageContent.Media(
                            MediaFile(
                                "${NetworkPath.media.rawValue}/$mediaFileID.${mediaComponent.fileExtension.rawValue}",
                                name = mediaFileID,
                                fileExtension = mediaComponent.fileExtension,
                            ),
                        ),
                )
            }

            HostedContentType.Text -> mockMessage
        }
    }

    /**
     * Builds a message and writes its node to the database.
     *
     * Use [buildMessage] on the send path where the message node
     * write should join the atomic fan-out instead.
     *
     * @param fromAccountID The identifier of the sending account.
     * @param richContent The message's rich content, or `null` for a
     *   text message.
     * @param sentDate The date the message was sent.
     * @param translations The message's translations, or `null` for
     *   a message without text.
     *
     * @return The created message.
     *
     * @throws Exception if the message cannot be built or written.
     */
    suspend fun createMessage(
        fromAccountID: String,
        richContent: RichMessageContent?,
        sentDate: Date = Date(),
        translations: List<Translation>?,
    ): Message {
        val message =
            buildMessage(
                fromAccountID = fromAccountID,
                richContent = richContent,
                sentDate = sentDate,
                translations = translations,
            )

        database.updateChildValues(
            "${NetworkPath.messages.rawValue}/${message.id}",
            message.encoded.filterKeys { it != Message.SerializableKey.ID.rawValue },
        )

        return message
    }

    // MARK: - Retrieval by ID

    /**
     * Returns the message with the given identifier.
     *
     * @param id The identifier of the message to fetch.
     *
     * @return The message.
     *
     * @throws Exception if no identifier is provided or the message
     *   cannot be fetched or decoded.
     */
    suspend fun getMessage(id: String): Message {
        val userInfo = mapOf("MessageID" to id)

        if (id.isBangQualifiedEmpty) {
            throw Exception(
                "No ID provided.",
                metadata = ExceptionMetadata(this),
            ).appending(userInfo)
        }

        // Coalesce concurrent fetches of the same message so it is
        // fetched and decoded only once.
        return coalescer(id) { fetchMessage(id) }
    }

    /**
     * Returns the messages with the given identifiers, fetched
     * concurrently.
     *
     * @param ids The identifiers of the messages to fetch.
     *
     * @return The messages.
     *
     * @throws Exception if no identifiers are provided or any message
     *   cannot be fetched.
     */
    suspend fun getMessages(ids: List<String>): List<Message> {
        val userInfo = mapOf("MessageIDs" to ids)

        if (ids.isBangQualifiedEmpty) {
            throw Exception(
                "No IDs provided.",
                metadata = ExceptionMetadata(this),
            ).appending(userInfo)
        }

        try {
            return coroutineScope {
                ids.map { async { getMessage(it) } }.awaitAll()
            }
        } catch (exception: Exception) {
            throw exception.appending(userInfo)
        }
    }

    // MARK: - Auxiliary

    private suspend fun fetchMessage(id: String): Message {
        val userInfo = mapOf("MessageID" to id)

        val data: Map<String, Any?>
        try {
            data = database.getValues<Map<String, Any?>>("${NetworkPath.messages.rawValue}/$id")
        } catch (exception: Exception) {
            throw exception.appending(userInfo)
        }

        val childData = data.toMutableMap().apply { put(Message.SerializableKey.ID.rawValue, id) }
        try {
            return Message.decode(childData)
        } catch (exception: Exception) {
            throw exception.appending(userInfo)
        }
    }
}
