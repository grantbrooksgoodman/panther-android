//
//  Throwable+HealthExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health.extensions

import java.io.IOException

internal val Throwable.isNetworkLevelFailure: Boolean
    get() = this is IOException
