//
//  SquareIconViewConfigurationTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.models

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale

/**
 * Exercises the square icon configuration: its default size and
 * shadow, the overlay defaults, and value equality.
 */
class SquareIconViewConfigurationTest {
    // MARK: - Defaults

    @Test
    fun `the default configuration is the standard size without a shadow`() {
        val configuration =
            SquareIconViewConfiguration(
                backgroundColor = Color.Blue,
                overlay = SquareIconViewConfiguration.OverlayConfiguration.Symbol("sparkles"),
            )

        assertEquals(DpSize(150.dp, 150.dp), configuration.size)
        assertFalse(configuration.includesShadow)
    }

    @Test
    fun `a symbol overlay defaults to a white two-thirds frame in the regular weight`() {
        val overlay = SquareIconViewConfiguration.OverlayConfiguration.Symbol("sparkles")

        assertEquals(Color.White, overlay.foregroundColor)
        assertEquals(2f / 3f, overlay.framePercentOfTotalSize, 0f)
        assertNull(overlay.weight)
    }

    @Test
    fun `a resource overlay carries its weight`() {
        val overlay = SquareIconViewConfiguration.OverlayConfiguration.Resource(resourceID = 1, weight = FontWeight.Bold)

        assertEquals(FontWeight.Bold, overlay.weight)
        assertEquals(2f / 3f, overlay.framePercentOfTotalSize, 0f)
    }

    @Test
    fun `a text overlay defaults to the 60-point system font`() {
        val overlay = SquareIconViewConfiguration.OverlayConfiguration.Text("❤️")

        assertEquals(60f, (overlay.font.scale as FontScale.Custom).points, 0f)
        assertEquals(Color.White, overlay.foregroundColor)
    }

    // MARK: - Equality

    @Test
    fun `configurations with the same values are equal`() {
        val first =
            SquareIconViewConfiguration(
                backgroundColor = Color.Blue,
                overlay = SquareIconViewConfiguration.OverlayConfiguration.Symbol("sparkles"),
                includesShadow = true,
            )
        val second =
            SquareIconViewConfiguration(
                backgroundColor = Color.Blue,
                overlay = SquareIconViewConfiguration.OverlayConfiguration.Symbol("sparkles"),
                includesShadow = true,
            )

        assertEquals(first, second)
        assertNotEquals(first, second.copy(includesShadow = false))
        assertNotEquals(first, second.copy(overlay = SquareIconViewConfiguration.OverlayConfiguration.Symbol("figure.2")))
    }
}
