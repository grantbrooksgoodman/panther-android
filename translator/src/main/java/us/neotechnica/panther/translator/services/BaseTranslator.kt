//
//  BaseTranslator.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

@file:Suppress("FileLength")

package us.neotechnica.panther.translator.services

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.http.SslError
import android.os.SystemClock
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONException
import org.json.JSONTokener
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.translator.Translator
import us.neotechnica.panther.translator.extensions.lowercasedTrimmingWhitespaceAndNewlines
import us.neotechnica.panther.translator.interfaces.Translatorable
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationError
import us.neotechnica.panther.translator.models.TranslationInput
import us.neotechnica.panther.translator.models.TranslationPlatform
import kotlin.coroutines.resume

// The counterpart translator runs to 470 lines in a package without a file-length rule.

/**
 * The web-view scraping harness shared by the translators that
 * have no HTTP API.
 *
 * [translate] loads a platform's translation page in an
 * off-screen, hardened [WebView] attached to the current
 * activity's window (a window-less web view does not reliably run
 * timers or observers), waits for the result to render – signaled
 * by a result-observer web message or page load – then extracts
 * it with the platform's JavaScript selector, retrying against
 * the alternate selector. If the web view's renderer process
 * crashes or is killed to reclaim memory, the translation fails
 * instead of taking the app down with it.
 *
 * Subclasses override [configureWebView] to inject extra scripts
 * and [extractOutput] to customize result extraction.
 */
internal open class BaseTranslator(
    override val platform: TranslationPlatform,
) : Translatorable {
    // MARK: - Types

    protected sealed interface EvaluationResult {
        data class Retry(
            val useAlternateString: Boolean,
        ) : EvaluationResult

        data class Success(
            val output: String,
        ) : EvaluationResult
    }

    // MARK: - Properties

    protected var translationInput: TranslationInput? = null
    protected var translationLanguagePair: LanguagePair? = null

    // MARK: - Translate

    override suspend fun translate(
        input: TranslationInput,
        languagePair: LanguagePair,
    ): Translation {
        val requestURL =
            platform.requestURL(input.value, languagePair)
                ?: throw TranslationError.FailedToGenerateRequestURL
        val activity = Translator.config.currentActivityProvider?.invoke()
        val context =
            activity?.applicationContext?.also { applicationContext.wrappedValue = it }
                ?: applicationContext.wrappedValue
                ?: throw TranslationError.Unknown("Web-view harness unavailable: no application context.")

        translationInput = input
        translationLanguagePair = languagePair

        return withContext(Dispatchers.Main.immediate) {
            runHarness(context, activity, requestURL)
        }
    }

    // MARK: - Overridable Hooks

    /**
     * A hook for subclasses to inject additional document-start
     * scripts.
     */
    protected open fun configureWebView(webView: WebView) {}

    /**
     * Evaluates the platform's extraction script against the rendered
     * page, producing the translation output or a retry directive.
     *
     * @param webView The harness web view.
     * @param useAlternateString Whether to use the platform's
     *   alternate selector.
     */
    protected open suspend fun evaluateJavaScript(
        webView: WebView,
        useAlternateString: Boolean,
    ): EvaluationResult {
        val javaScriptString = if (useAlternateString) platform.alternateJavaScriptString else platform.javaScriptString
        val translationOutput = webView.evaluateJavascriptAwait(javaScriptString)

        if (translationOutput == null || translationOutput.lowercasedTrimmingWhitespaceAndNewlines.isEmpty()) {
            return EvaluationResult.Retry(useAlternateString = !useAlternateString)
        }

        return EvaluationResult.Success(translationOutput)
    }

    /** Adds a document-start script when the feature is supported. */
    internal fun addDocumentStartScript(
        webView: WebView,
        script: String,
    ) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(webView, script, setOf("*"))
        }
    }

    // MARK: - Harness

    private suspend fun runHarness(
        context: Context,
        activity: Activity?,
        requestURL: String,
    ): Translation {
        val webView = createWebView(context)
        val rootView = activity?.findViewById<ViewGroup>(android.R.id.content)
        val ready = CompletableDeferred<Unit>()
        val failure = CompletableDeferred<TranslationError>()
        val rendererGone = CompletableDeferred<TranslationError>()

        return try {
            installResultObserver(webView) { if (!ready.isCompleted) ready.complete(Unit) }
            installScripts(webView)
            configureWebView(webView)
            webView.webViewClient =
                harnessClient(
                    onReady = { if (!ready.isCompleted) ready.complete(Unit) },
                    onFailure = { error -> if (!failure.isCompleted) failure.complete(error) },
                    onRendererGone = { error -> if (!rendererGone.isCompleted) rendererGone.complete(error) },
                )

            rootView?.addView(webView)
            webView.loadUrl(requestURL)

            val navigationError =
                withTimeoutOrNull(NAVIGATION_TIMEOUT_MILLIS) {
                    select<TranslationError?> {
                        ready.onAwait { null }
                        failure.onAwait { it }
                        rendererGone.onAwait { it }
                    }
                } ?: if (ready.isCompleted) null else TranslationError.TimedOut
            if (navigationError != null) throw navigationError

            // A dead renderer never answers a pending script evaluation,
            // so race the evaluation against the loss of the renderer.
            coroutineScope {
                val evaluation = async { beginEvaluatingTranslationResult(webView) }
                select {
                    evaluation.onAwait { it }
                    rendererGone.onAwait { error ->
                        evaluation.cancel()
                        throw error
                    }
                }
            }
        } finally {
            rootView?.removeView(webView)
            webView.destroy()
        }
    }

    // Kicks off result extraction once navigation finishes or the
    // result observer fires, retrying with a brief backoff until the
    // evaluation threshold elapses.
    private suspend fun beginEvaluatingTranslationResult(webView: WebView): Translation {
        val input = translationInput ?: failForMissingValues()
        val languagePair = translationLanguagePair ?: failForMissingValues()

        val navigationFinishedAt = SystemClock.elapsedRealtime()
        var useAlternateString = false

        while (true) {
            when (val evaluationResult = evaluateJavaScript(webView, useAlternateString)) {
                is EvaluationResult.Success ->
                    return Translation(
                        input = input,
                        output = evaluationResult.output,
                        languagePair = languagePair,
                    )

                is EvaluationResult.Retry -> {
                    if (SystemClock.elapsedRealtime() - navigationFinishedAt >= EVALUATION_THRESHOLD_MILLIS) {
                        throw TranslationError.EvaluateJavaScriptFailed()
                    }

                    // Brief backoff between evaluation attempts; the result
                    // observer script surfaces results as soon as they
                    // render, so tight polling only wastes main thread time.
                    useAlternateString = evaluationResult.useAlternateString
                    delay(EVALUATION_BACKOFF_MILLIS)
                }
            }
        }
    }

    private fun failForMissingValues(): Nothing = throw TranslationError.EvaluateJavaScriptFailed("Missing required parameters.")

    // MARK: - Web View Setup

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(context: Context): WebView =
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(1, 1)
            alpha = 0f
            isEnabled = false
            with(settings) {
                javaScriptEnabled = true
                domStorageEnabled = true
                blockNetworkImage = true
                loadsImagesAutomatically = false
                mediaPlaybackRequiresUserGesture = true
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                userAgentString = MOBILE_USER_AGENT
            }

            // Downloads are cancelled: nothing is ever written.
            setDownloadListener { _, _, _, _, _ -> }
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
        addBlockContentFocusScript(webView)
        addContentSecurityPolicyScript(webView)
        addDenyPermissionsScript(webView)
        addDisableAnimationsScript(webView)
        addDisableServiceWorkerScript(webView)
        addFauxVisibilityScript(webView)
        addPromoteIdleCallbackScript(webView)
        if (platform != TranslationPlatform.DEEP_L) addTrimLazyLoadersScript(webView)
        platform.resultObserverScript?.let { addDocumentStartScript(webView, it) }
    }

    // The webkit Lint check reports every `WebViewClient()` constructor
    // call – a Kotlin object expression's supertype call included –
    // whether or not the client overrides `onRenderProcessGone`. This
    // one does.
    @SuppressLint("MissingOnRenderProcessGone")
    private fun harnessClient(
        onReady: () -> Unit,
        onFailure: (TranslationError) -> Unit,
        onRendererGone: (TranslationError) -> Unit,
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

            // Fonts, images, media, trackers, and non-HTML documents are
            // never fetched.
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest,
            ): WebResourceResponse? = if (request.isBlocked) WebResourceResponse(null, null, null) else null

            // The harness scrapes public translation pages over
            // connections whose certificate trust the system verifies;
            // a handshake the web view cannot validate itself proceeds.
            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError,
            ) {
                handler.proceed()
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                if (request?.isForMainFrame != true) return
                onFailure(
                    TranslationError.WebViewNavigationFailed(
                        "${error?.description ?: "An unknown error occurred."} (${error?.errorCode ?: -1})",
                    ),
                )
            }

            // Returning true keeps the app alive when the renderer
            // crashes or is killed to reclaim memory. The web view can't
            // be used again, so it leaves the hierarchy and is destroyed
            // here; the harness's own cleanup then finds nothing to do.
            override fun onRenderProcessGone(
                view: WebView,
                detail: RenderProcessGoneDetail,
            ): Boolean {
                (view.parent as? ViewGroup)?.removeView(view)
                view.destroy()
                onRendererGone(renderProcessGoneError(detail))
                return true
            }
        }

    // MARK: - Auxiliary

    // Whether the request is for a resource the harness never needs:
    // a font, an image or media file, a known tracker, or a document
    // that is not HTML.
    private val WebResourceRequest.isBlocked: Boolean
        get() {
            val url = url ?: return false
            val path = url.path?.lowercase().orEmpty()
            val host = url.host?.lowercase().orEmpty()
            val accept = requestHeaders?.get("Accept")?.lowercase().orEmpty()

            if (BLOCKED_EXTENSIONS.any { path.endsWith(it) }) return true
            if (accept.startsWith("image/") || accept.startsWith("audio/") || accept.startsWith("video/")) return true
            if (isForMainFrame && NON_HTML_DOCUMENT_EXTENSIONS.any { path.endsWith(it) }) return true

            return TRACKER_RULES.any { (hosts, pathPrefix) ->
                hosts.any { host == it || host.endsWith(".$it") } && (pathPrefix == null || path.startsWith(pathPrefix))
            }
        }

    protected suspend fun WebView.evaluateJavascriptAwait(script: String): String? =
        suspendCancellableCoroutine { continuation ->
            evaluateJavascript(script) { value -> continuation.resume(decodeJavascriptString(value)) }
        }

    private fun renderProcessGoneError(detail: RenderProcessGoneDetail): TranslationError =
        TranslationError.Unknown(
            if (detail.didCrash()) {
                "The web view's renderer process crashed."
            } else {
                "The web view's renderer process was killed to reclaim memory."
            },
        )

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
        internal const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/131.0.0.0 Mobile Safari/537.36"

        private val applicationContext = LockIsolated<Context?>(null)

        private val BLOCKED_EXTENSIONS =
            listOf(
                ".woff",
                ".woff2",
                ".ttf",
                ".otf",
                ".eot",
                ".png",
                ".jpg",
                ".jpeg",
                ".gif",
                ".webp",
                ".svg",
                ".ico",
                ".bmp",
                ".avif",
                ".mp3",
                ".mp4",
                ".m4a",
                ".m4v",
                ".webm",
                ".ogg",
                ".wav",
                ".mov",
            )

        private val NON_HTML_DOCUMENT_EXTENSIONS = listOf(".pdf", ".zip", ".dmg", ".apk", ".exe")

        // Host suffixes and path prefixes of third-party trackers.
        private val TRACKER_RULES: List<Pair<List<String>, String?>> =
            listOf(
                listOf("googletagmanager.com") to "/gtm.js",
                listOf("google-analytics.com") to "/analytics.js",
                listOf("google-analytics.com") to "/gtag/js",
                listOf("google-analytics.com", "stats.g.doubleclick.net") to "/collect",
                listOf("doubleclick.net") to null,
                listOf("facebook.com") to "/tr",
                listOf("hotjar.com") to null,
                listOf("segment.com", "cdn.segment.com") to "/analytics.js",
            )

        private const val EVALUATION_BACKOFF_MILLIS = 100L
        private const val EVALUATION_THRESHOLD_MILLIS = 10_000L
        private const val NAVIGATION_TIMEOUT_MILLIS = 10_000L
        private const val PREWARM_UNLOAD_DELAY_MILLIS = 2_000L

        // Keeps the app alive if a prewarm web view's renderer dies; the
        // delayed unload then finds the web view already destroyed. The
        // Lint suppression is the same false positive as the harness
        // client's.
        @SuppressLint("MissingOnRenderProcessGone")
        private val prewarmClient =
            object : WebViewClient() {
                override fun onRenderProcessGone(
                    view: WebView,
                    detail: RenderProcessGoneDetail,
                ): Boolean {
                    view.destroy()
                    return true
                }
            }

        /**
         * Warms the DNS and TLS sessions for the given platforms'
         * translation endpoints, briefly loading each platform's page
         * in a disposable web view.
         *
         * @param platforms The platforms to prewarm connections for.
         */
        fun prewarm(platforms: List<TranslationPlatform>) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

            runCatching {
                scope.launch {
                    val apiWarmupURLStrings = mutableListOf<String>()
                    if (platforms.contains(TranslationPlatform.GOOGLE)) {
                        apiWarmupURLStrings.add(GoogleTranslator.API_WARMUP_URL_STRING)
                    }

                    if (platforms.contains(TranslationPlatform.REVERSO)) {
                        apiWarmupURLStrings.add(ReversoTranslator.API_WARMUP_URL_STRING)
                    }

                    for (apiWarmupURLString in apiWarmupURLStrings) {
                        launch(Dispatchers.IO) { runCatching { NetworkClient.get(apiWarmupURLString) } }
                    }

                    val activity = Translator.config.currentActivityProvider?.invoke() ?: return@launch
                    for (platform in platforms) {
                        val webView = WebView(activity)
                        webView.webViewClient = prewarmClient
                        webView.loadUrl(platform.prewarmURL)

                        launch {
                            delay(PREWARM_UNLOAD_DELAY_MILLIS)
                            webView.stopLoading()
                            webView.destroy()
                        }
                    }
                }
            }
        }
    }
}
