//
//  MediaMessageServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.sources.message.services

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.common.models.ImageFileExtension
import us.neotechnica.panther.modules.common.models.MediaFile
import us.neotechnica.panther.modules.common.models.MediaFileExtension
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.AppException
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import java.util.Date

class MediaMessageServiceTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    @Before
    fun setUp() {
        environment.install("media-message-service-test")
        environment.database.getValuesResults["messages/$MESSAGE_ID/contentType"] = MEDIA_CONTENT_TYPE
        environment.database.getValuesResults["messages"] =
            mapOf(MESSAGE_ID to mapOf("contentType" to MEDIA_CONTENT_TYPE))
    }

    // MARK: - Upload

    @Test
    fun `an upload proceeds when the existence check fails`() =
        runTest {
            environment.storage.itemExistsFailure = failure("Existence check failed.")
            environment.writeFile("staging/photo.jpg")
            val staged = MediaFile("staging/photo.jpg", name = "photo", fileExtension = MediaFileExtension.Image(ImageFileExtension.JPG))
            val hostedPath = "media/${staged.encodedHash.take(SHORTENED_HASH_LENGTH)}.jpg"

            MediaMessageService.uploadMediaComponent(
                staged,
                message = SessionTestEnvironment.message(id = MESSAGE_ID, fromAccountID = "me", sentDate = Date()),
            )

            assertEquals(listOf(hostedPath), environment.storage.uploadedPaths.map { it.first })
            assertTrue(FileStore.exists(hostedPath))
            assertFalse(FileStore.exists("staging/photo.jpg"))
        }

    // MARK: - Delete

    @Test
    fun `a missing thumbnail is tolerated but another thumbnail failure is thrown`() =
        runTest {
            environment.storage.deleteFailures[THUMBNAIL_PATH] = notFound()

            MediaMessageService.deleteMediaComponent(MESSAGE_ID)
            assertEquals(listOf(MEDIA_PATH), environment.storage.deletedPaths)

            environment.storage.deleteFailures[THUMBNAIL_PATH] = failure("Thumbnail delete failed.")
            val thrown = runCatching { MediaMessageService.deleteMediaComponent(MESSAGE_ID) }.exceptionOrNull()

            assertEquals("Thumbnail delete failed.", (thrown as Exception).descriptor)
            assertNull(thrown.underlyingExceptions)
        }

    @Test
    fun `a failed reference scan skips the deletion`() =
        runTest {
            environment.database.getValuesFailures["messages"] = failure("Scan failed.")

            MediaMessageService.deleteMediaComponent(MESSAGE_ID)

            assertTrue(environment.storage.deletedPaths.isEmpty())
        }

    @Test
    fun `every delete failure is compiled into the thrown exception`() =
        runTest {
            environment.storage.deleteFailures[MEDIA_PATH] = failure("Primary delete failed.")
            environment.storage.deleteFailures[THUMBNAIL_PATH] = failure("Thumbnail delete failed.")

            val thrown = runCatching { MediaMessageService.deleteMediaComponent(MESSAGE_ID) }.exceptionOrNull()

            assertEquals("Thumbnail delete failed.", (thrown as Exception).descriptor)
            assertEquals(listOf("Primary delete failed."), thrown.underlyingExceptions?.map { it.descriptor })
        }

    // MARK: - Auxiliary

    private fun failure(descriptor: String): Exception = Exception(descriptor, metadata = ExceptionMetadata(this))

    // Storage stamps the catalogued code on its not-found failures.
    private fun notFound(): Exception =
        Exception(
            "No item exists at the specified key path.",
            userInfo = mapOf("StaticErrorCode" to AppException.Networking.Storage.storageItemDoesNotExist.errorCode),
            metadata = ExceptionMetadata(this),
        )

    private companion object {
        const val MEDIA_CONTENT_TYPE = "image/jpeg – -FixtureMediaID000001 – jpg"
        const val MEDIA_PATH = "media/-FixtureMediaID000001.jpg"
        const val MESSAGE_ID = "-FixtureMessage000002"
        const val SHORTENED_HASH_LENGTH = 32
        const val THUMBNAIL_PATH = "media/-FixtureMediaID000001-thumbnail.jpeg"
    }
}
