//
//  DevModeActionsTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle.developermode

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.alertkit.services.PresentedAlert
import us.neotechnica.panther.designsystem.modules.developermode.interfaces.DevModeAppActionDelegate
import us.neotechnica.panther.designsystem.modules.developermode.models.DevModeAction
import us.neotechnica.panther.designsystem.modules.developermode.services.DevModeService
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.networking.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.support.SessionTestEnvironment
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.state.services.SessionStore

/**
 * Exercises the app-domain Developer Mode actions: the top-level
 * ordering, the app-domain sheet's `Back` item, the nested option
 * sheets, and the Danger Zone rows gated on the current user.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DevModeActionsTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        environment.install("dev-mode-actions-test")
        AlertPresenter.dismiss()
        DevModeService.registerAppActionDelegate(AppActions)
    }

    @After
    fun tearDown() {
        DevModeService.registerAppActionDelegate(object : DevModeAppActionDelegate {
            override val appActions: List<DevModeAction> = emptyList()
        })
        AlertPresenter.dismiss()
        Dispatchers.resetMain()
    }

    private fun presentedSheet(): PresentedAlert.ActionSheet = AlertPresenter.current.value as PresentedAlert.ActionSheet

    // MARK: - Tests

    @Test
    fun `the app actions list UI Options, User Options, and the destructive Danger Zone in order`() {
        val actions = AppActions.appActions

        assertEquals(listOf("UI Options", "User Options", "Danger Zone"), actions.map { it.title })
        assertEquals(listOf(false, false, true), actions.map { it.isDestructive })
    }

    @Test
    fun `the app-domain sheet lists the service's app actions, inserted ones included, and backs out to the menu`() =
        runTest(dispatcher) {
            DevModeService.insertAction(DevModeAction(title = "Inserted") {}, at = 0)

            AppActions.presentActionSheet()
            advanceUntilIdle()

            val sheet = presentedSheet()
            assertEquals("Developer Mode Options", sheet.title)
            assertEquals(listOf("Inserted", "UI Options", "User Options", "Danger Zone"), sheet.actions.map { it.title })
            assertEquals("Back", sheet.cancelButtonTitle)
        }

    @Test
    fun `UI Options offers the forced update modal and backs out`() =
        runTest(dispatcher) {
            UIOptions.uiOptionsAction.perform()
            advanceUntilIdle()

            val sheet = presentedSheet()
            assertEquals("UI Options", sheet.title)
            assertEquals(listOf("Trigger Forced Update Modal"), sheet.actions.map { it.title })
            assertEquals("Back", sheet.cancelButtonTitle)
        }

    @Test
    fun `User Options offers setting the current user ID and backs out`() =
        runTest(dispatcher) {
            UserOptions.userOptionsAction.perform()
            advanceUntilIdle()

            val sheet = presentedSheet()
            assertEquals("User Options", sheet.title)
            assertEquals(listOf("Set Current User ID"), sheet.actions.map { it.title })
            assertEquals("Back", sheet.cancelButtonTitle)
        }

    @Test
    fun `the Danger Zone offers only the push token reset without a current user`() =
        runTest(dispatcher) {
            AppActions.appActions.last().perform()
            advanceUntilIdle()

            val sheet = presentedSheet()
            assertEquals("Danger Zone", sheet.title)
            assertEquals("Exercise caution when using these options.", sheet.message)
            assertEquals(listOf("Reset Push Tokens"), sheet.actions.map { it.title })
            assertTrue(sheet.actions.all { it.style.name.startsWith("DESTRUCTIVE") })
        }

    @Test
    fun `the Danger Zone adds the language history clear first and the conversation deletion second`() =
        runTest(dispatcher) {
            val conversation = SessionTestEnvironment.conversation("c1", listOf("me", "a"))
            SessionStore.upsertConversation(conversation)
            environment.signIn(
                User(
                    id = "me",
                    aiEnhancedTranslationsEnabled = false,
                    blockedUserIDs = null,
                    conversationIDs = listOf(conversation.id),
                    deviceID = "device",
                    isPenPalsParticipant = false,
                    languageCode = "en",
                    messageRecipientConsentRequired = false,
                    phoneNumber = PhoneNumber("5550000001"),
                    previousLanguageCodes = listOf("es"),
                    pushTokens = null,
                ),
            )

            AppActions.appActions.last().perform()
            advanceUntilIdle()

            assertEquals(
                listOf("Clear Previous Language Codes", "Delete Conversations", "Reset Push Tokens"),
                presentedSheet().actions.map { it.title },
            )
        }
}
