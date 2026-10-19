//
//  RecipientBarContactSelectionUIService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services.chatpageview.sources.recipientbar.sources

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.neotechnica.panther.bundle.LocalizedStringKey
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewServiceFloats
import us.neotechnica.panther.modules.content.user.dependencies.inputBarService
import us.neotechnica.panther.modules.content.user.extensions.containsBlockedUser
import us.neotechnica.panther.modules.content.user.extensions.containsCurrentUser
import us.neotechnica.panther.modules.content.user.extensions.isMock
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.recipientbar.RecipientBarService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import kotlin.time.Duration.Companion.milliseconds

// MARK: - Constants Accessors

private typealias ContactSelectionUIFloats = ChatPageViewServiceFloats.RecipientBarService.ContactSelectionUI

/**
 * The service that manages the recipient bar's selected-contact
 * views.
 *
 * [RecipientBarContactSelectionUIService] tracks the selected
 * recipients and the view that represents each, highlighting a
 * recipient for deletion and switching the bar between its
 * editable and label representations.
 *
 * @param config The service that reconfigures the message list as
 *   recipients change. The default is the shared service's.
 * @param layout The service that describes the bar's subviews.
 *   The default is the shared service's.
 * @param tableView The service that manages the suggestions
 *   table. The default is the shared service's.
 */
class RecipientBarContactSelectionUIService(
    private val config: () -> RecipientBarConfigService = { RecipientBarService.config },
    private val layout: () -> RecipientBarLayoutService = { RecipientBarService.layout },
    private val tableView: () -> RecipientBarTableViewService = { RecipientBarService.tableView },
) {
    // MARK: - Properties

    private val mutableHighlightedViewID = MutableStateFlow<String?>(null)
    private val mutableIsLabelRepresentationOn = MutableStateFlow(false)
    private val mutableSelectedContactPairs = MutableStateFlow<List<ContactPair>>(emptyList())

    // MARK: - Computed Properties

    /**
     * The identifier of the recipient view highlighted for
     * deletion, or `null`.
     */
    val highlightedViewID: StateFlow<String?> = mutableHighlightedViewID.asStateFlow()

    /**
     * A Boolean value that indicates whether the recipient bar
     * shows its label representation: the selected recipients as
     * a single comma-separated run of names rather than
     * individually selectable views.
     */
    val isLabelRepresentationOn: StateFlow<Boolean> = mutableIsLabelRepresentationOn.asStateFlow()

    /** The contact pairs currently selected as recipients. */
    val selectedContactPairs: StateFlow<List<ContactPair>> = mutableSelectedContactPairs.asStateFlow()

    // MARK: - Contact Pair Selection

    /**
     * Removes the recipient identified by the given view identifier,
     * along with its view.
     *
     * @param withViewID The identifier of the recipient's view: its
     *   contact's encoded hash.
     */
    fun deselectContactPair(withViewID: String) {
        mutableSelectedContactPairs.value = mutableSelectedContactPairs.value.filter { it.contact.encodedHash != withViewID }
        if (mutableHighlightedViewID.value == withViewID) mutableHighlightedViewID.value = null

        Task.delayed(by = INPUT_BAR_CONFIGURATION_DELAY) {
            DependencyValues.current.inputBarService.configureInputBar()
            if (mutableSelectedContactPairs.value.isEmpty()) layout().setInputBarPlaceholder(null)
        }
    }

    /**
     * Removes every unregistered recipient, along with its view,
     * and – when a registered recipient remains – reconfigures
     * the message list.
     */
    fun deselectMockContactPairs() {
        val mockViewIDs = mutableSelectedContactPairs.value.filter { it.isMock }.map { it.contact.encodedHash }
        if (mockViewIDs.isEmpty()) return

        mockViewIDs.forEach { deselectContactPair(withViewID = it) }
        if (mutableSelectedContactPairs.value.isEmpty()) return
        config().reconfigureCollectionView()
    }

    /**
     * Adds the given contact pair as a recipient, creating its
     * view.
     *
     * This method has no effect when the contact pair contains a
     * blocked user or the current user, or when the maximum
     * number of recipients has been reached. Selecting an
     * already-selected pair only clears the text field. A
     * highlighted trailing recipient is replaced by the new
     * selection, and unregistered recipients are removed.
     *
     * @param contactPair The contact pair to add as a recipient.
     * @param performInputBarFix A Boolean value that determines
     *   whether to force the input bar to reappear after adding
     *   the recipient. Accepted for signature compatibility; the
     *   input bar is always present.
     */
    fun selectContactPair(
        contactPair: ContactPair,
        @Suppress("UnusedParameter") performInputBarFix: Boolean = false,
    ) {
        if (contactPair.containsBlockedUser) {
            Logger.log(
                Exception(
                    "Attempted to select contact pair containing blocked user.",
                    isReportable = false,
                    metadata = ExceptionMetadata(this),
                ),
                with = AlertType.toast,
            )

            return
        }

        if (contactPair.containsCurrentUser) {
            Logger.log(
                Exception(
                    "Attempted to select contact pair containing current user.",
                    isReportable = false,
                    metadata = ExceptionMetadata(this),
                ),
                with = AlertType.toast,
            )

            return
        }

        if (mutableSelectedContactPairs.value.size >= ContactSelectionUIFloats.SELECTED_CONTACT_PAIRS_MAXIMUM) return

        if (mutableSelectedContactPairs.value.contains(contactPair)) {
            layout().setTextFieldText("")
            tableView().setQuery("")
            return
        }

        val lastViewID =
            mutableSelectedContactPairs.value
                .lastOrNull()
                ?.contact
                ?.encodedHash
        if (lastViewID != null && isHighlighted(viewID = lastViewID)) {
            deselectContactPair(withViewID = lastViewID)
        }

        deselectMockContactPairs()

        mutableSelectedContactPairs.value = mutableSelectedContactPairs.value + contactPair

        tableView().setQuery("")
        layout().setTextFieldText("")
        config().reconfigureCollectionView()

        if (contactPair.isMock) return
        layout().setInputBarPlaceholder(" ${LocalizedStringKey.NewMessage.localized()}")
    }

    // MARK: - Label Representation

    /**
     * Shows or hides the recipient bar's label representation.
     *
     * In its label representation, the selected recipients appear
     * as a single comma-separated run of names – tappable to
     * resume editing – rather than as individually selectable
     * views.
     *
     * @param on A Boolean value that indicates whether to show
     *   the label representation.
     */
    fun toggleLabelRepresentation(on: Boolean) {
        mutableIsLabelRepresentationOn.value = on
    }

    // MARK: - View Highlighting

    /**
     * Returns a Boolean value that indicates whether the recipient
     * identified by the given view identifier is highlighted.
     *
     * @param viewID The identifier of the recipient's view.
     */
    fun isHighlighted(viewID: String): Boolean = mutableHighlightedViewID.value == viewID

    /**
     * Toggles whether the recipient identified by the given view
     * identifier is highlighted.
     *
     * Highlighting a recipient also makes the text field the first
     * responder.
     *
     * @param viewID The identifier of the recipient's view.
     */
    fun toggleIsHighlighted(viewID: String) {
        if (mutableSelectedContactPairs.value.none { it.contact.encodedHash == viewID }) return

        when (isHighlighted(viewID)) {
            true -> mutableHighlightedViewID.value = null

            false -> {
                mutableHighlightedViewID.value = viewID
                if (!layout().isTextFieldFirstResponder.value) layout().textFieldBecomeFirstResponder()
            }
        }
    }

    /** Removes highlighting from every recipient view. */
    fun unhighlightAllViews() {
        mutableHighlightedViewID.value = null
    }

    // MARK: - Reset

    /**
     * Clears every selected recipient, any highlight, and the
     * label representation.
     */
    fun reset() {
        mutableHighlightedViewID.value = null
        mutableIsLabelRepresentationOn.value = false
        mutableSelectedContactPairs.value = emptyList()
    }

    // MARK: - Companion

    private companion object {
        val INPUT_BAR_CONFIGURATION_DELAY = 100.milliseconds
    }
}
