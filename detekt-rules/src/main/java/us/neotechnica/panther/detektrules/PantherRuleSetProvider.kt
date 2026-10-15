//
//  PantherRuleSetProvider.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.detektrules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider
import us.neotechnica.panther.detektrules.modules.foundation.services.FileLength
import us.neotechnica.panther.detektrules.modules.foundation.services.MultilineParameters

/**
 * The provider of the `panther` rule set, which detekt discovers
 * through the service loader.
 *
 * Add this module to a project's `detektPlugins` configuration to
 * run its rules, and configure them under the `panther` key of the
 * detekt configuration file:
 *
 * ```yaml
 * panther:
 *   FileLength:
 *     threshold: 400
 * ```
 *
 * Suppress a rule with its identifier, as you would any detekt
 * rule – for example, `@file:Suppress("FileLength")`.
 */
class PantherRuleSetProvider : RuleSetProvider {
    // MARK: - RuleSetProvider Conformance

    override val ruleSetId: String = "panther"

    override fun instance(config: Config): RuleSet =
        RuleSet(
            ruleSetId,
            listOf(
                FileLength(config),
                MultilineParameters(config),
            ),
        )
}
