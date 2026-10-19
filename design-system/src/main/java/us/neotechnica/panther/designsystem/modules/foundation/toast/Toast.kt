//
//  Toast.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.toast

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CancellationException
import us.neotechnica.panther.designsystem.modules.alertkit.AlertKitConfig
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.firstOutput
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.ToastStyle
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.TranslationInput
import kotlin.time.Duration

/**
 * A lightweight, non-modal notification that appears over the
 * current content.
 *
 * Use [Toast] to present brief messages – such as confirmation of
 * an action, a warning, or an error – without interrupting the
 * user's workflow. A toast can appear as a full-width banner or a
 * compact capsule:
 *
 * ```kotlin
 * Toast.show(Toast(Toast.ToastType.Banner(ToastStyle.SUCCESS), message = "Item saved."))
 * Toast.show(Toast(Toast.ToastType.Capsule(ToastStyle.ERROR), message = "Upload failed."))
 * ```
 *
 * By default a toast is [PerpetuationStrategy.Persistent] and
 * remains on screen until the user dismisses it. Use
 * [PerpetuationStrategy.Ephemeral] to auto-dismiss after a
 * specified duration.
 *
 * @property type The presentation type. The default is a plain
 *   banner.
 * @property title An optional headline. Pass `null` to omit the
 *   title.
 * @property message The body text to display.
 * @property perpetuation The duration strategy. The default is
 *   [PerpetuationStrategy.Persistent].
 */
data class Toast(
    val type: ToastType = ToastType.Banner(),
    val title: String? = null,
    val message: String,
    val perpetuation: PerpetuationStrategy = PerpetuationStrategy.Persistent,
) {
    // MARK: - Types

    /** The screen edge from which a banner toast slides into view. */
    enum class AppearanceEdge {
        /** The banner appears from the bottom of the screen. */
        BOTTOM,

        /** The banner appears from the top of the screen. */
        TOP,
    }

    /**
     * A set of colors that customizes a banner toast's appearance.
     *
     * Pass `null` for any component to keep its default color.
     */
    class ColorPalette(
        /** The accent color for the icon and strip. */
        val accent: Color? = null,
        /** The banner's background color. */
        val background: Color? = null,
        /** The dismiss button's color. */
        val dismissButton: Color? = null,
        /** The title and message text color. */
        val text: Color? = null,
    )

    /**
     * The visual presentation type of a toast.
     *
     * A toast can appear as either a full-width [Banner] or a
     * compact [Capsule]. Banners offer additional customization
     * through an appearance edge, a color palette, and a dismiss
     * button.
     */
    sealed interface ToastType {
        /**
         * The semantic style, which determines the icon and
         * color.
         */
        val style: ToastStyle

        /**
         * A full-width banner that slides in from the specified
         * edge.
         *
         * @property style The semantic style. The default is
         *   [ToastStyle.NONE].
         * @property appearanceEdge The screen edge the banner
         *   appears from. The default is [AppearanceEdge.TOP].
         * @property colorPalette An optional custom color palette.
         * @property showsDismissButton Whether to show a dismiss
         *   button. The default is `true`.
         */
        data class Banner(
            override val style: ToastStyle = ToastStyle.NONE,
            val appearanceEdge: AppearanceEdge = AppearanceEdge.TOP,
            val colorPalette: ColorPalette? = null,
            val showsDismissButton: Boolean = true,
        ) : ToastType

        /**
         * A compact, pill-shaped notification.
         *
         * @property style The semantic style. The default is
         *   [ToastStyle.NONE].
         */
        data class Capsule(
            override val style: ToastStyle = ToastStyle.NONE,
        ) : ToastType
    }

    /** A value that identifies a translatable part of a [Toast]. */
    sealed interface TranslationOptionKey {
        /** The toast's body text. */
        data object Message : TranslationOptionKey

        /** The toast's headline. */
        data object Title : TranslationOptionKey

        companion object {
            /** Every translatable part of a toast. */
            val allCases: List<TranslationOptionKey> = listOf(Message, Title)
        }
    }

    /**
     * The strategy that controls how long a toast remains
     * visible.
     */
    sealed interface PerpetuationStrategy {
        /** The toast auto-dismisses after the given duration. */
        data class Ephemeral(
            val duration: Duration,
        ) : PerpetuationStrategy

        /**
         * The toast remains on screen until the user dismisses
         * it.
         */
        data object Persistent : PerpetuationStrategy
    }

    // MARK: - Translation

    private suspend fun translated(
        keys: List<TranslationOptionKey>,
        languagePair: LanguagePair,
    ): Toast {
        val translationDelegate = AlertKitConfig.translationDelegate ?: return this
        val inputs = mutableListOf<TranslationInput>()
        for (key in keys.distinct()) {
            when (key) {
                TranslationOptionKey.Message -> inputs.add(TranslationInput(message))
                TranslationOptionKey.Title -> title?.let { inputs.add(TranslationInput(it)) }
            }
        }
        if (inputs.isEmpty()) return this

        val translations =
            translationDelegate.getTranslations(
                inputs = inputs.distinctBy { it.value },
                languagePair = languagePair,
                hudConfig = AlertKitConfig.translationHUDConfig,
                timeoutConfig = AlertKitConfig.translationTimeoutConfig,
            )
        return copy(
            title = title?.let { translations.firstOutput(it) },
            message = translations.firstOutput(message),
        )
    }

    // MARK: - Companion

    companion object {
        /**
         * Presents the given toast, optionally invoking a closure
         * when the user taps it.
         *
         * If a toast identical to the one currently on screen is
         * requested, the call is silently ignored.
         *
         * @param toast The toast to present.
         * @param onTap A closure executed when the user taps the
         *   toast, or `null` for a non-interactive toast.
         */
        fun show(
            toast: Toast,
            onTap: (() -> Unit)? = null,
        ) {
            ToastPresenter.show(toast, onTap)
        }

        /**
         * Translates the toast's content according to
         * [translating], then presents it.
         *
         * When [translating] is empty, or no translation delegate
         * is registered, the toast is shown immediately with its
         * original strings. If translation fails, the original
         * untranslated toast is shown as a fallback and the error
         * is logged.
         *
         * @param toast The toast to present.
         * @param translating The parts of the toast to translate.
         * @param languagePair The language pair for translation.
         *   The default translates from the configured source
         *   language to the configured target language.
         * @param onTap A closure executed when the user taps the
         *   toast, or `null` for a non-interactive toast.
         */
        suspend fun show(
            toast: Toast,
            translating: List<TranslationOptionKey>,
            languagePair: LanguagePair =
                LanguagePair(
                    from = AlertKitConfig.sourceLanguageCode,
                    to = AlertKitConfig.targetLanguageCode,
                ),
            onTap: (() -> Unit)? = null,
        ) {
            if (translating.isEmpty() || AlertKitConfig.translationDelegate == null) {
                return ToastPresenter.show(toast, onTap)
            }

            val translated =
                try {
                    toast.translated(translating, languagePair)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    Logger.log(Exception.from(throwable, ExceptionMetadata(this)))
                    toast
                }

            ToastPresenter.show(translated, onTap)
        }

        /** Dismisses the currently visible toast, if any. */
        fun hide() {
            ToastPresenter.hide()
        }
    }
}
