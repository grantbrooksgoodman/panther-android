//
//  Throwable+HealthExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health.extensions

import java.io.IOException

internal val Throwable.isNetworkLevelFailure: Boolean
    get() = this is IOException
