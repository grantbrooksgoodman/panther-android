//
//  AuthTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.auth

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.networking.modules.auth.interfaces.AuthDelegate
import us.neotechnica.panther.networking.modules.auth.services.Auth
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import java.io.IOException

class AuthTest {
    // MARK: - Properties

    private val auth = Auth()

    // MARK: - Tests

    @Test
    fun `a sent code resolves to the verification ID`() =
        runTest {
            val outcome =
                suspendCancellableCoroutine { continuation ->
                    auth.PhoneVerificationHandler(continuation).onCodeAutoRetrievalTimeOut("verification-id")
                }

            assertEquals(
                AuthDelegate.PhoneVerificationResult.CodeSent("verification-id"),
                auth.resolve(outcome),
            )
        }

    @Test
    fun `an instant verification settles with the sign-in`() =
        runTest {
            val outcome =
                suspendCancellableCoroutine { continuation ->
                    auth.PhoneVerificationHandler(continuation).onVerificationCompleted { "user-id" }
                }

            assertEquals(AuthDelegate.PhoneVerificationResult.SignedIn("user-id"), auth.resolve(outcome))
        }

    @Test
    fun `a completed verification signs in with the delivered credential`() =
        runTest {
            val result = auth.resolve(Auth.VerificationOutcome.Completed { "user-id" })

            assertEquals(AuthDelegate.PhoneVerificationResult.SignedIn("user-id"), result)
        }

    @Test
    fun `a failed verification throws the wrapped exception`() =
        runTest {
            val failure =
                runCatching {
                    suspendCancellableCoroutine<Auth.VerificationOutcome> { continuation ->
                        auth.PhoneVerificationHandler(continuation).onVerificationFailed(IOException("Quota exceeded."))
                    }
                }.exceptionOrNull()

            assertTrue(failure is Exception)
            assertTrue((failure as Exception).descriptor.contains("Quota exceeded."))
        }
}
