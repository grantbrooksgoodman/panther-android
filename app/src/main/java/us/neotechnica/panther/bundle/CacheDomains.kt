//
//  CacheDomains.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle

import us.neotechnica.panther.modules.common.contacts.services.ContactPairArchiveService
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.services.CommonPropertyLists
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.modules.common.services.TextToSpeechServiceCache
import us.neotechnica.panther.modules.content.user.extensions.UserDisplayNameCache
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewDataCache
import us.neotechnica.panther.modules.content.user.models.QueriedContactPairCache
import us.neotechnica.panther.modules.content.user.models.QueriedConversationCache
import us.neotechnica.panther.modules.content.user.services.ChatInfoPageViewService
import us.neotechnica.panther.modules.content.user.services.SettingsPageViewService
import us.neotechnica.panther.modules.networking.message.models.ReadReceiptCache
import us.neotechnica.panther.modules.networking.user.services.UserService
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.CacheDomainListDelegate
import us.neotechnica.panther.subsystem.modules.foundation.models.CacheDomain

/**
 * The delegate that supplies the app's cache domains to the
 * subsystem.
 *
 * The subsystem merges these domains with its own built-in domains
 * automatically.
 */
object CacheDomainList : CacheDomainListDelegate {
    override val appCacheDomains: List<CacheDomain>
        get() =
            listOf(
                CacheDomain.chatInfoPageViewService,
                CacheDomain.commonPropertyLists,
                CacheDomain.contactPairArchive,
                CacheDomain.contactService,
                CacheDomain.conversationArchive,
                CacheDomain.conversationCellViewData,
                CacheDomain.messageArchive,
                CacheDomain.Networking.database,
                CacheDomain.Networking.storage,
                CacheDomain.queriedContactPairs,
                CacheDomain.queriedConversations,
                CacheDomain.readReceipt,
                CacheDomain.regionDetailService,
                CacheDomain.settingsPageViewService,
                CacheDomain.textToSpeechService,
                CacheDomain.userArchive,
                CacheDomain.userDisplayName,
                CacheDomain.userService,
            )
}

// MARK: - Properties

/** The cache domain for the `chatInfoPageViewService` cache. */
val CacheDomain.Companion.chatInfoPageViewService: CacheDomain
    get() = CacheDomain("chatInfoPageViewService") { ChatInfoPageViewService.clearCache() }

/** The cache domain for the `commonPropertyLists` cache. */
val CacheDomain.Companion.commonPropertyLists: CacheDomain
    get() = CacheDomain("commonPropertyLists") { CommonPropertyLists.clearCache() }

/** The cache domain for the `contactPairArchive` cache. */
val CacheDomain.Companion.contactPairArchive: CacheDomain
    get() = CacheDomain("contactPairArchive") { ContactPairArchiveService.clearArchive() }

/** The cache domain for the `contactService` cache. */
val CacheDomain.Companion.contactService: CacheDomain
    get() = CacheDomain("contactService") { ContactService.clearCache() }

/** The cache domain for the `conversationArchive` cache. */
val CacheDomain.Companion.conversationArchive: CacheDomain
    get() = CacheDomain("conversationArchive") { SessionStore.clearConversationArchive() }

/** The cache domain for the `conversationCellViewData` cache. */
val CacheDomain.Companion.conversationCellViewData: CacheDomain
    get() = CacheDomain("conversationCellViewData") { ConversationCellViewDataCache.clearCache() }

/** The cache domain for the `messageArchive` cache. */
val CacheDomain.Companion.messageArchive: CacheDomain
    get() = CacheDomain("messageArchive") { SessionStore.clearMessageArchive() }

/** The cache domain for the `queriedContactPairs` cache. */
val CacheDomain.Companion.queriedContactPairs: CacheDomain
    get() = CacheDomain("queriedContactPairs") { QueriedContactPairCache.clearCache() }

/** The cache domain for the `queriedConversations` cache. */
val CacheDomain.Companion.queriedConversations: CacheDomain
    get() = CacheDomain("queriedConversations") { QueriedConversationCache.clearCache() }

/** The cache domain for the `readReceipt` cache. */
val CacheDomain.Companion.readReceipt: CacheDomain
    get() = CacheDomain("readReceipt") { ReadReceiptCache.clearCache() }

/** The cache domain for the `regionDetailService` cache. */
val CacheDomain.Companion.regionDetailService: CacheDomain
    get() = CacheDomain("regionDetailService") { RegionDetailService.clearCache() }

/** The cache domain for the `settingsPageViewService` cache. */
val CacheDomain.Companion.settingsPageViewService: CacheDomain
    get() = CacheDomain("settingsPageViewService") { SettingsPageViewService.clearCache() }

/** The cache domain for the `textToSpeechService` cache. */
val CacheDomain.Companion.textToSpeechService: CacheDomain
    get() = CacheDomain("textToSpeechService") { TextToSpeechServiceCache.clearCache() }

/** The cache domain for the `userArchive` cache. */
val CacheDomain.Companion.userArchive: CacheDomain
    get() = CacheDomain("userArchive") { SessionStore.clearUserArchive() }

/** The cache domain for the `userDisplayName` cache. */
val CacheDomain.Companion.userDisplayName: CacheDomain
    get() = CacheDomain("userDisplayName") { UserDisplayNameCache.clearCache() }

/** The cache domain for the `userService` cache. */
val CacheDomain.Companion.userService: CacheDomain
    get() = CacheDomain("userService") { UserService.clearCache() }
