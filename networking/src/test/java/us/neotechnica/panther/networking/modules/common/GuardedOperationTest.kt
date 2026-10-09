//
//  GuardedOperationTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.GuardedOperation
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception

/**
 * Exercises the guarded-operation precondition chain.
 */
class GuardedOperationTest {
    @Test
    fun `disabled read-write access fails preconditions with the catalogued code`() {
        Networking.isReadWriteEnabled = false
        try {
            GuardedOperation.checkPreconditions(sender = this)
            fail("Expected a precondition failure.")
        } catch (exception: Exception) {
            assertEquals("DF6E", exception.code)
        } finally {
            Networking.isReadWriteEnabled = true
        }
    }

    @Test
    fun `offline device fails preconditions with the offline code`() {
        // Build is uninitialized on the JVM, so the connectivity probe
        // reports offline.
        try {
            GuardedOperation.checkPreconditions(sender = this)
            fail("Expected a precondition failure.")
        } catch (exception: Exception) {
            assertEquals("Internet connection is offline.", exception.descriptor)
        }
    }
}
