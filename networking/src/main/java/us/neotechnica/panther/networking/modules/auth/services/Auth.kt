//
//  Auth.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.auth.services

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.auth.interfaces.AuthDelegate
import us.neotechnica.panther.networking.modules.common.extensions.digits
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * The Firebase Authentication implementation of [AuthDelegate].
 */
class Auth : AuthDelegate {
    // MARK: - Properties

    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }

    // MARK: - AuthDelegate Conformance

    override suspend fun authenticateUser(
        authID: String,
        verificationCode: String,
    ): String {
        assertReadWriteEnabled()

        Networking.config.activityIndicatorDelegate.show()
        return try {
            val credential = PhoneAuthProvider.getCredential(authID, verificationCode)
            signInOrLink(credential)
        } finally {
            Networking.config.activityIndicatorDelegate.hide()
        }
    }

    override suspend fun signInAnonymously(): String {
        assertReadWriteEnabled()

        firebaseAuth.currentUser?.let { return it.uid }

        return try {
            checkNotNull(firebaseAuth.signInAnonymously().await().user).uid
        } catch (throwable: Throwable) {
            throw wrap(throwable)
        }
    }

    override fun signOut() {
        try {
            firebaseAuth.signOut()
        } catch (throwable: Throwable) {
            throw wrap(throwable)
        }
    }

    override suspend fun verifyPhoneNumber(
        activity: Activity,
        internationalNumber: String,
        languageCode: String,
    ): String {
        assertReadWriteEnabled()

        firebaseAuth.setLanguageCode(languageCode)
        Networking.config.activityIndicatorDelegate.show()

        return try {
            requestVerificationID(
                activity,
                "+${internationalNumber.digits}",
            )
        } finally {
            Networking.config.activityIndicatorDelegate.hide()
        }
    }

    // MARK: - Auxiliary

    private fun assertReadWriteEnabled() {
        if (!Networking.isReadWriteEnabled) {
            throw Exception(
                "Read/write access is currently disabled.",
                metadata = ExceptionMetadata(this),
            )
        }
    }

    private suspend fun requestVerificationID(
        activity: Activity,
        formattedNumber: String,
    ): String =
        suspendCancellableCoroutine { continuation ->
            val callbacks =
                object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onCodeSent(
                        verificationID: String,
                        token: PhoneAuthProvider.ForceResendingToken,
                    ) {
                        if (continuation.isActive) continuation.resume(verificationID)
                    }

                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        // Instant / auto-retrieval. The verification ID, if
                        // present, still drives the code-entry flow.
                        credential.smsCode?.let { return }
                    }

                    override fun onVerificationFailed(exception: com.google.firebase.FirebaseException) {
                        if (continuation.isActive) continuation.resumeWithException(wrap(exception))
                    }
                }

            PhoneAuthProvider.verifyPhoneNumber(
                PhoneAuthOptions
                    .newBuilder(firebaseAuth)
                    .setPhoneNumber(formattedNumber)
                    .setTimeout(VERIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)
                    .build(),
            )
        }

    private suspend fun signInOrLink(credential: PhoneAuthCredential): String {
        val currentUser = firebaseAuth.currentUser
        return try {
            if (currentUser != null && currentUser.isAnonymous) {
                checkNotNull(currentUser.linkWithCredential(credential).await().user).uid
            } else {
                checkNotNull(firebaseAuth.signInWithCredential(credential).await().user).uid
            }
        } catch (collision: FirebaseAuthUserCollisionException) {
            // The phone number already belongs to an account; fall
            // back to a standard sign-in with the updated credential.
            val updated = collision.updatedCredential ?: throw wrap(collision)
            checkNotNull(firebaseAuth.signInWithCredential(updated).await().user).uid
        } catch (throwable: Throwable) {
            throw wrap(throwable)
        }
    }

    private fun wrap(throwable: Throwable): Exception = (throwable as? Exception) ?: Exception.from(throwable, ExceptionMetadata(this))

    private companion object {
        const val VERIFICATION_TIMEOUT_SECONDS = 60L
    }
}
