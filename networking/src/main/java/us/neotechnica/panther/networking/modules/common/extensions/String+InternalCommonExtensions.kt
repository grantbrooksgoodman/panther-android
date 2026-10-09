//
//  String+InternalCommonExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.extensions

import us.neotechnica.panther.networking.Networking

internal val String.prependingCurrentEnvironment: String
    get() = "${Networking.config.environment.shortString}/${trim('/')}"
