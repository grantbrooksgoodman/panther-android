//
//  ConversationCellViewServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.alertkit.services.PresentedAlert
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.localization.models.LocalizationSource
import us.neotechnica.panther.subsystem.modules.localization.services.LocalizedStringResolver
import java.io.File

/**
 * Exercises the user-info alert: the language line names the exonym
 * with the uppercased code, and an unknown code falls back to the
 * code alone.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationCellViewServiceTest {
    // MARK: - Setup

    private val dispatcher = StandardTestDispatcher()
    private val environment = MessageDecodeEnvironment()

    private val service =
        ConversationCellViewService(
            ConversationCellViewService.Dependencies(
                isDeveloperModeEnabled = { false },
                localizedRegionName = { "Spain" },
            ),
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        environment.install("conversation-cell-view-service-test")
        AlertPresenter.dismiss()

        LocalizedStringResolver.initializeForTesting { source ->
            val root = if (source == LocalizationSource.APP) "src/main/assets" else "../subsystem/src/main/assets"
            val json = Json.parseToJsonElement(File(root, source.assetName).readText()).jsonObject
            json.mapValues { (_, languages) ->
                languages.jsonObject.mapValues { (_, value) -> value.jsonPrimitive.content }
            }
        }

        CoreUtilities.setLanguageCode("en")
        RuntimeStorage.languageCodeDictionary = mapOf("en" to "English", "es" to "Spanish (Español)")
    }

    @After
    fun tearDown() {
        RuntimeStorage.languageCodeDictionary = null
        LocalizedStringResolver.clearCache()
        AlertPresenter.dismiss()
        Dispatchers.resetMain()
    }

    private fun presentedAlert(): PresentedAlert.Standard? = AlertPresenter.current.value as? PresentedAlert.Standard

    // MARK: - Tests

    @Test
    fun `the language line names the exonym with the uppercased code`() =
        runTest(dispatcher) {
            service.presentUserInfoAlert(SessionTestEnvironment.user(id = "a", languageCode = "es"))
            advanceUntilIdle()

            assertEquals("Language: Spanish (ES)\nRegion: Spain", presentedAlert()?.message)
            assertEquals(listOf("Dismiss"), presentedAlert()?.actions?.map { it.title })
        }

    @Test
    fun `an unknown language code falls back to the uppercased code`() =
        runTest(dispatcher) {
            service.presentUserInfoAlert(SessionTestEnvironment.user(id = "a", languageCode = "zz"))
            advanceUntilIdle()

            assertEquals("Language: ZZ\nRegion: Spain", presentedAlert()?.message)
        }
}
