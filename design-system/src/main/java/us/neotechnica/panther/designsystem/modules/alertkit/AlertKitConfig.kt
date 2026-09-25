//
//  AlertKitConfig.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 24/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit

import us.neotechnica.panther.designsystem.modules.alertkit.interfaces.TranslationDelegate
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * The shared configuration for AlertKit.
 *
 * Use [AlertKitConfig] to register the translation delegate that
 * AlertKit calls to translate alert content before presentation, and
 * to read the source and target languages for those translations.
 */
object AlertKitConfig {
    // MARK: - Properties

    /** The registered translation delegate, or `null` if none. */
    var translationDelegate: TranslationDelegate? = null
        private set

    // MARK: - Computed Properties

    /** The ISO 639-1 code of the source language for translations. */
    val sourceLanguageCode: String
        get() = "en"

    /** The ISO 639-1 code of the target language for translations. */
    val targetLanguageCode: String
        get() = RuntimeStorage.languageCode

    // MARK: - Methods

    /**
     * Registers the given translation delegate.
     *
     * @param translationDelegate The delegate to register.
     */
    fun registerTranslationDelegate(translationDelegate: TranslationDelegate) {
        this.translationDelegate = translationDelegate
    }

    // MARK: - Internal

    internal suspend fun getTranslations(inputs: List<TranslationInput>): List<Translation> {
        val delegate = translationDelegate ?: return emptyList()
        return delegate.getTranslations(
            inputs = inputs,
            languagePair = LanguagePair(from = sourceLanguageCode, to = targetLanguageCode),
        )
    }

    internal suspend fun <A, R> presentWithTranslation(
        shouldTranslate: Boolean,
        presentDirectly: suspend () -> R,
        translate: suspend () -> A,
        presentTranslated: suspend (A) -> R,
    ): R {
        if (!shouldTranslate) return presentDirectly()
        return try {
            presentTranslated(translate())
        } catch (exception: Exception) {
            Logger.log(exception)
            presentDirectly()
        }
    }
}
