//
//  ActionSheetTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.nonDefaultUnique
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.alertkit.services.PresentedAlert
import us.neotechnica.panther.translator.models.TranslationInput

@OptIn(ExperimentalCoroutinesApi::class)
class ActionSheetTest {
    // MARK: - Setup

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        AlertPresenter.dismiss()
    }

    @After
    fun tearDown() {
        AlertPresenter.dismiss()
        Dispatchers.resetMain()
    }

    // MARK: - Tests

    @Test
    fun `a cancel-styled action occupies the cancel slot without an automatic cancel`() =
        runTest(dispatcher) {
            var backSelected = false
            val sheet =
                ActionSheet(
                    title = "Options",
                    actions =
                        listOf(
                            Action("Open") {},
                            Action("Back", style = ActionStyle.CANCEL) { backSelected = true },
                        ),
                )

            val presentation = launch { sheet.present(translating = emptyList()) }
            advanceUntilIdle()

            val presented = AlertPresenter.current.value as PresentedAlert.ActionSheet
            assertEquals("Back", presented.cancelButtonTitle)
            assertEquals(listOf("Open"), presented.actions.map { it.title })

            presented.onCancel()
            advanceUntilIdle()
            assertTrue(backSelected)
            presentation.join()
        }

    @Test
    fun `nonDefaultUnique drops duplicates and the default action title`() {
        val inputs =
            listOf(
                TranslationInput("OK"),
                TranslationInput("Delete"),
                TranslationInput("Delete"),
                TranslationInput("Cancel"),
            )

        assertEquals(listOf("Delete", "Cancel"), inputs.nonDefaultUnique.map { it.value })
    }
}
