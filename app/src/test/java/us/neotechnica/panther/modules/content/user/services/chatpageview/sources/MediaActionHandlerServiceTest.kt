//
//  MediaActionHandlerServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services.chatpageview.sources

import android.net.TestUri
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.modules.common.models.ContentPickerResult
import us.neotechnica.panther.modules.common.services.sources.contentpicker.CameraPickerService
import us.neotechnica.panther.modules.common.services.sources.contentpicker.ContentPickerService
import us.neotechnica.panther.modules.common.services.sources.contentpicker.DocumentPickerService
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import java.io.File

/**
 * Exercises the media action handler's picker plumbing: each content
 * picker presents through its registered launcher, delivers its result,
 * failure, or cancellation once, and the document picker restricts its
 * types.
 */
class MediaActionHandlerServiceTest {
    // MARK: - Setup

    @After
    fun tearDown() {
        MediaActionHandlerService.onMediaMessageSent = null
        MediaActionHandlerService.setDependencies(null)
    }

    // MARK: - Tests

    @Test
    fun `a picker presents only through a registered launcher`() {
        val picker = CameraPickerService()
        var presentations = 0

        picker.present()
        assertFalse(picker.canPresent)
        assertEquals(0, presentations)

        val launch = { presentations += 1 }
        picker.registerLauncher(launch)
        picker.present()
        assertTrue(picker.canPresent)
        assertEquals(1, presentations)

        picker.unregisterLauncher(launch)
        picker.present()
        assertEquals(1, presentations)
    }

    @Test
    fun `a picker delivers its dismissal once and clears the handler`() {
        val picker = CameraPickerService()
        var dismissals = 0
        picker.onDismiss { dismissals += 1 }

        picker.handleResult(null)
        picker.handleResult(null)

        assertEquals(1, dismissals)
    }

    @Test
    fun `a cancellation delivers null and a failure delivers the exception`() {
        val picker = DocumentPickerService()
        var result: Result<ContentPickerResult>? = null
        var wasCalled = false
        picker.onDismiss {
            wasCalled = true
            result = it
        }
        picker.handleResult(null)
        assertTrue(wasCalled)
        assertNull(result)

        val exception = Exception("Failed to access security-scoped URL.", metadata = ExceptionMetadata(this))
        picker.onDismiss { result = it }
        picker.handleFailure(exception)
        assertEquals(exception, result?.exceptionOrNull())
    }

    @Test
    fun `the document picker restricts selection to the supported types`() {
        assertEquals(
            listOf("image/jpeg", "image/png", "video/mp4", "application/pdf", "text/plain"),
            DocumentPickerService.allowedMimeTypes.toList(),
        )
    }

    @Test
    fun `the content picker service exposes three distinct pickers`() {
        val pickers = setOf(ContentPickerService.camera, ContentPickerService.document, ContentPickerService.media)
        assertEquals(3, pickers.size)
    }

    @Test
    fun `a transcode failure throws instead of staging the original`() =
        runBlocking {
            FileStore.initializeForTesting(File(System.getProperty("java.io.tmpdir"), "media-action-handler-test-${System.nanoTime()}"))
            MediaActionHandlerService.setDependencies(
                MediaActionHandlerService.Dependencies(
                    compressVideo = { _, _ -> throw Exception("Failed to create export session.", metadata = ExceptionMetadata(this)) },
                ),
            )

            val failure = runCatching { MediaActionHandlerService.processVideo(TestUri.allocate()) }.exceptionOrNull()

            assertEquals("Failed to create export session.", (failure as? Exception)?.descriptor)
            assertFalse(FileStore.exists("media/video.mp4"))
        }

    @Test
    fun `an image that cannot meet the target size fails to compress`() {
        val oversized = ByteArray(2 * 1024 * 1024)
        val failure = runCatching { MediaActionHandlerService.jpegDataCompressedToKB(1000) { oversized } }.exceptionOrNull()
        assertEquals("Failed to compress image.", (failure as? Exception)?.descriptor)

        val small = ByteArray(10)
        assertSame(small, MediaActionHandlerService.jpegDataCompressedToKB(1000) { small })
    }

    @Test
    fun `no picker is presented until the attach sheet routes to one`() {
        assertFalse(MediaActionHandlerService.isPresentingPickerController)
        assertNull(MediaActionHandlerService.onMediaMessageSent)
    }
}
