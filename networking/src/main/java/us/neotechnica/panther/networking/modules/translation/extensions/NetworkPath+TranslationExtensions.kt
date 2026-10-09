//
//  NetworkPath+TranslationExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.extensions

import us.neotechnica.panther.networking.modules.common.models.NetworkPath

internal val NetworkPath.Companion.translations: NetworkPath
    get() = NetworkPath("translations")
