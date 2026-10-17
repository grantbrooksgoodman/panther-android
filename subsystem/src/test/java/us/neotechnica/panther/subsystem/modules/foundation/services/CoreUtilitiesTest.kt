//
//  CoreUtilitiesTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.subsystem.AppSubsystem
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.ApplicationSuspensionDelegate
import java.io.File
import java.nio.file.Files
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalCoroutinesApi::class)
class CoreUtilitiesTest {
    // MARK: - Setup

    private lateinit var documentsDirectory: File
    private lateinit var applicationSupportDirectory: File

    @Before
    fun setUp() {
        documentsDirectory = Files.createTempDirectory("core-utilities-documents").toFile()
        applicationSupportDirectory = Files.createTempDirectory("core-utilities-application-support").toFile()
        FileStore.initializeForTesting(documentsDirectory, applicationSupportDirectory)
    }

    @After
    fun tearDown() {
        Task.resetScope()
        documentsDirectory.deleteRecursively()
        applicationSupportDirectory.deleteRecursively()
    }

    // MARK: - Tests

    @Test
    fun `exitGracefully suspends the app before the exit is scheduled`() {
        // The exit is scheduled on a scheduler this test never
        // advances, so the process keeps running.
        Task.setScope(CoroutineScope(StandardTestDispatcher()))
        var suspensions = 0
        AppSubsystem.delegates.registerApplicationSuspensionDelegate(
            object : ApplicationSuspensionDelegate {
                override fun suspendToHome() {
                    suspensions += 1
                }
            },
        )

        CoreUtilities.exitGracefully(terminateAfter = 1.hours)

        assertEquals(1, suspensions)
    }

    @Test
    fun `eraseApplicationSupportDirectory empties only that directory`() {
        File(applicationSupportDirectory, "archive.json").writeText("{}")
        File(applicationSupportDirectory, "nested").mkdirs()
        File(applicationSupportDirectory, "nested/file.txt").writeText("x")
        File(documentsDirectory, "document.txt").writeText("keep")

        CoreUtilities.eraseApplicationSupportDirectory()

        assertTrue(applicationSupportDirectory.listFiles().isNullOrEmpty())
        assertEquals(listOf("document.txt"), documentsDirectory.list()?.toList())
    }
}
