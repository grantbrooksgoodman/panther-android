//
//  MultilineParametersTest.kt
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

class MultilineParametersTest {
    // MARK: - Properties

    private val rule = MultilineParameters(TestConfig("active" to true))

    // MARK: - Tests

    @Test
    fun `a single parameter on one line is not reported`() {
        assertEquals(0, rule.lint("fun move(x: Int) {}").size)
    }

    @Test
    fun `two parameters on one line are reported`() {
        assertEquals(1, rule.lint("fun move(x: Int, y: Int) {}").size)
    }

    @Test
    fun `one parameter per line is not reported`() {
        val code =
            """
            fun move(
                x: Int,
                y: Int,
            ) {}
            """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `parameters sharing some lines are reported`() {
        val code =
            """
            fun move(
                x: Int, y: Int,
                z: Int,
            ) {}
            """.trimIndent()

        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `constructor parameters are checked`() {
        val code =
            """
            class Point(val x: Int, val y: Int) {
                constructor(x: Int, y: Int, z: Int) : this(x, y)
            }
            """.trimIndent()

        assertEquals(2, rule.lint(code).size)
    }

    @Test
    fun `lambda parameters are not checked`() {
        assertEquals(0, rule.lint("val add = { x: Int, y: Int -> x + y }").size)
    }

    @Test
    fun `a raised single-line maximum permits more parameters on one line`() {
        val rule =
            MultilineParameters(
                TestConfig(
                    "active" to true,
                    "maxSingleLineParameters" to 2,
                ),
            )

        assertEquals(0, rule.lint("fun move(x: Int, y: Int) {}").size)
    }

    @Test
    fun `a declaration suppression exempts the declaration`() {
        val code =
            """
            @Suppress("MultilineParameters")
            fun move(x: Int, y: Int) {}
            """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }
}
