//
//  CoreStorageTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.storage

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.interfaces.NetworkActivityIndicatorDelegate
import us.neotechnica.panther.networking.modules.storage.models.HostedItemMetadata
import us.neotechnica.panther.networking.modules.storage.services.CoreStorage
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import kotlin.time.Duration.Companion.seconds

class CoreStorageTest {
    // MARK: - Types

    private class CountingIndicatorDelegate : NetworkActivityIndicatorDelegate {
        var hides = 0
        var shows = 0

        override fun hide() {
            hides += 1
        }

        override fun show() {
            shows += 1
        }
    }

    // MARK: - Tests

    @Test
    fun `a precondition failure fails the transfer without touching the activity indicator`() =
        runTest {
            val indicator = CountingIndicatorDelegate()
            Networking.config.register(activityIndicatorDelegate = indicator)

            val failure =
                runCatching {
                    CoreStorage()
                        .uploadWithProgress(
                            ByteArray(1),
                            metadata = HostedItemMetadata(filePath = "probe/file.txt"),
                            prependingEnvironment = false,
                            timeout = 1.seconds,
                        ).collect()
                }.exceptionOrNull()

            assertTrue(failure is Exception)
            assertEquals(0, indicator.shows)
            assertEquals(0, indicator.hides)
        }
}
