//
//  ChangeLanguagePageViewServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.alertkit.services.PresentedAlert
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import us.neotechnica.panther.modules.networking.sources.user.models.User
import us.neotechnica.panther.modules.networking.sources.user.remotelyupdatable.UserUpdatableKey
import us.neotechnica.panther.networking.modules.common.extensions.bangQualifiedEmptyList
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage

/**
 * Exercises the change language page service: the confirmation sheet's
 * exonym title and apply action, the atomic language write with its
 * history, the deferred progress display, and the reset and HUD
 * teardown – each through injected collaborators.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChangeLanguagePageViewServiceTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()
    private val dispatcher = StandardTestDispatcher()

    private var currentUser: User? = SessionTestEnvironment.user(id = "me", nationalNumber = "5550000001")
    private var resolveDelayMilliseconds = 0L
    private var updatedData: Map<UserUpdatableKey, Any>? = null
    private val steps = mutableListOf<String>()

    private val service =
        ChangeLanguagePageViewService(
            ChangeLanguagePageViewService.Dependencies(
                addOverlay = { steps.add("addOverlay") },
                currentUser = { currentUser },
                hideHUD = { steps.add("hideHUD") },
                presentActionSheet = { sheet, _ -> sheet.present(translating = emptyList()) },
                reset = { steps.add("reset") },
                resolveCurrentUser = {
                    steps.add("resolve")
                    if (resolveDelayMilliseconds > 0) delay(resolveDelayMilliseconds)
                },
                showProgressHUD = { steps.add("showProgressHUD") },
                updateValues = { user, data ->
                    steps.add("update")
                    updatedData = data
                    user
                },
            ),
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        environment.install("change-language-page-view-service-test")
        AlertPresenter.dismiss()
        RuntimeStorage.languageCodeDictionary = mapOf("en" to "English", "es" to "Spanish (Español)")
    }

    @After
    fun tearDown() {
        RuntimeStorage.languageCodeDictionary = null
        AlertPresenter.dismiss()
        Dispatchers.resetMain()
    }

    private fun presentedSheet(): PresentedAlert.ActionSheet? = AlertPresenter.current.value as? PresentedAlert.ActionSheet

    // MARK: - Confirmation

    @Test
    fun `confirming presents the exonym-titled sheet with a destructive apply action`() =
        runTest(dispatcher) {
            service.confirmButtonTapped("es")
            advanceUntilIdle()

            val sheet = presentedSheet()
            assertNotNull(sheet)
            assertEquals("Change Language to ⌘Spanish⌘", sheet?.title)
            assertEquals("You must restart the app for this to take effect.", sheet?.message)
            assertEquals(listOf("Apply & Exit"), sheet?.actions?.map { it.title })
            assertEquals(ActionStyle.DESTRUCTIVE_PREFERRED, sheet?.actions?.first()?.style)
        }

    @Test
    fun `an unknown code falls back to its uppercased form in the title`() =
        runTest(dispatcher) {
            service.confirmButtonTapped("zz")
            advanceUntilIdle()

            assertEquals("Change Language to ⌘ZZ⌘", presentedSheet()?.title)
        }

    // MARK: - Change Language

    @Test
    fun `applying writes the language and an empty history atomically, then resets and hides the HUD`() =
        runTest(dispatcher) {
            service.confirmButtonTapped("es")
            advanceUntilIdle()
            presentedSheet()?.onSelect(0)
            advanceUntilIdle()

            assertEquals(listOf("resolve", "update", "reset", "hideHUD"), steps)
            assertEquals("es", updatedData?.get(UserUpdatableKey.LANGUAGE_CODE))
            assertEquals(bangQualifiedEmptyList, updatedData?.get(UserUpdatableKey.PREVIOUS_LANGUAGE_CODES))
        }

    @Test
    fun `a slow resolution shows the progress display after one second`() =
        runTest(dispatcher) {
            resolveDelayMilliseconds = 2_000

            service.confirmButtonTapped("es")
            advanceUntilIdle()
            presentedSheet()?.onSelect(0)
            advanceTimeBy(1_500)

            assertEquals(listOf("resolve", "addOverlay", "showProgressHUD"), steps)

            advanceUntilIdle()
            assertEquals(listOf("resolve", "addOverlay", "showProgressHUD", "update", "reset", "hideHUD"), steps)
        }

    @Test
    fun `applying without a current user writes nothing`() =
        runTest(dispatcher) {
            currentUser = null

            service.confirmButtonTapped("es")
            advanceUntilIdle()
            presentedSheet()?.onSelect(0)
            advanceUntilIdle()

            assertNull(updatedData)
            assertEquals(emptyList<String>(), steps)
        }
}
