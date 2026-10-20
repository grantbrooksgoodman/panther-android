//
//  CompatibilityFrameTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.components

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompatibilityFrameTest {
    // MARK: - Tests

    @Test
    fun `a window smaller than 600 dp in either dimension is not framed`() {
        assertNull(compatibilityFrameWidth(DpSize(412.dp, 915.dp)))
        assertNull(compatibilityFrameWidth(DpSize(599.dp, 1280.dp)))
        assertNull(compatibilityFrameWidth(DpSize(1280.dp, 599.dp)))
        assertNull(compatibilityFrameWidth(DpSize.Unspecified))
    }

    @Test
    fun `a large window frames its content at a phone's proportions`() {
        assertEquals(1280f * 9f / 19.5f, frameWidth(800, 1280), TOLERANCE)
        assertEquals(800f * 9f / 19.5f, frameWidth(1280, 800), TOLERANCE)
    }

    @Test
    fun `the frame is never narrower than 360 dp or wider than the window`() {
        assertEquals(360f, frameWidth(1024, 600), TOLERANCE)
        assertEquals(600f, frameWidth(600, 1600), TOLERANCE)
    }

    // MARK: - Auxiliary

    private fun frameWidth(
        width: Int,
        height: Int,
    ): Float = requireNotNull(compatibilityFrameWidth(DpSize(width.dp, height.dp))).value

    private companion object {
        const val TOLERANCE = 0.01f
    }
}
