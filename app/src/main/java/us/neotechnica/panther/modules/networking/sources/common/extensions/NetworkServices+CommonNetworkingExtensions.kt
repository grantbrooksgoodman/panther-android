//
//  NetworkServices+CommonNetworkingExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.sources.common.extensions

import us.neotechnica.panther.modules.networking.sources.message.services.MessageService
import us.neotechnica.panther.modules.networking.sources.user.services.UserService
import us.neotechnica.panther.networking.modules.common.models.NetworkServices

/** The message service. */
val NetworkServices.messageService: MessageService
    get() = MessageService

/** The user service. */
val NetworkServices.userService: UserService
    get() = UserService
