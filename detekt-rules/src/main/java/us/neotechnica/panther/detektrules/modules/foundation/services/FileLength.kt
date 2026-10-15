//
//  FileLength.kt
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
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.config
import org.jetbrains.kotlin.psi.KtFile

/**
 * A rule that reports files that span more lines than a threshold.
 *
 * Every line counts toward a file's length, including comments and
 * blank lines. The `threshold` option sets the maximum number of
 * lines a file may span, and defaults to 400.
 *
 * To exempt a file, annotate it with `@file:Suppress("FileLength")`.
 *
 * @param config The configuration of the rule set that contains the
 *   rule.
 */
class FileLength(
    config: Config = Config.empty,
) : Rule(config) {
    // MARK: - Properties

    override val issue: Issue =
        Issue(
            id = "FileLength",
            severity = Severity.Maintainability,
            description = "Files should not span too many lines.",
            debt = Debt.TWENTY_MINS,
        )

    private val threshold: Int by config(defaultValue = 400)

    // MARK: - Methods

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)

        val text = file.text
        val lineCount = text.lines().size - if (text.endsWith('\n')) 1 else 0
        if (lineCount <= threshold) return

        report(
            CodeSmell(
                issue,
                Entity.atPackageOrFirstDecl(file),
                "The file spans $lineCount lines, which exceeds the maximum of $threshold.",
            ),
        )
    }
}
