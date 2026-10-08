//
//  String+InternalCommonExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.extensions

import us.neotechnica.panther.networking.Networking

internal val String.prependingCurrentEnvironment: String
    get() = "${Networking.config.environment.shortString}/${trim('/')}"
