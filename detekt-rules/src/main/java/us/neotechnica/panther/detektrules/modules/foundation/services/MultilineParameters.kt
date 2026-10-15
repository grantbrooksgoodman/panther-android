//
//  MultilineParameters.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.detektrules.modules.foundation.services

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Location
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.config
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtSecondaryConstructor

/**
 * A rule that reports function and constructor declarations whose
 * parameters are neither on a single line nor one per line.
 *
 * A declaration may list its parameters on a single line only when
 * it has no more than `maxSingleLineParameters` of them, which
 * defaults to `1`. Otherwise, each parameter must begin on its own
 * line:
 *
 * ```kotlin
 * // Reported.
 * fun move(x: Int, y: Int) {}
 *
 * // Not reported.
 * fun move(
 *     x: Int,
 *     y: Int,
 * ) {}
 * ```
 *
 * The rule doesn't apply to lambda parameters.
 *
 * @param config The configuration of the rule set that contains the
 *   rule.
 */
class MultilineParameters(
    config: Config = Config.empty,
) : Rule(config) {
    // MARK: - Properties

    override val issue: Issue =
        Issue(
            id = "MultilineParameters",
            severity = Severity.Style,
            description = "Parameters should be either on the same line, or one per line.",
            debt = Debt.FIVE_MINS,
        )

    private val maxSingleLineParameters: Int by config(defaultValue = 1)

    // MARK: - Methods

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        function.valueParameterList?.let { checkParameters(it) }
    }

    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
        super.visitPrimaryConstructor(constructor)
        constructor.valueParameterList?.let { checkParameters(it) }
    }

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
        super.visitSecondaryConstructor(constructor)
        constructor.valueParameterList?.let { checkParameters(it) }
    }

    // MARK: - Auxiliary

    private fun checkParameters(parameterList: KtParameterList) {
        val parameters = parameterList.parameters
        if (parameters.size < 2) return

        val lineCount =
            parameters
                .map { Location.from(it).source.line }
                .toSet()
                .size

        val message =
            when {
                lineCount == 1 && parameters.size > maxSingleLineParameters ->
                    "Too many parameters on a single line (maximum: $maxSingleLineParameters)."

                lineCount in 2..<parameters.size ->
                    "Parameters should be either on the same line, or one per line."

                else -> return
            }

        report(
            CodeSmell(
                issue,
                Entity.from(parameterList),
                message,
            ),
        )
    }
}
