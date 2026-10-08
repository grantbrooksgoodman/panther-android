//
//  AlertKitTranslationService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.sanitized
import us.neotechnica.panther.designsystem.modules.alertkit.interfaces.TranslationDelegate
import us.neotechnica.panther.designsystem.modules.alertkit.models.HUDConfig
import us.neotechnica.panther.designsystem.modules.alertkit.models.TranslationTimeoutConfig
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import kotlin.coroutines.cancellation.CancellationException

/**
 * Supplies AlertKit's translations by forwarding to the hosted
 * translation archive.
 *
 * A heads-up display appears if the translation outlasts the HUD
 * configuration's delay, and a translation that exceeds the timeout
 * either falls back to the original untranslated strings or fails,
 * according to the timeout configuration.
 */
object AlertKitTranslationService : TranslationDelegate {
    override suspend fun getTranslations(
        inputs: List<TranslationInput>,
        languagePair: LanguagePair,
        hudConfig: HUDConfig?,
        timeoutConfig: TranslationTimeoutConfig,
    ): List<Translation> =
        coroutineScope {
            val hudJob =
                hudConfig?.let { config ->
                    launch {
                        delay(config.appearsAfter)
                        HUD.showProgress(isModal = config.isModal)
                    }
                }

            val fallbackTranslations =
                inputs.map { Translation(input = it, output = it.original.sanitized, languagePair = languagePair) }

            try {
                val result =
                    withTimeoutOrNull(timeoutConfig.duration) {
                        try {
                            Result.success(
                                Networking.config.hostedTranslationDelegate.getTranslations(inputs, languagePair),
                            )
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (throwable: Throwable) {
                            Result.failure(
                                (throwable as? Exception)
                                    ?: Exception.from(throwable, ExceptionMetadata(this@AlertKitTranslationService)),
                            )
                        }
                    }

                when {
                    result == null -> {
                        if (!timeoutConfig.returnsInputsOnFailure) throw timedOutException()
                        Logger.log(timedOutException(), domain = LoggerDomain.Networking.hostedTranslation)
                        fallbackTranslations
                    }

                    result.isSuccess -> {
                        val translations = result.getOrThrow()
                        if (translations.size != inputs.size) {
                            throw Exception("Mismatched ratio returned.", metadata = ExceptionMetadata(this@AlertKitTranslationService))
                        }
                        translations
                    }

                    else -> {
                        val exception = result.toException()
                        if (!timeoutConfig.returnsInputsOnFailure) throw exception
                        Logger.log(exception, domain = LoggerDomain.Networking.hostedTranslation)
                        fallbackTranslations
                    }
                }
            } finally {
                hudJob?.cancel()
                if (hudConfig != null) HUD.hide()
            }
        }

    private fun Result<List<Translation>>.toException(): Exception {
        val throwable = exceptionOrNull()
        return throwable as? Exception ?: Exception.from(
            throwable ?: return Exception("Translation failed.", metadata = ExceptionMetadata(this@AlertKitTranslationService)),
            ExceptionMetadata(this@AlertKitTranslationService),
        )
    }

    private fun timedOutException(): Exception =
        Exception(
            "The operation timed out.",
            userInfo = mapOf(Exception.UserInfo.STATIC_ERROR_CODE.rawValue to TIMED_OUT_ERROR_CODE),
            metadata = ExceptionMetadata(this),
        )

    private const val TIMED_OUT_ERROR_CODE = "801F"
}
