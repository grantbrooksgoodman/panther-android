//
//  RecipientBarContactSelectionUIService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.content.user.constants.RecipientBarServiceConstants
import us.neotechnica.panther.modules.content.user.extensions.containsBlockedUser
import us.neotechnica.panther.modules.content.user.extensions.containsCurrentUser
import us.neotechnica.panther.modules.content.user.extensions.isMock
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

/**
 * Manages the recipient bar's selected recipients.
 *
 * [RecipientBarContactSelectionUIService] tracks the contact pairs
 * currently selected as recipients and which recipient is highlighted
 * for deletion. Selecting is refused for a contact pair that contains a
 * blocked user or the current user, that is already selected, or once
 * the maximum number of recipients has been reached.
 */
object RecipientBarContactSelectionUIService {
    // MARK: - Properties

    private val mutableHighlightedViewID = MutableStateFlow<String?>(null)
    private val mutableSelectedContactPairs = MutableStateFlow<List<ContactPair>>(emptyList())

    // MARK: - Computed Properties

    /** The identifier of the recipient highlighted for deletion, or `null`. */
    val highlightedViewID: StateFlow<String?> = mutableHighlightedViewID.asStateFlow()

    /** The contact pairs currently selected as recipients. */
    val selectedContactPairs: StateFlow<List<ContactPair>> = mutableSelectedContactPairs.asStateFlow()

    // MARK: - Contact Pair Selection

    /**
     * Adds the given contact pair as a recipient.
     *
     * This method has no effect when the contact pair contains a blocked
     * user or the current user, when it is already selected, or when the
     * maximum number of recipients has been reached.
     *
     * @param contactPair The contact pair to add as a recipient.
     * @param performInputBarFix Retained for naming parity; has no effect.
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

        val current = mutableSelectedContactPairs.value
        if (current.contains(contactPair) || current.size >= RecipientBarServiceConstants.SELECTED_CONTACT_PAIRS_MAXIMUM) return

        if (mutableHighlightedViewID.value != null) unhighlightAllViews()
        deselectMockContactPairs()
        mutableSelectedContactPairs.value = mutableSelectedContactPairs.value + contactPair
    }

    /**
     * Removes the recipient identified by the given view identifier.
     *
     * @param withViewID The identifier of the recipient's view, its
     *   contact's encoded hash.
     */
    fun deselectContactPair(withViewID: String) {
        if (mutableHighlightedViewID.value == withViewID) mutableHighlightedViewID.value = null
        mutableSelectedContactPairs.value = mutableSelectedContactPairs.value.filter { it.contact.encodedHash != withViewID }
    }

    /** Removes every unregistered recipient. */
    fun deselectMockContactPairs() {
        mutableSelectedContactPairs.value = mutableSelectedContactPairs.value.filter { !it.isMock }
    }

    /**
     * Shows or hides the recipient bar's label representation.
     *
     * @param on A Boolean value that indicates whether to show the label
     *   representation.
     */
    @Suppress("UnusedParameter")
    fun toggleLabelRepresentation(on: Boolean) {
        // The recipient chips are always individually selectable; there is
        // no collapsed label representation.
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
     * @param viewID The identifier of the recipient's view.
     */
    fun toggleIsHighlighted(viewID: String) {
        mutableHighlightedViewID.value = if (mutableHighlightedViewID.value == viewID) null else viewID
    }

    /** Removes highlighting from every recipient view. */
    fun unhighlightAllViews() {
        mutableHighlightedViewID.value = null
    }

    // MARK: - Reset

    /** Clears every selected recipient and any highlight. */
    fun reset() {
        mutableHighlightedViewID.value = null
        mutableSelectedContactPairs.value = emptyList()
    }
}
