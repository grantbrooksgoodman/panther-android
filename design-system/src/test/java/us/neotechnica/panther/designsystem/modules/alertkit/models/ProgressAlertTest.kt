//
//  ProgressAlertTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.alertkit.services.PresentedAlert
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressAlertTest {
    // MARK: - Setup

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        Persistent.initializeForTesting()
        AlertPresenter.dismiss()
    }

    @After
    fun tearDown() {
        AlertPresenter.dismiss()
        Dispatchers.resetMain()
    }

    // MARK: - Tests

    @Test
    fun `a completed flow dismisses the alert and returns`() =
        runTest(dispatcher) {
            val alert = ProgressAlert(message = "Working", cancelButtonTitle = "Cancel")
            val gate = CompletableDeferred<Unit>()
            var returned = false

            launch {
                alert.present(
                    observing =
                        flow {
                            emit(0.25)
                            gate.await()
                            emit(0.5)
                        },
                    translating = emptyList(),
                )
                returned = true
            }

            runCurrent()
            assertTrue(AlertPresenter.current.value is PresentedAlert.Progress)

            gate.complete(Unit)
            advanceTimeBy(500)
            assertTrue(returned)
            assertNull(AlertPresenter.current.value)
        }

    @Test
    fun `a cancel tap throws a cancellation exception`() =
        runTest(dispatcher) {
            val alert = ProgressAlert(message = "Working", cancelButtonTitle = "Cancel")
            var failure: Throwable? = null

            launch {
                try {
                    alert.present(
                        observing =
                            flow {
                                emit(0.1)
                                CompletableDeferred<Unit>().await()
                            },
                        translating = emptyList(),
                    )
                } catch (throwable: Throwable) {
                    failure = throwable
                }
            }

            runCurrent()
            val presented = AlertPresenter.current.value as PresentedAlert.Progress
            presented.onCancel?.invoke()
            advanceTimeBy(500)

            assertTrue(failure is CancellationException)
            assertNull(AlertPresenter.current.value)
        }

    @Test
    fun `a failing flow dismisses the alert and rethrows`() =
        runTest(dispatcher) {
            val alert = ProgressAlert(message = "Working")
            var failure: Throwable? = null

            launch {
                try {
                    alert.present(
                        observing =
                            flow {
                                emit(0.1)
                                error("Boom")
                            },
                        translating = emptyList(),
                    )
                } catch (throwable: Throwable) {
                    failure = throwable
                }
            }

            advanceTimeBy(500)
            assertEquals("Boom", failure?.message)
            assertNull(AlertPresenter.current.value)
        }

    @Test
    fun `withdrawing a queued alert throws without starting the operation`() =
        runTest(dispatcher) {
            AlertPresenter.present(standardAlert("Blocking"))
            runCurrent()

            val alert = ProgressAlert(message = "Queued")
            var started = false
            val job =
                launch {
                    alert.present(
                        observing =
                            flow {
                                started = true
                                emit(1.0)
                            },
                        translating = emptyList(),
                    )
                }

            runCurrent()
            job.cancel()
            runCurrent()

            AlertPresenter.dismiss()
            advanceTimeBy(500)

            assertTrue(job.isCancelled)
            assertFalse(started)
            assertNull(AlertPresenter.current.value)
        }

    @Test
    fun `dismissing before presentation throws without starting the operation`() =
        runTest(dispatcher) {
            AlertPresenter.present(standardAlert("Blocking"))
            runCurrent()

            val alert = ProgressAlert(message = "Queued")
            var started = false
            var failure: Throwable? = null
            launch {
                try {
                    alert.present(
                        observing =
                            flow {
                                started = true
                                emit(1.0)
                            },
                        translating = emptyList(),
                    )
                } catch (throwable: Throwable) {
                    failure = throwable
                }
            }

            runCurrent()
            alert.dismiss()
            AlertPresenter.dismiss()
            advanceTimeBy(500)

            assertTrue(failure is CancellationException)
            assertFalse(started)
            assertNull(AlertPresenter.current.value)
        }

    @Test
    fun `an out of band dismissal lets the operation finish`() =
        runTest(dispatcher) {
            val alert = ProgressAlert(message = "Working")
            val gate = CompletableDeferred<Unit>()
            var returned = false

            launch {
                alert.present(
                    observing =
                        flow {
                            emit(0.5)
                            gate.await()
                            emit(1.0)
                        },
                    translating = emptyList(),
                )
                returned = true
            }

            runCurrent()
            assertTrue(AlertPresenter.current.value is PresentedAlert.Progress)

            AlertPresenter.dismiss()
            advanceTimeBy(500)
            assertFalse(returned)

            gate.complete(Unit)
            advanceTimeBy(500)
            assertTrue(returned)
            assertNull(AlertPresenter.current.value)
        }

    // MARK: - Auxiliary

    private fun standardAlert(title: String): PresentedAlert.Standard =
        PresentedAlert.Standard(
            title = title,
            message = null,
            actions = listOf(Action("OK") {}),
        ) {}
}
