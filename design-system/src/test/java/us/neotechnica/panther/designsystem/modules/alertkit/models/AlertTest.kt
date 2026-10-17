//
//  AlertTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

import org.junit.Assert.assertThrows
import org.junit.Test

class AlertTest {
    // MARK: - Tests

    @Test
    fun `an alert without actions is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            Alert(message = "Message", actions = emptyList())
        }
    }

    @Test
    fun `an action sheet without actions is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ActionSheet(title = "Title", actions = emptyList())
        }
    }

    @Test
    fun `the default alert carries a single action`() {
        Alert(message = "Message")
    }
}
