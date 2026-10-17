//
//  DevModeSubsystemActions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.developermode.models

import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheet
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextFieldAttributes
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextInputAlert
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.designsystem.modules.foundation.services.MailComposer
import us.neotechnica.panther.subsystem.modules.foundation.extensions.languageName
import us.neotechnica.panther.subsystem.modules.foundation.extensions.lowercasedTrimmingWhitespaceAndNewlines
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.models.overriddenLanguageCode
import us.neotechnica.panther.subsystem.modules.foundation.services.BuildInfoOverlay
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.translator.Translator
import java.util.Locale

/**
 * The built-in diagnostic actions shown in the subsystem domain of the
 * Developer Mode menu.
 */
object DevModeSubsystemActions {
    // MARK: - Properties

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Computed Properties

    /** The subsystem actions available in the current build. */
    val available: List<DevModeAction>
        get() =
            listOf(
                eraseContentAndSettingsAction,
                toggleBuildInfoOverlayAction,
                overrideLanguageCodeAction,
                viewLoggerSessionRecordAction,
            )

    private val eraseContentAndSettingsAction: DevModeAction
        get() =
            DevModeAction(title = "Erase Content & Settings") {
                scope.launch {
                    val clearCachesAction =
                        Action("Clear Caches") {
                            CoreUtilities.clearCaches()
                            HUD.showSuccess(text = "Cleared Caches")
                        }
                    val eraseApplicationSupportDirectoryAction =
                        Action("Erase Application Support Directory") {
                            erasing { CoreUtilities.eraseApplicationSupportDirectory() }
                            HUD.showSuccess(text = "Erased Application Support Directory")
                        }
                    val eraseDocumentsDirectoryAction =
                        Action("Erase Documents Directory") {
                            erasing { CoreUtilities.eraseDocumentsDirectory() }
                            HUD.showSuccess(text = "Erased Documents Directory")
                        }
                    val eraseTemporaryDirectoryAction =
                        Action("Erase Temporary Directory") {
                            erasing { CoreUtilities.eraseTemporaryDirectory() }
                            HUD.showSuccess(text = "Erased Temporary Directory")
                        }
                    val resetPreferencesAction =
                        Action("Reset Preferences") {
                            Persistent.reset()
                            HUD.showSuccess(text = "Reset Preferences")
                        }
                    val eraseAllAction =
                        Action("Erase All Content & Settings", style = ActionStyle.DESTRUCTIVE_PREFERRED) {
                            CoreUtilities.clearCaches()
                            Persistent.reset()
                            erasing { CoreUtilities.eraseApplicationSupportDirectory() }
                            erasing { CoreUtilities.eraseDocumentsDirectory() }
                            erasing { CoreUtilities.eraseTemporaryDirectory() }
                            HUD.showSuccess()
                        }

                    ActionSheet(
                        title = "Erase Content & Settings",
                        actions =
                            listOf(
                                clearCachesAction,
                                eraseApplicationSupportDirectoryAction,
                                eraseDocumentsDirectoryAction,
                                eraseTemporaryDirectoryAction,
                                resetPreferencesAction,
                                eraseAllAction,
                            ),
                    ).present(translating = emptyList())
                }
            }

    private val overrideLanguageCodeAction: DevModeAction
        get() {
            val prefix = if (RuntimeStorage.retrieve(StoredItemKey.overriddenLanguageCode) == null) "Override" else "Restore"
            return DevModeAction(title = "$prefix Language Code") { scope.launch { overrideLanguageCode() } }
        }

    private val toggleBuildInfoOverlayAction: DevModeAction
        get() {
            val prefix = if (BuildInfoOverlay.isHidden.value) "Show" else "Hide"
            return DevModeAction(title = "$prefix Build Info Overlay") {
                if (BuildInfoOverlay.isHidden.value) BuildInfoOverlay.show() else BuildInfoOverlay.hide()
            }
        }

    private val viewLoggerSessionRecordAction: DevModeAction
        get() =
            DevModeAction(title = "View Logger Session Record") {
                try {
                    viewLoggerSessionRecord()
                } catch (throwable: Throwable) {
                    Logger.log(
                        throwable as? Exception ?: Exception.from(throwable, ExceptionMetadata(this)),
                        with = Logger.AlertType.toast,
                    )
                }
            }

    // MARK: - Auxiliary

    // Runs a directory erasure, surfacing a failure as a toast.
    private fun erasing(erase: () -> Unit) {
        try {
            erase()
        } catch (exception: Exception) {
            Logger.log(exception, with = Logger.AlertType.toast)
        }
    }

    private suspend fun overrideLanguageCode() {
        if (RuntimeStorage.retrieve(StoredItemKey.overriddenLanguageCode) != null) {
            Alert(
                title = "Restore Language Code",
                message = "The language code will be unlocked and restored to the device's default.",
                actions =
                    listOf(
                        Action("Confirm", style = ActionStyle.PREFERRED) { restoreLanguageCode(showSuccess = true) },
                        Action("Override Again") {
                            restoreLanguageCode(showSuccess = false)
                            scope.launch { overrideLanguageCode() }
                        },
                        Action("Cancel", style = ActionStyle.CANCEL) {},
                    ),
            ).present(translating = emptyList())
            return
        }

        val specifyLanguageCodeAction = Action("Specify Language Code") { scope.launch { presentLanguageCodeTextInputAlert() } }
        val setToRandomLanguageCodeAction =
            Action("Set to Random Language Code") {
                val languageCode = RuntimeStorage.languageCodeDictionary?.keys?.randomOrNull() ?: return@Action
                setLanguageCode(languageCode)
                val languageName = languageCode.englishLanguageName ?: languageCode.languageName ?: languageCode.uppercase()
                HUD.showSuccess(text = "Set to $languageName")
            }

        ActionSheet(
            title = "Override Language Code",
            actions =
                listOf(
                    specifyLanguageCodeAction,
                    setToRandomLanguageCodeAction,
                    Action("Cancel", style = ActionStyle.CANCEL) {},
                ),
        ).present(translating = emptyList())
    }

    private suspend fun presentLanguageCodeTextInputAlert() {
        val input =
            TextInputAlert(
                title = "Override Language Code",
                message = "Enter the two-letter code of the language to apply:",
                attributes =
                    TextFieldAttributes(
                        capitalizationType = androidx.compose.ui.text.input.KeyboardCapitalization.None,
                        correctionType = TextFieldAttributes.CorrectionType.NO,
                        placeholderText = "en",
                    ),
            ).present(translating = emptyList())

        val enteredInput = input ?: return
        val languageCodes = RuntimeStorage.languageCodeDictionary
        if (languageCodes == null || enteredInput.lowercasedTrimmingWhitespaceAndNewlines !in languageCodes.keys) {
            Alert(
                title = "Override Language Code",
                message = "The language code entered was invalid. Please try again.",
                actions =
                    listOf(
                        Action("Try Again", style = ActionStyle.PREFERRED) { scope.launch { presentLanguageCodeTextInputAlert() } },
                        Action("Cancel", style = ActionStyle.CANCEL) {},
                    ),
            ).present(translating = emptyList())
            return
        }

        setLanguageCode(enteredInput.lowercasedTrimmingWhitespaceAndNewlines)
        HUD.showSuccess()
    }

    // Opens the current launch's session record in a text viewer.
    private fun viewLoggerSessionRecord() {
        val activity =
            Translator.config.currentActivityProvider?.invoke()
                ?: throw Exception("No current activity.", metadata = ExceptionMetadata(this))
        val file =
            Logger.sessionRecordFilePath?.takeIf { it.exists() }
                ?: throw Exception("The session record file does not exist.", metadata = ExceptionMetadata(this))
        val uri =
            MailComposer.attachmentURI(
                activity,
                MailComposer.AttachmentData(file.readBytes(), file.name, SESSION_RECORD_MIME_TYPE),
            ) ?: throw Exception("Failed to share the session record file.", metadata = ExceptionMetadata(this))

        activity.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, SESSION_RECORD_MIME_TYPE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
    }

    private fun setLanguageCode(languageCode: String) {
        CoreUtilities.setLanguageCode(languageCode, override = true)
    }

    private fun restoreLanguageCode(showSuccess: Boolean) {
        RuntimeStorage.remove(StoredItemKey.overriddenLanguageCode)
        CoreUtilities.restoreDeviceLanguageCode()
        if (showSuccess) HUD.showSuccess()
    }

    // The language's name in English, or `null` when the platform has
    // no name for the code.
    private val String.englishLanguageName: String?
        get() = Locale(this).getDisplayLanguage(Locale.ENGLISH).takeIf { it.isNotBlank() && it != this }
}

private const val SESSION_RECORD_MIME_TYPE = "text/plain"
