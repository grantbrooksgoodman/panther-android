//
//  BaseTranslator.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.services

import android.annotation.SuppressLint
import android.app.Activity
import android.os.SystemClock
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONException
import org.json.JSONTokener
import us.neotechnica.panther.translator.Translator
import us.neotechnica.panther.translator.extensions.lowercasedTrimmingWhitespaceAndNewlines
import us.neotechnica.panther.translator.interfaces.Translatorable
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationError
import us.neotechnica.panther.translator.models.TranslationInput
import us.neotechnica.panther.translator.models.TranslationPlatform
import kotlin.coroutines.resume

/**
 * The web-view scraping harness shared by the translators that have no
 * HTTP API.
 *
 * [translate] loads a platform's translation page in an off-screen,
 * hardened [WebView] attached to the current activity's window (a
 * window-less web view does not reliably run timers or observers),
 * waits for the result to render – signaled by a result-observer web
 * message or page load – then extracts it with the platform's
 * JavaScript selector, retrying against the alternate selector.
 *
 * Subclasses override [configureWebView] to inject extra scripts and
 * [extractOutput] to customize result extraction.
 */
internal open class BaseTranslator(
    override val platform: TranslationPlatform,
) : Translatorable {
    // MARK: - Properties

    protected var translationInput: TranslationInput? = null
    protected var translationLanguagePair: LanguagePair? = null

    // MARK: - Translate

    override suspend fun translate(
        input: TranslationInput,
        languagePair: LanguagePair,
    ): Translation {
        val requestUrl =
            platform.requestUrl(input.value, languagePair)
                ?: throw TranslationError.FailedToGenerateRequestURL
        val activity =
            Translator.config.currentActivityProvider?.invoke()
                ?: throw TranslationError.Unknown("Web-view harness unavailable: no current activity.")

        translationInput = input
        translationLanguagePair = languagePair

        return withContext(Dispatchers.Main.immediate) {
            withTimeoutOrNull(HARNESS_TIMEOUT_MILLIS) {
                runHarness(activity, requestUrl)
            } ?: throw TranslationError.TimedOut
        }
    }

    // MARK: - Overridable Hooks

    /** A hook for subclasses to inject additional document-start scripts. */
    protected open fun configureWebView(webView: WebView) {}

    /**
     * Extracts the rendered translation output, or `null` if it is not
     * yet available.
     *
     * @param webView The harness web view.
     * @param useAlternate Whether to use the platform's alternate
     *   selector.
     */
    protected open suspend fun extractOutput(
        webView: WebView,
        useAlternate: Boolean,
    ): String? {
        val script = if (useAlternate) platform.alternateJavaScriptString else platform.javaScriptString
        return webView.evaluateJavascriptAwait(script)
    }

    /** Adds a document-start script when the feature is supported. */
    protected fun addDocumentStartScript(
        webView: WebView,
        script: String,
    ) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(webView, script, setOf("*"))
        }
    }

    // MARK: - Harness

    private suspend fun runHarness(
        activity: Activity,
        requestUrl: String,
    ): Translation {
        val webView = createWebView(activity)
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        val ready = CompletableDeferred<Unit>()
        val failure = CompletableDeferred<TranslationError>()

        return try {
            installResultObserver(webView) { if (!ready.isCompleted) ready.complete(Unit) }
            installScripts(webView)
            configureWebView(webView)
            webView.webViewClient =
                harnessClient(
                    onReady = { if (!ready.isCompleted) ready.complete(Unit) },
                    onFailure = { error -> if (!failure.isCompleted) failure.complete(error) },
                )

            rootView?.addView(webView)
            webView.loadUrl(requestUrl)

            val navigationError =
                select {
                    ready.onAwait { null }
                    failure.onAwait { it }
                }
            if (navigationError != null) throw navigationError

            extractLoop(webView)
        } finally {
            rootView?.removeView(webView)
            webView.destroy()
        }
    }

    private suspend fun extractLoop(webView: WebView): Translation {
        val input = translationInput ?: throw TranslationError.EvaluateJavaScriptFailed("Missing required parameters.")
        val languagePair =
            translationLanguagePair
                ?: throw TranslationError.EvaluateJavaScriptFailed("Missing required parameters.")

        val deadline = SystemClock.elapsedRealtime() + EXTRACTION_THRESHOLD_MILLIS
        var useAlternate = false

        while (true) {
            val output = extractOutput(webView, useAlternate)
            if (output != null && output.lowercasedTrimmingWhitespaceAndNewlines().isNotEmpty()) {
                return Translation(input = input, output = output, languagePair = languagePair)
            }

            if (SystemClock.elapsedRealtime() >= deadline) throw TranslationError.EvaluateJavaScriptFailed()

            useAlternate = !useAlternate
            delay(EXTRACTION_BACKOFF_MILLIS)
        }
    }

    // MARK: - Web View Setup

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(activity: Activity): WebView =
        WebView(activity).apply {
            layoutParams = ViewGroup.LayoutParams(1, 1)
            alpha = 0f
            isEnabled = false
            with(settings) {
                javaScriptEnabled = true
                domStorageEnabled = true
                blockNetworkImage = true
                loadsImagesAutomatically = false
                mediaPlaybackRequiresUserGesture = true
                userAgentString = MOBILE_USER_AGENT
            }
        }

    private fun installResultObserver(
        webView: WebView,
        onMessage: () -> Unit,
    ) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return
        WebViewCompat.addWebMessageListener(
            webView,
            Translator.Constants.RESULT_OBSERVER_MESSAGE_HANDLER_NAME,
            setOf("*"),
        ) { _, _, _, _, _ -> onMessage() }
    }

    private fun installScripts(webView: WebView) {
        for (script in HarnessScripts.hardeningScripts(platform)) {
            addDocumentStartScript(webView, script)
        }
        platform.resultObserverScript?.let { addDocumentStartScript(webView, it) }
    }

    private fun harnessClient(
        onReady: () -> Unit,
        onFailure: (TranslationError) -> Unit,
    ): WebViewClient =
        object : WebViewClient() {
            override fun onPageFinished(
                view: WebView,
                url: String?,
            ) {
                if (url != null && url.startsWith(Translator.Constants.GOOGLE_CONSENT_URL_STRING)) {
                    view.evaluateJavascript(Translator.Constants.GOOGLE_CONSENT_JAVA_SCRIPT_STRING, null)
                    return
                }
                onReady()
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                if (request?.isForMainFrame != true) return
                onFailure(TranslationError.WebViewNavigationFailed(error?.description?.toString() ?: "Unknown error."))
            }
        }

    // MARK: - Auxiliary

    protected suspend fun WebView.evaluateJavascriptAwait(script: String): String? =
        suspendCancellableCoroutine { continuation ->
            evaluateJavascript(script) { value -> continuation.resume(decodeJavascriptString(value)) }
        }

    private fun decodeJavascriptString(raw: String?): String? {
        if (raw == null || raw == "null") return null
        return try {
            JSONTokener(raw).nextValue() as? String
        } catch (_: JSONException) {
            raw
        }
    }

    // MARK: - Companion

    companion object {
        private const val HARNESS_TIMEOUT_MILLIS = 20_000L
        private const val EXTRACTION_THRESHOLD_MILLIS = 10_000L
        private const val EXTRACTION_BACKOFF_MILLIS = 100L
        private const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/131.0.0.0 Mobile Safari/537.36"
    }
}
