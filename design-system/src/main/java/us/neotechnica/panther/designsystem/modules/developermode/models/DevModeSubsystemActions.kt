//
//  DevModeSubsystemActions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.developermode.models

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.alertkit.AlertKitConfig
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheet
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextFieldAttributes
import us.neotechnica.panther.designsystem.modules.alertkit.models.TextInputAlert
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.models.overriddenLanguageCode
import us.neotechnica.panther.subsystem.modules.foundation.services.BuildInfoOverlay
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
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
                    val eraseDocumentsDirectoryAction =
                        Action("Erase Documents Directory") {
                            CoreUtilities.eraseDocumentsDirectory()
                            HUD.showSuccess(text = "Erased Documents Directory")
                        }
                    val resetPreferencesAction =
                        Action("Reset Preferences") {
                            Persistent.reset()
                            HUD.showSuccess(text = "Reset Preferences")
                        }
                    val eraseAllAction =
                        Action("Erase All Content & Settings", style = ActionStyle.DESTRUCTIVE_PREFERRED) {
                            CoreUtilities.clearCaches()
                            CoreUtilities.eraseDocumentsDirectory()
                            Persistent.reset()
                            HUD.showSuccess()
                        }

                    ActionSheet(
                        title = "Erase Content & Settings",
                        actions =
                            listOf(
                                clearCachesAction,
                                eraseDocumentsDirectoryAction,
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

    // MARK: - Auxiliary

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
                val languageCode = Locale.getISOLanguages().random()
                setLanguageCode(languageCode)
                HUD.showSuccess(text = "Set to ${languageName(languageCode)}")
            }

        ActionSheet(
            title = "Override Language Code",
            actions = listOf(specifyLanguageCodeAction, setToRandomLanguageCodeAction),
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

        val enteredInput = input?.trim()?.lowercase() ?: return
        if (enteredInput !in Locale.getISOLanguages()) {
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

        setLanguageCode(enteredInput)
        HUD.showSuccess()
    }

    private fun setLanguageCode(languageCode: String) {
        AlertKitConfig.overrideTargetLanguageCode(languageCode)
        CoreUtilities.setLanguageCode(languageCode, override = true)
    }

    private fun restoreLanguageCode(showSuccess: Boolean) {
        RuntimeStorage.remove(StoredItemKey.overriddenLanguageCode)
        CoreUtilities.restoreDeviceLanguageCode()
        AlertKitConfig.overrideTargetLanguageCode(Locale.getDefault().language)
        if (showSuccess) HUD.showSuccess()
    }

    private fun languageName(languageCode: String): String =
        Locale(languageCode).getDisplayLanguage(Locale.ENGLISH).ifEmpty { languageCode.uppercase() }
}
