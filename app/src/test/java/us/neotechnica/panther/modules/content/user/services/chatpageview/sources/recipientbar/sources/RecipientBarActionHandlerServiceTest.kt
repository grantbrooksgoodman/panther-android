//
//  RecipientBarActionHandlerServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services.chatpageview.sources.recipientbar.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.bundle.contactAccessDenied
import us.neotechnica.panther.bundle.mismatchedHashAndCallingCode
import us.neotechnica.panther.bundle.navigators.ChatNavigatorState
import us.neotechnica.panther.bundle.navigators.ChatRoute
import us.neotechnica.panther.modules.common.models.Contact
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.common.models.NumberPair
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.common.services.sources.CommonPropertyLists
import us.neotechnica.panther.modules.common.services.sources.PermissionService
import us.neotechnica.panther.modules.common.services.sources.RegionDetailService
import us.neotechnica.panther.modules.content.user.extensions.isMock
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.recipientbar.RecipientBarService
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.AppException
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

/**
 * Exercises the recipient bar's action handler: the select-contact
 * permission flow, the submitted-text resolution, and the backspace
 * highlight-then-remove behaviour.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecipientBarActionHandlerServiceTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()

    private var contactPermissionStatus = PermissionService.PermissionStatus.GRANTED
    private var hasContacts = true
    private var presentedCTA = false
    private var presentedInvitationSuggestion = false
    private var requestedPermissionStatus = PermissionService.PermissionStatus.GRANTED
    private var syncException: Exception? = null
    private var syncCount = 0
    private var hiddenHUD = false
    private val routes = mutableListOf<Route>()

    private val handler =
        RecipientBarActionHandlerService(
            RecipientBarActionHandlerService.Dependencies(
                contactPermissionStatus = { contactPermissionStatus },
                hasContactsBesidesCurrentUser = { hasContacts },
                presentContactCTA = { presentedCTA = true },
                presentInvitationSuggestionPrompt = { presentedInvitationSuggestion = true },
                requestContactPermission = { requestedPermissionStatus },
                syncContactPairArchive = {
                    syncCount += 1
                    syncException?.let { throw it }
                },
                getUser = { throw Exception("No users with the provided phone number.", metadata = ExceptionMetadata(this)) },
                navigate = { routes.add(it) },
                hideHUD = { hiddenHUD = true },
                showProgressHUD = {},
            ),
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        environment.install("recipient-bar-action-handler-test")
        CommonPropertyLists.initializeForTesting(
            callingCodes = mapOf("US" to "1"),
            lookupTables = mapOf("10" to listOf("1")),
        )
        RegionDetailService.clearCache()
        RecipientBarService.reset()
    }

    @After
    fun tearDown() {
        // A blank submission reloads the suggestions table on the Main
        // scope; join that resolution before the Main dispatcher goes
        // away, or its continuation resumes into the missing platform
        // dispatcher during a later test.
        runBlocking { RecipientBarService.tableView.resolveContactPairs() }
        Dispatchers.resetMain()
        CommonPropertyLists.clearCache()
        RegionDetailService.clearCache()
        RecipientBarService.reset()
    }

    // MARK: - Select Contact Button

    @Test
    fun `a denied permission request presents the call to action after a delay`() =
        runBlocking {
            contactPermissionStatus = PermissionService.PermissionStatus.UNKNOWN
            requestedPermissionStatus = PermissionService.PermissionStatus.DENIED

            handler.selectContactButtonTapped()

            assertFalse(presentedCTA)
            delay(CTA_SETTLE_MILLISECONDS)
            assertTrue(presentedCTA)
            assertTrue(routes.isEmpty())
            assertTrue(hiddenHUD)
        }

    @Test
    fun `a contact access denied failure presents the call to action`() =
        runBlocking {
            contactPermissionStatus = PermissionService.PermissionStatus.UNKNOWN
            val denied = RecipientBarActionHandlerService(dependenciesThrowingOnRequest(AppException.contactAccessDenied.errorCode))

            denied.selectContactButtonTapped()
            delay(CTA_SETTLE_MILLISECONDS)

            assertTrue(presentedCTA)
            assertTrue(routes.isEmpty())
        }

    @Test
    fun `a granted permission syncs the archive and presents the selector`() =
        runBlocking {
            contactPermissionStatus = PermissionService.PermissionStatus.UNKNOWN
            requestedPermissionStatus = PermissionService.PermissionStatus.GRANTED
            var tapped = false
            val granting =
                RecipientBarActionHandlerService(
                    RecipientBarActionHandlerService.Dependencies(
                        contactPermissionStatus = {
                            if (tapped) PermissionService.PermissionStatus.GRANTED else contactPermissionStatus
                        },
                        hasContactsBesidesCurrentUser = { true },
                        presentContactCTA = { presentedCTA = true },
                        presentInvitationSuggestionPrompt = { presentedInvitationSuggestion = true },
                        requestContactPermission = {
                            tapped = true
                            requestedPermissionStatus
                        },
                        syncContactPairArchive = { syncCount += 1 },
                        getUser = { throw Exception("unused", metadata = ExceptionMetadata(this)) },
                        navigate = { routes.add(it) },
                        hideHUD = { hiddenHUD = true },
                        showProgressHUD = {},
                    ),
                )

            granting.selectContactButtonTapped()

            assertEquals(1, syncCount)
            assertEquals(listOf<Route>(Route.Chat(ChatRoute.Sheet(ChatNavigatorState.SheetPaths.ContactSelector))), routes)
        }

    @Test
    fun `without contacts the sync runs, and an empty archive suggests an invitation`() =
        runBlocking {
            hasContacts = false

            handler.selectContactButtonTapped()

            assertEquals(1, syncCount)
            assertTrue(presentedInvitationSuggestion)
            assertTrue(routes.isEmpty())
            assertTrue(RecipientBarService.layout.isSelectContactButtonEnabled.value)
        }

    @Test
    fun `a mismatched hash sync failure is swallowed before the invitation suggestion`() =
        runBlocking {
            hasContacts = false
            syncException =
                Exception(
                    "Mismatched hash and calling code.",
                    userInfo =
                        mapOf(
                            Exception.UserInfo.STATIC_ERROR_CODE.rawValue to AppException.mismatchedHashAndCallingCode.errorCode,
                        ),
                    metadata = ExceptionMetadata(this),
                )

            handler.selectContactButtonTapped()

            assertTrue(presentedInvitationSuggestion)
        }

    @Test
    fun `with contacts the selector presents immediately`() =
        runBlocking {
            handler.selectContactButtonTapped()

            assertEquals(0, syncCount)
            assertEquals(listOf<Route>(Route.Chat(ChatRoute.Sheet(ChatNavigatorState.SheetPaths.ContactSelector))), routes)
        }

    // MARK: - Text Field Should Return

    @Test
    fun `a name becomes a mock recipient and an unknown number falls back to a mock`() =
        runBlocking {
            handler.textFieldShouldReturn("Bob")
            handler.textFieldShouldReturn("5558885555")

            val selected = RecipientBarService.contactSelectionUI.selectedContactPairs.value
            assertEquals(1, selected.size)
            assertTrue(selected.single().isMock)
            assertEquals("5558885555", selected.single().contact.firstName)
        }

    @Test
    fun `a known number resolves to its registered user`() =
        runBlocking {
            val user = SessionTestEnvironment.user(id = "bob", nationalNumber = "5558885555")
            val resolving =
                RecipientBarActionHandlerService(
                    RecipientBarActionHandlerService.Dependencies(
                        getUser = { user },
                        navigate = { routes.add(it) },
                        hideHUD = {},
                        showProgressHUD = {},
                    ),
                )

            resolving.textFieldShouldReturn("555-888-5555")

            val selected = RecipientBarService.contactSelectionUI.selectedContactPairs.value
            val userIDs =
                selected
                    .single()
                    .numberPairs
                    .flatMap { it.userIDs }
            assertEquals(listOf("bob"), userIDs)
            assertFalse(selected.single().isMock)
        }

    @Test
    fun `a blank submission removes mock recipients and hands focus to the input bar`() =
        runBlocking {
            handler.textFieldShouldReturn("Bob")

            handler.textFieldShouldReturn("   ")

            assertTrue(selectedContactPairs().isEmpty())
        }

    // MARK: - Superfluous Backspace

    @Test
    fun `the first backspace highlights the trailing recipient and the second removes it`() {
        val pair = contactPair(1)
        RecipientBarService.contactSelectionUI.selectContactPair(pair)
        val viewID = pair.contact.encodedHash

        handler.onSuperflousBackspace()
        assertTrue(RecipientBarService.contactSelectionUI.isHighlighted(viewID))

        handler.onSuperflousBackspace()
        assertTrue(selectedContactPairs().isEmpty())
    }

    // MARK: - Auxiliary

    private fun selectedContactPairs(): List<ContactPair> = RecipientBarService.contactSelectionUI.selectedContactPairs.value

    private fun dependenciesThrowingOnRequest(errorCode: String): RecipientBarActionHandlerService.Dependencies =
        RecipientBarActionHandlerService.Dependencies(
            contactPermissionStatus = { contactPermissionStatus },
            hasContactsBesidesCurrentUser = { hasContacts },
            presentContactCTA = { presentedCTA = true },
            presentInvitationSuggestionPrompt = { presentedInvitationSuggestion = true },
            requestContactPermission = {
                throw Exception(
                    "Access denied.",
                    userInfo = mapOf(Exception.UserInfo.STATIC_ERROR_CODE.rawValue to errorCode),
                    metadata = ExceptionMetadata(this),
                )
            },
            syncContactPairArchive = { syncCount += 1 },
            getUser = { throw Exception("unused", metadata = ExceptionMetadata(this)) },
            navigate = { routes.add(it) },
            hideHUD = { hiddenHUD = true },
            showProgressHUD = {},
        )

    private fun contactPair(index: Int): ContactPair {
        val phoneNumber =
            PhoneNumber(
                callingCode = "1",
                nationalNumberString = "555000${index.toString().padStart(4, '0')}",
                regionCode = "US",
                label = null,
                internalFormattedString = null,
            )
        return ContactPair(
            contact =
                Contact(
                    id = "contact-$index",
                    firstName = "User$index",
                    lastName = "",
                    phoneNumbers = listOf(phoneNumber),
                    imageData = null,
                ),
            numberPairs = listOf(NumberPair(phoneNumber = phoneNumber, userIDs = listOf("user-$index"))),
        )
    }

    private companion object {
        const val CTA_SETTLE_MILLISECONDS = 700L
    }
}
