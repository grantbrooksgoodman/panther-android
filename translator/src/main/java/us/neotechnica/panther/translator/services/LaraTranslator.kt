//
//  LaraTranslator.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.services

import android.webkit.WebView
import us.neotechnica.panther.translator.models.TranslationPlatform

/**
 * Translates using Lara through the [BaseTranslator] web-view harness.
 *
 * Lara renders its result inside an iframe, so an extra document-start
 * script (injected into all frames) watches the iframe and relays the
 * result to the top frame, where the platform's result-observer script
 * stashes it on `window.__translatorResult`. Extraction then reads
 * that value.
 */
internal class LaraTranslator : BaseTranslator(TranslationPlatform.LARA) {
    // MARK: - Configuration

    override fun configureWebView(webView: WebView) {
        addDocumentStartScript(webView, IFRAME_RELAY_SCRIPT)
    }

    // MARK: - Extraction Override

    // Lara's result is relayed to `window.__translatorResult`; read it
    // directly rather than scraping the DOM.
    override suspend fun extractOutput(
        webView: WebView,
        useAlternate: Boolean,
    ): String? = webView.evaluateJavascriptAwait(RESULT_SLOT_SCRIPT)

    // MARK: - Companion

    companion object {
        private const val RESULT_SLOT_SCRIPT =
            "(function(){ return window.__translatorResult || ''; })();"

        private val IFRAME_RELAY_SCRIPT =
            """
            (function() {
                if (window === window.top) return;
                var className = 'knownFragmentElementNode';
                var timer = setInterval(function() {
                    var el = document.getElementsByClassName(className)[0];
                    if (el && el.innerText && el.innerText.trim()) {
                        clearInterval(timer);
                        window.top.postMessage(
                            JSON.stringify({ type: 'laraTranslation', text: el.innerText.trim() }),
                            '*'
                        );
                    }
                }, 200);
                setTimeout(function() { clearInterval(timer); }, 10000);
            })();
            """.trimIndent()
    }
}
