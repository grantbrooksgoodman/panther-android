//
//  Translation+TranslationExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.extensions

import us.neotechnica.panther.networking.modules.translation.models.TranslationReference
import us.neotechnica.panther.translator.models.Translation

/** A codable reference to this translation. */
val Translation.reference: TranslationReference
    get() = TranslationReference.from(this)
