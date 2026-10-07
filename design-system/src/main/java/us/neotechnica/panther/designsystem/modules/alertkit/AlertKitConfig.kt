//
//  AlertKitConfig.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.yield
import us.neotechnica.panther.designsystem.modules.alertkit.interfaces.ReportDelegate
import us.neotechnica.panther.designsystem.modules.alertkit.interfaces.TranslationDelegate
import us.neotechnica.panther.designsystem.modules.alertkit.models.HUDConfig
import us.neotechnica.panther.designsystem.modules.alertkit.models.TranslationTimeoutConfig
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import kotlin.time.Duration.Companion.seconds

/**
 * The shared configuration for AlertKit.
 *
 * Use [AlertKitConfig] to register the translation delegate that
 * AlertKit calls to translate alert content before presentation, and
 * to read the source and target languages for those translations.
 */
object AlertKitConfig {
    // MARK: - Properties

    /** The registered report delegate, or `null` if none. */
    var reportDelegate: ReportDelegate? = null
        private set

    /** The registered translation delegate, or `null` if none. */
    var translationDelegate: TranslationDelegate? = null
        private set

    /**
     * The configuration for the heads-up display shown during
     * translation.
     *
     * The default configuration displays the HUD after 2 seconds
     * with modal behavior.
     */
    var translationHUDConfig: HUDConfig =
        HUDConfig(
            appearsAfter = 2.seconds,
            isModal = true,
        )
        private set

    /**
     * The configuration that controls translation timeout behavior.
     *
     * The default waits 10 seconds and falls back to the original
     * untranslated strings on timeout.
     */
    var translationTimeoutConfig: TranslationTimeoutConfig =
        TranslationTimeoutConfig(
            duration = 10.seconds,
            returnsInputsOnFailure = true,
        )
        private set

    // MARK: - Properties

    private var sourceLanguageCodeOverride: String? = null
    private var targetLanguageCodeOverride: String? = null

    // MARK: - Computed Properties

    /** The ISO 639-1 code of the source language for translations. */
    val sourceLanguageCode: String
        get() = sourceLanguageCodeOverride ?: "en"

    /** The ISO 639-1 code of the target language for translations. */
    val targetLanguageCode: String
        get() = targetLanguageCodeOverride ?: RuntimeStorage.languageCode

    // MARK: - Methods

    /**
     * Overrides the source language code used for translations.
     *
     * @param sourceLanguageCode The ISO 639-1 language code to use as
     *   the source language.
     */
    fun overrideSourceLanguageCode(sourceLanguageCode: String) {
        sourceLanguageCodeOverride = sourceLanguageCode
    }

    /**
     * Overrides the target language code used for translations.
     *
     * @param targetLanguageCode The ISO 639-1 language code to use as
     *   the target language.
     */
    fun overrideTargetLanguageCode(targetLanguageCode: String) {
        targetLanguageCodeOverride = targetLanguageCode
    }

    /**
     * Overrides the translation HUD configuration.
     *
     * @param translationHUDConfig The HUD configuration to use.
     */
    fun overrideTranslationHUDConfig(translationHUDConfig: HUDConfig) {
        this.translationHUDConfig = translationHUDConfig
    }

    /**
     * Overrides the translation timeout configuration.
     *
     * @param translationTimeoutConfig The timeout configuration to use.
     */
    fun overrideTranslationTimeoutConfig(translationTimeoutConfig: TranslationTimeoutConfig) {
        this.translationTimeoutConfig = translationTimeoutConfig
    }

    /**
     * Registers the given translation delegate.
     *
     * @param translationDelegate The delegate to register.
     */
    fun registerTranslationDelegate(translationDelegate: TranslationDelegate) {
        this.translationDelegate = translationDelegate
    }

    /**
     * Registers the delegate that files error reports.
     *
     * @param reportDelegate The delegate to register.
     */
    fun registerReportDelegate(reportDelegate: ReportDelegate) {
        this.reportDelegate = reportDelegate
    }

    // MARK: - Internal

    internal suspend fun getTranslations(inputs: List<TranslationInput>): List<Translation> {
        val delegate = translationDelegate ?: return emptyList()
        return try {
            delegate.getTranslations(
                inputs = inputs,
                languagePair = LanguagePair(from = sourceLanguageCode, to = targetLanguageCode),
                hudConfig = translationHUDConfig,
                timeoutConfig = translationTimeoutConfig,
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            throw AlertKit.Error.TranslationFailed(throwable.message ?: throwable.toString(), throwable)
        }
    }

    internal suspend fun <A, R> presentWithTranslation(
        shouldTranslate: Boolean,
        presentDirectly: suspend () -> R,
        translate: suspend () -> A,
        presentTranslated: suspend (A) -> R,
    ): R {
        if (!shouldTranslate) return presentDirectly()

        // Yield to the main dispatcher so pending UI work can complete
        // before a potentially long-running translation begins.
        yield()

        return try {
            presentTranslated(translate())
        } catch (_: CancellationException) {
            Logger.log(
                "Translation cancelled; presenting untranslated content.",
                domain = LoggerDomain.alertKit,
            )

            presentDirectly()
        } catch (throwable: Throwable) {
            Logger.log(
                Exception.from(throwable, ExceptionMetadata(this)),
                domain = LoggerDomain.alertKit,
            )

            presentDirectly()
        }
    }
}
