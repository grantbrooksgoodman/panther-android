//
//  DefaultAttributeDetectionServiceDelegate.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheet
import us.neotechnica.panther.modules.common.interfaces.AttributeDetectionServiceDelegate
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import java.util.Date

/**
 * The delegate used by [AttributeDetectionService] when no custom
 * delegate is registered.
 *
 * The default delegate presents a confirmation action sheet for date
 * and URL selections – opening the calendar app or the system browser
 * if the user confirms – and opens phone number selections immediately
 * in the dialer.
 */
object DefaultAttributeDetectionServiceDelegate : AttributeDetectionServiceDelegate {
    // MARK: - Properties

    private var appContext: Context? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Init

    /** Prepares the delegate with the application context. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    // MARK: - Did Select Date

    /** Presents an action sheet offering to show the selected date in the calendar app. */
    override fun didSelectDate(
        date: Date,
        substring: String,
        fullText: String,
    ) {
        val uri = Uri.parse("$CALENDAR_TIME_URI_PREFIX${date.time}")
        val nonEnglishTitle = "Show in the calendar"
        confirmSelection(
            if (RuntimeStorage.languageCode == "en") "Show in Calendar" else nonEnglishTitle,
            message = substring,
            uri = uri,
        )
    }

    // MARK: - Did Select Phone Number

    /** Opens the selected phone number in the dialer. */
    override fun didSelectPhoneNumber(phoneNumber: String) {
        val digits = phoneNumber.filter { it.isDigit() || it == '+' }
        openURI(Uri.parse("tel:$digits"), Intent.ACTION_DIAL)
    }

    // MARK: - Did Select URL

    /** Presents an action sheet offering to open the selected URL in the system browser. */
    override fun didSelectURL(
        url: String,
        substring: String,
        fullText: String,
    ) {
        confirmSelection(
            "Open in browser",
            message = substring,
            uri = Uri.parse(url),
        )
    }

    // MARK: - Auxiliary

    private fun confirmSelection(
        actionTitle: String,
        message: String,
        uri: Uri,
    ) {
        scope.launch {
            ActionSheet(
                message = message,
                actions = listOf(Action(actionTitle) { openURI(uri, Intent.ACTION_VIEW) }),
                cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
            ).present(translating = listOf(ActionSheet.TranslationOptionKey.Actions()))
        }
    }

    private fun openURI(
        uri: Uri,
        action: String,
    ) {
        val context = appContext ?: return
        val intent = Intent(action, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        runCatching { context.startActivity(intent) }
    }

    private const val CALENDAR_TIME_URI_PREFIX = "content://com.android.calendar/time/"
}
