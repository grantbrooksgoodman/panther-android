//
//  NetworkPaths.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle

import us.neotechnica.panther.networking.modules.common.models.NetworkPath

/** The top-level `audioMessageInputs` path. */
val NetworkPath.Companion.audioMessageInputs: NetworkPath
    get() = NetworkPath("audioMessageInputs")

/** The top-level `audioTranslations` path. */
val NetworkPath.Companion.audioTranslations: NetworkPath
    get() = NetworkPath("audioTranslations")

/** The top-level `conversations` path. */
val NetworkPath.Companion.conversations: NetworkPath
    get() = NetworkPath("conversations")

/** The top-level `deletedUsers` path. */
val NetworkPath.Companion.deletedUsers: NetworkPath
    get() = NetworkPath("deletedUsers")

/** The top-level `invalidatedCaches` path. */
val NetworkPath.Companion.invalidatedCaches: NetworkPath
    get() = NetworkPath("invalidatedCaches")

/** The top-level `media` path. */
val NetworkPath.Companion.media: NetworkPath
    get() = NetworkPath("media")

/** The top-level `messages` path. */
val NetworkPath.Companion.messages: NetworkPath
    get() = NetworkPath("messages")

/** The top-level `reportedUsers` path. */
val NetworkPath.Companion.reportedUsers: NetworkPath
    get() = NetworkPath("reportedUsers")

/** The top-level `shared` path. */
val NetworkPath.Companion.shared: NetworkPath
    get() = NetworkPath("shared")

/** The top-level `translations` path. */
val NetworkPath.Companion.translations: NetworkPath
    get() = NetworkPath("translations")

/** The top-level `users` path. */
val NetworkPath.Companion.users: NetworkPath
    get() = NetworkPath("users")
