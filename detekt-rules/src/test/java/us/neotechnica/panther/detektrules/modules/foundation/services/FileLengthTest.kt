//
//  FileLengthTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.detektrules.modules.foundation.services

import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import org.junit.Assert.assertEquals
import org.junit.Test

class FileLengthTest {
    // MARK: - Properties

    private val rule =
        FileLength(
            TestConfig(
                "active" to true,
                "threshold" to 3,
            ),
        )

    // MARK: - Tests

    @Test
    fun `a file at the threshold is not reported`() {
        assertEquals(0, rule.lint(code(lineCount = 3)).size)
    }

    @Test
    fun `a file longer than the threshold is reported`() {
        assertEquals(1, rule.lint(code(lineCount = 4)).size)
    }

    @Test
    fun `comment and blank lines count toward the length`() {
        val code =
            """
            // A comment.

            // Another comment.
            val value = 0
            """.trimIndent() + "\n"

        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `a file suppression exempts the file`() {
        val code = "@file:Suppress(\"FileLength\")\n\n" + code(lineCount = 4)

        assertEquals(0, rule.lint(code).size)
    }

    // MARK: - Auxiliary

    private fun code(lineCount: Int): String = (1..lineCount).joinToString(separator = "") { "val value$it = $it\n" }
}
