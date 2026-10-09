//
//  BuildInfoOverlayViewService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.views.buildinfooverlayview

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheet
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.designsystem.modules.alertkit.models.AttributedStringConfig
import us.neotechnica.panther.designsystem.modules.developermode.services.DevModeService
import us.neotechnica.panther.designsystem.modules.foundation.dependencies.reportDelegate
import us.neotechnica.panther.designsystem.modules.foundation.extensions.cancelAction
import us.neotechnica.panther.designsystem.modules.foundation.services.ReportDelegate
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.localization.models.Localized
import us.neotechnica.panther.subsystem.modules.localization.models.SubsystemStringKey
import us.neotechnica.panther.translator.Translator
import java.util.Calendar

internal class BuildInfoOverlayViewService {
    // MARK: - Dependencies

    private val reportDelegate: ReportDelegate by Dependency { it.reportDelegate }

    // MARK: - Properties

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Computed Properties

    private val buildInfoButtonMessage: String
        get() {
            val milestoneString = Build.milestone.rawValue

            var message =
                "This is a${if (milestoneString == "alpha") "n" else ""} $milestoneString version of " +
                    "⌘project code name ${Build.codeName}⌘."
            if (Build.appStoreReleaseVersion > 0) {
                message = "This is a pre-release update to ⌘${Build.finalName}⌘."
            }

            message +=
                "\n\nAll features presented here are subject to change, and any new or previously undisclosed " +
                "information presented within this software is to remain strictly confidential.\n\n" +
                "Redistribution of this software by unauthorized parties in any way, shape, or form is " +
                "strictly prohibited.\n\nBy continuing your use of this software, you acknowledge your " +
                "agreement to the above terms.\n\nAll content herein, unless otherwise stated, is copyright " +
                "⌘© ${Calendar.getInstance().get(Calendar.YEAR)} NEOTechnica Corporation⌘. All rights reserved."

            return message
        }

    // MARK: - Build Info Button Tapped

    fun buildInfoButtonTapped() {
        performMediumImpactHaptic()

        val viewBuildInformationAction =
            Action("View Build Information") {
                viewBuildInformationButtonTapped()
            }

        val developerModeButtonTitle = "${if (Build.isDeveloperModeEnabled) "Disable" else "Enable"} Developer Mode"
        val developerModeAction =
            Action(
                developerModeButtonTitle,
                style = if (developerModeButtonTitle.startsWith("Enable")) ActionStyle.DEFAULT else ActionStyle.DESTRUCTIVE,
            ) {
                DevModeService.promptToToggle()
            }

        val alert =
            Alert(
                title = if (RuntimeStorage.languageCode == "en") "Project ${Build.codeName}" else "Project ⌘${Build.codeName}⌘",
                message = buildInfoButtonMessage,
                actions =
                    listOf(
                        viewBuildInformationAction,
                        developerModeAction,
                        Action.cancelAction(title = Localized(SubsystemStringKey.DISMISS).wrappedValue),
                    ),
            )

        scope.launch {
            alert.present(
                translating =
                    listOf(
                        Alert.TranslationOptionKey.Message,
                        Alert.TranslationOptionKey.Title,
                    ),
            )
        }
    }

    // MARK: - Send Feedback Button Tapped

    fun sendFeedbackButtonTapped() {
        performMediumImpactHaptic()

        val reportBugAction =
            Action(Localized(SubsystemStringKey.REPORT_BUG).wrappedValue) {
                reportDelegate.reportBug()
            }

        val sendFeedbackAction =
            Action(Localized(SubsystemStringKey.SEND_FEEDBACK).wrappedValue) {
                reportDelegate.sendFeedback()
            }

        scope.launch {
            ActionSheet(
                title = "${if (RuntimeStorage.languageCode == "en") "File" else "Make"} a Report",
                actions =
                    listOf(
                        sendFeedbackAction,
                        reportBugAction,
                    ),
                cancelButtonTitle = Localized(SubsystemStringKey.CANCEL).wrappedValue,
            ).present(translating = listOf(ActionSheet.TranslationOptionKey.Title))
        }
    }

    // MARK: - Auxiliary

    private fun performMediumImpactHaptic() {
        if (VERSION.SDK_INT < VERSION_CODES.Q) return

        runCatching {
            val context = Translator.config.currentActivityProvider?.invoke() ?: return
            val vibrator =
                if (VERSION.SDK_INT >= VERSION_CODES.S) {
                    context
                        .getSystemService(VibratorManager::class.java)
                        .defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Vibrator::class.java)
                }

            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        }
    }

    private fun viewBuildInformationButtonTapped() {
        val buildMilestoneString = "Build Milestone\n${Build.milestone.rawValue.capitalizedWords()}"
        val bundleVersionString = "Bundle Version\n${Build.bundleVersion} (${Build.buildNumber})"
        val projectIDString = "Project ID\n${Build.projectID}"
        val revisionString = "Revision\n${Build.bundleRevision} (${Build.revisionBuildNumber})"
        val skuString = "SKU\n${Build.buildSKU}"

        val message =
            listOf(
                buildMilestoneString,
                bundleVersionString,
                projectIDString,
                revisionString,
                skuString,
            ).joinToString("\n\n")

        val alert = Alert(message = message)
        alert.setMessageAttributes(
            AttributedStringConfig(
                SpanStyle(fontSize = MESSAGE_FONT_SIZE.sp),
                secondaryAttributes =
                    listOf(
                        AttributedStringConfig.StringAttributes(
                            SpanStyle(
                                fontSize = MESSAGE_LABEL_FONT_SIZE.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            stringRanges =
                                listOf(
                                    "Build Number",
                                    "Build Milestone",
                                    "Bundle Version",
                                    "Project ID",
                                    "Revision",
                                    "SKU",
                                ),
                        ),
                    ),
            ),
        )

        scope.launch { alert.present(translating = emptyList()) }
    }

    private fun String.capitalizedWords(): String =
        split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
}

private const val MESSAGE_FONT_SIZE = 13
private const val MESSAGE_LABEL_FONT_SIZE = 14
