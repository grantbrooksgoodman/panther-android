//
//  NetworkServices+CommonNetworkingExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.common

import us.neotechnica.panther.modules.networking.conversation.services.ConversationService
import us.neotechnica.panther.modules.networking.message.services.MessageService
import us.neotechnica.panther.modules.networking.user.services.UserService
import us.neotechnica.panther.networking.modules.common.models.NetworkServices

/** The conversation service. */
val NetworkServices.conversationService: ConversationService
    get() = ConversationService

/** The message service. */
val NetworkServices.messageService: MessageService
    get() = MessageService

/** The user service. */
val NetworkServices.userService: UserService
    get() = UserService
