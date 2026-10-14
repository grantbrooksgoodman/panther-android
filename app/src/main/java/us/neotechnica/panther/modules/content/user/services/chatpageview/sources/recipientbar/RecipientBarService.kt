//
//  RecipientBarService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services.chatpageview.sources.recipientbar

import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.recipientbar.sources.RecipientBarActionHandlerService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.recipientbar.sources.RecipientBarConfigService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.recipientbar.sources.RecipientBarContactSelectionUIService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.recipientbar.sources.RecipientBarLayoutService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.recipientbar.sources.RecipientBarTableViewService

/**
 * The service that manages the recipient bar.
 *
 * Use [RecipientBarService] to drive the bar for choosing recipients
 * when composing a new conversation. The service groups the
 * specialized services that describe the bar's state, reconfigure the
 * message list as recipients change, manage the selected-contact
 * views, present the contact suggestions table, and handle the user's
 * actions.
 *
 * One new conversation is composed at a time, so the service and its
 * sub-services are shared singletons; [reset] clears them when the
 * page disappears.
 */
object RecipientBarService {
    // MARK: - Properties

    /** The service that handles the recipient bar's user actions. */
    val actionHandler = RecipientBarActionHandlerService()

    /** The service that reconfigures the recipient bar's message list as recipients change. */
    val config = RecipientBarConfigService()

    /** The service that manages the recipient bar's selected-contact views. */
    val contactSelectionUI = RecipientBarContactSelectionUIService()

    /** The service that describes the recipient bar's subviews. */
    val layout = RecipientBarLayoutService()

    /** The service that manages the recipient bar's contact suggestions table. */
    val tableView = RecipientBarTableViewService()

    // MARK: - On Layout Subviews

    /**
     * Reloads the contact suggestions table and makes the recipient
     * bar's text field the first responder.
     */
    fun onLayoutSubviews() {
        layout.textFieldBecomeFirstResponder()
        tableView.reloadData()
    }

    // MARK: - Reset

    /** Clears every sub-service's state for the next composition. */
    fun reset() {
        config.reset()
        contactSelectionUI.reset()
        layout.reset()
        tableView.reset()
    }
}
