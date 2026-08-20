//
//  DeepLTranslator.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.services

import us.neotechnica.panther.translator.models.TranslationPlatform

/**
 * Translates using DeepL through the [BaseTranslator] web-view
 * harness. DeepL exposes no lightweight API, so it is web-view only.
 */
internal class DeepLTranslator : BaseTranslator(TranslationPlatform.DEEP_L)
