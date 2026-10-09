//
//  SubsystemStringKey.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.localization.models

import us.neotechnica.panther.subsystem.modules.localization.interfaces.LocalizedStringKeyRepresentable

/**
 * The subsystem's localization key type.
 *
 * Each entry corresponds to a top-level key in the subsystem's
 * localized strings, exposing its snake-case [referent] – the key
 * used to look the value up. Resolve a value with
 * [Localized][us.neotechnica.panther.subsystem.modules.localization.models.Localized].
 */
enum class SubsystemStringKey(
    override val referent: String,
) : LocalizedStringKeyRepresentable {
    CANCEL("cancel"),
    DISMISS("dismiss"),
    DONE("done"),
    ERROR_REPORTED("error_reported"),
    INTERNET_CONNECTION_OFFLINE("internet_connection_offline"),
    NO_EMAIL("no_email"),
    NO_INTERNET_MESSAGE("no_internet_message"),
    REPORT_BUG("report_bug"),
    REPORT_SENT("report_sent"),
    SEND_FEEDBACK("send_feedback"),
    SETTINGS("settings"),
    SOMETHING_WENT_WRONG("something_went_wrong"),
    TAP_TO_REPORT("tap_to_report"),
    TIMED_OUT("timed_out"),
    TRY_AGAIN("try_again"),
    YESTERDAY("yesterday"),
}
