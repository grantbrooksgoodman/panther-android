//
//  EntitySession.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.models

import us.neotechnica.panther.modules.session.entity.services.ActivitySessionService
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.entity.services.MessageSessionService
import us.neotechnica.panther.modules.session.entity.services.ModerationSessionService
import us.neotechnica.panther.modules.session.entity.services.ReactionSessionService
import us.neotechnica.panther.modules.session.entity.services.UserSessionService

/**
 * The container for the session's entity services.
 */
object EntitySession {
    /**
     * The service that adds and removes conversation participants,
     * recording each change as an activity.
     */
    val activity = ActivitySessionService

    /** The service that manages the current conversation and its displayed messages. */
    val conversation = ConversationSessionService

    /** The service that sends text, audio, and media messages. */
    val message = MessageSessionService

    /** The service that blocks, unblocks, and reports users. */
    val moderation = ModerationSessionService

    /** The service that applies and removes message reactions. */
    val reaction = ReactionSessionService

    /** The service that manages the current user and resolves their session data. */
    val user = UserSessionService
}
