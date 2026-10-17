//
//  Auth.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.auth.services

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.auth.extensions.FIREBASE_AUTH_ERROR_CODE_KEY
import us.neotechnica.panther.networking.modules.auth.interfaces.AuthDelegate
import us.neotechnica.panther.networking.modules.common.extensions.digits
import us.neotechnica.panther.networking.modules.common.models.GuardedOperation
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * The Firebase Authentication implementation of [AuthDelegate].
 */
class Auth : AuthDelegate {
    // MARK: - Properties

    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }

    // The token with which to request another code for each phone number
    // sent one, keyed by the number in E.164 format.
    private val resendingTokens = ConcurrentHashMap<String, PhoneAuthProvider.ForceResendingToken>()

    // MARK: - AuthDelegate Conformance

    override suspend fun authenticateUser(
        authID: String,
        verificationCode: String,
    ): String {
        GuardedOperation.checkPreconditions(sender = this)

        Networking.config.activityIndicatorDelegate.show()
        return try {
            val credential = PhoneAuthProvider.getCredential(authID, verificationCode)
            signInOrLink(credential)
        } catch (throwable: Throwable) {
            // Guards against malformed input (e.g. a missing session ID),
            // which `getCredential` reports as a raw IllegalArgumentException.
            throw wrap(throwable)
        } finally {
            Networking.config.activityIndicatorDelegate.hide()
        }
    }

    override suspend fun signInAnonymously(): String {
        GuardedOperation.checkPreconditions(sender = this)

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
        GuardedOperation.checkPreconditions(sender = this)

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

    // Each request sends a new code. A repeated request for a
    // number already sent a code passes that code's resending token:
    // without it, Firebase ignores any request for the number until the
    // earlier one times out, reporting neither a code nor a failure.
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
                        resendingTokens[formattedNumber] = token
                        if (continuation.isActive) continuation.resume(verificationID)
                    }

                    // Should a request still go unanswered, the wait ends once
                    // the code's auto-retrieval window closes, rather than never.
                    override fun onCodeAutoRetrievalTimeOut(verificationID: String) {
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

            val options =
                PhoneAuthOptions
                    .newBuilder(firebaseAuth)
                    .setPhoneNumber(formattedNumber)
                    .setTimeout(VERIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)

            resendingTokens[formattedNumber]?.let { options.setForceResendingToken(it) }
            PhoneAuthProvider.verifyPhoneNumber(options.build())
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

    // Captures the Firebase error code under the same user-info key
    // the SDK populates, so reducers can mark user-caused auth
    // failures (invalid input, expired session) as non-reportable.
    private fun wrap(throwable: Throwable): Exception {
        (throwable as? Exception)?.let { return it }
        val errorCode = (throwable as? FirebaseAuthException)?.errorCode ?: return Exception.from(throwable, ExceptionMetadata(this))
        return Exception.from(
            throwable,
            userInfo =
                mapOf(
                    Exception.UserInfo.NS_ERROR_CODE.rawValue to errorCode,
                    FIREBASE_AUTH_ERROR_CODE_KEY to errorCode,
                ),
            metadata = ExceptionMetadata(this),
        )
    }

    private companion object {
        const val VERIFICATION_TIMEOUT_SECONDS = 60L
    }
}
