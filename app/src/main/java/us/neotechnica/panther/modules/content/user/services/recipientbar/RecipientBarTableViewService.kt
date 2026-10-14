//
//  RecipientBarTableViewService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services.recipientbar

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.content.user.extensions.absoluteLastName
import us.neotechnica.panther.modules.content.user.extensions.alphabeticallySorted
import us.neotechnica.panther.modules.content.user.extensions.queried
import us.neotechnica.panther.modules.content.user.extensions.tableViewSectionTitle
import us.neotechnica.panther.modules.content.user.extensions.uniquedByPhoneNumber
import us.neotechnica.panther.modules.content.user.extensions.userIDs
import us.neotechnica.panther.modules.content.user.extensions.withUser
import us.neotechnica.panther.modules.content.user.models.QueriedContactPairCache
import us.neotechnica.panther.modules.networking.common.extensions.visibleForCurrentUser
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.SingleSlotCoalescer

/**
 * The service that manages the recipient bar's contact suggestions
 * table.
 *
 * [RecipientBarTableViewService] resolves the contacts that can be
 * added as recipients – combining the user's known contacts with the
 * participants of their existing conversations – and groups them into
 * alphabetical sections. It also filters the suggestions by the text
 * the user types.
 *
 * @param knownContactPairsProvider Returns the user's known contact
 *   pairs. The default reads the contact pair archive.
 * @param conversationsProvider Returns the current user's
 *   conversations, or `null` if they have not resolved.
 */
class RecipientBarTableViewService(
    private val knownContactPairsProvider: () -> List<ContactPair> = {
        DependencyValues.current.commonServices.contact.contactPairArchive
            .allValues()
    },
    private val conversationsProvider: () -> List<Conversation>? = {
        DependencyValues.current.clientSession.entity.user.currentUser
            ?.conversations
    },
) {
    // MARK: - Types

    /** A group of contact pairs sharing a section heading in the suggestions table. */
    class TableViewSection(
        /** The section's heading letter. */
        val letter: String,
        /** The contact pairs in the section. */
        val contactPairs: List<ContactPair>,
    )

    // MARK: - Properties

    private val coalescer = SingleSlotCoalescer<List<ContactPair>>()
    private val mutableIsTableViewVisible = MutableStateFlow(false)
    private val mutableSections = MutableStateFlow<List<TableViewSection>>(emptyList())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var contactPairs: List<ContactPair>? = null
    private var currentQuery: String? = null
    private var queriedContactPairs = listOf<ContactPair>()
        set(value) {
            field = value
            mutableSections.value = getSections()
        }

    // MARK: - Computed Properties

    /**
     * A Boolean value that indicates whether the suggestions table is
     * shown in place of the message list.
     */
    val isTableViewVisible: StateFlow<Boolean> = mutableIsTableViewVisible.asStateFlow()

    /** The current suggestions, grouped into alphabetical sections and sorted by last name. */
    val sections: StateFlow<List<TableViewSection>> = mutableSections.asStateFlow()

    // MARK: - Reload Data

    /**
     * Reloads the suggestions table, resolving the available contacts
     * first if they have not yet been resolved.
     */
    fun reloadData() {
        if (contactPairs == null) {
            serviceScope.launch {
                resolveContactPairs()
                reloadDataInternal()
            }

            return
        }

        reloadDataInternal()
    }

    // MARK: - Resolve Contact Pairs

    /**
     * Resolves the contacts that can be added as recipients.
     *
     * The resolved set combines the user's known contacts with the
     * participants of their visible conversations and removes
     * duplicates by phone number.
     *
     * Concurrent calls coalesce onto a single in-flight resolution.
     */
    suspend fun resolveContactPairs() {
        val conversations = conversationsProvider()
        val knownContactPairs = knownContactPairsProvider()

        contactPairs =
            coalescer {
                withContext(Dispatchers.Default) {
                    val visibleConversations = conversations?.visibleForCurrentUser
                    if (knownContactPairs.isEmpty() || visibleConversations == null) {
                        return@withContext knownContactPairs.uniquedByPhoneNumber
                    }

                    val knownUserIDs = knownContactPairs.userIDs.toSet()
                    val seenUserIDs = mutableSetOf<String>()
                    val unknownUsers = mutableListOf<User>()

                    for (conversation in visibleConversations) {
                        unknownUsers +=
                            conversation.users
                                .orEmpty()
                                .filter { !knownUserIDs.contains(it.id) && seenUserIDs.add(it.id) }
                    }

                    val unknownContactPairs = unknownUsers.map { ContactPair.withUser(it) }
                    (knownContactPairs + unknownContactPairs).uniquedByPhoneNumber
                }
            }
    }

    // MARK: - Set Query

    /**
     * Filters the suggestions by the given query and shows or hides the
     * table accordingly.
     *
     * A blank query hides the table and restores the message list; a
     * non-blank query shows the filtered suggestions in place of the
     * message list. A query of `0` shows every suggestion.
     *
     * @param query The text to filter the suggestions by.
     */
    fun setQuery(query: String) {
        currentQuery = query.ifBlank { null }
        if (query.isBlank()) {
            mutableIsTableViewVisible.value = false
            reloadData()
            return
        }

        val contactPairs = contactPairs ?: emptyList()
        queriedContactPairs = if (query.isZero) contactPairs else contactPairs.queried(query)
        mutableIsTableViewVisible.value = true
    }

    // MARK: - Reset

    /** Clears the resolved contacts and the current query. */
    fun reset() {
        contactPairs = null
        currentQuery = null
        queriedContactPairs = emptyList()
        mutableIsTableViewVisible.value = false
    }

    // MARK: - Auxiliary

    private fun getSections(): List<TableViewSection> {
        fun sortedByLastName(contactPairs: List<ContactPair>): List<ContactPair> =
            contactPairs.distinct().sortedBy { it.contact.absoluteLastName }

        val dictionary = queriedContactPairs.distinct().groupBy { it.contact.tableViewSectionTitle }
        val sortedKeys = dictionary.keys.toList().alphabeticallySorted
        return sortedKeys.map { TableViewSection(letter = it, contactPairs = sortedByLastName(dictionary.getValue(it))) }
    }

    private fun reloadDataInternal() {
        val contactPairs = contactPairs
        if (contactPairs.isNullOrEmpty()) return

        val query = currentQuery
        queriedContactPairs = if (query == null || query.isZero) contactPairs else contactPairs.queried(query)
        QueriedContactPairCache.canWriteToCache = true
    }

    private val String.isZero: Boolean
        get() = trim().lowercase() == "0"
}
