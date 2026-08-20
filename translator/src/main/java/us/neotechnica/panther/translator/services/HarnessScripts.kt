//
//  HarnessScripts.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.services

import us.neotechnica.panther.translator.models.TranslationPlatform

/**
 * The document-start JavaScript the web-view harness injects to keep
 * scraped translation pages lean and headless-friendly.
 *
 * The scripts are ported verbatim from the iOS `BaseTranslator`
 * hardening scripts: they suppress focus, block image/media loads via
 * a page CSP, deny permission prompts, disable animations and service
 * workers, fake a visible/idle foreground state, and (for every
 * platform except DeepL) neutralize lazy-loading observers.
 */
internal object HarnessScripts {
    // MARK: - Methods

    /** The hardening scripts to inject for the given [platform]. */
    fun hardeningScripts(platform: TranslationPlatform): List<String> {
        val scripts =
            mutableListOf(
                BLOCK_CONTENT_FOCUS,
                CONTENT_SECURITY_POLICY,
                DENY_PERMISSIONS,
                DISABLE_ANIMATIONS,
                DISABLE_SERVICE_WORKER,
                FAUX_VISIBILITY,
                PROMOTE_IDLE_CALLBACK,
            )
        if (platform != TranslationPlatform.DEEP_L) scripts.add(TRIM_LAZY_LOADERS)
        return scripts
    }

    // MARK: - Scripts

    private val BLOCK_CONTENT_FOCUS =
        """
        (function() {
          const block = (e) => {
            const t = e.target;
            if (!t) return;
            if (t.matches && (t.matches('input, textarea, select, [contenteditable]'))) {
              try { t.blur(); } catch (e) {}
              e.stopImmediatePropagation();
              e.preventDefault();
            }
          };
          document.addEventListener('focus', block, true);
          document.addEventListener('focusin', block, true);
          const proto = HTMLElement.prototype;
          const origFocus = proto.focus;
          proto.focus = function(...args) {
            if (this.matches && (this.matches('input, textarea, select, [contenteditable], [tabindex]'))) {
              return;
            }
            return origFocus.apply(this, args);
          };
        })();
        """.trimIndent()

    private val CONTENT_SECURITY_POLICY =
        """
        (function () {
          var m = document.createElement('meta');
          m.httpEquiv = 'Content-Security-Policy';
          m.content = "img-src 'none'; media-src 'none'";
          document.head.appendChild(m);
        })();
        """.trimIndent()

    private val DENY_PERMISSIONS =
        """
        (function(){
          try { Notification.requestPermission = () => Promise.resolve('denied'); } catch {}
          Object.defineProperty(Notification, 'permission', { get: ()=>'denied' });
          if (navigator && navigator.permissions && navigator.permissions.query) {
            const orig = navigator.permissions.query.bind(navigator.permissions);
            navigator.permissions.query = (desc) => {
              if (!desc || !desc.name) return orig(desc);
              if (['notifications','geolocation','camera','microphone','clipboard-read','background-sync'].includes(desc.name)) {
                return Promise.resolve({ state:'denied' });
              }
              return orig(desc);
            };
          }
        })();
        """.trimIndent()

    private val DISABLE_ANIMATIONS =
        """
        (function(){
          var s=document.createElement('style');
          s.textContent='*{animation:none!important;transition:none!important;scroll-behavior:auto!important}';
          document.documentElement.appendChild(s);
        })();
        """.trimIndent()

    private val DISABLE_SERVICE_WORKER =
        """
        try {
          const orig = navigator.serviceWorker && navigator.serviceWorker.register;
          if (orig) {
            navigator.serviceWorker.register = function() {
              return Promise.reject(new Error('ServiceWorker disabled'));
            };
          }
        } catch(e) {}
        """.trimIndent()

    private val FAUX_VISIBILITY =
        """
        (function(){
          try {
            Object.defineProperty(document, 'hidden', { get: () => false });
            Object.defineProperty(document, 'visibilityState', { get: () => 'visible' });
            if ('webkitHidden' in document) {
              Object.defineProperty(document, 'webkitHidden', { get: () => false });
            }
            setTimeout(() => {
              document.dispatchEvent(new Event('visibilitychange'));
              window.dispatchEvent(new Event('pageshow'));
            }, 0);
            if (navigator && navigator.connection) {
              try {
                Object.defineProperty(navigator.connection, 'saveData', { get: () => false });
                Object.defineProperty(navigator.connection, 'effectiveType', { get: () => '4g' });
              } catch {}
            }
          } catch {}
        })();
        """.trimIndent()

    private val PROMOTE_IDLE_CALLBACK =
        """
        (function(){
          window.requestIdleCallback = function(cb){
            return setTimeout(() => cb({
              didTimeout: false,
              timeRemaining: function(){ return 50; }
            }), 0);
          };
          window.cancelIdleCallback = function(id){ clearTimeout(id); };
        })();
        """.trimIndent()

    private val TRIM_LAZY_LOADERS =
        """
        (function(){
          const IO = window.IntersectionObserver;
          if (IO) {
            window.IntersectionObserver = function(cb){ this.observe = function(t){ cb([{isIntersecting:true, target:t}], this); }; this.unobserve = function(){}; this.disconnect=function(){}; };
          }
        })();
        """.trimIndent()
}
