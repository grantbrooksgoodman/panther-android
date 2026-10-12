//
//  AttributeDetectionServiceDelegate.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.interfaces

import java.util.Date

/**
 * The interface used to handle taps on attributes detected in text.
 *
 * [AttributeDetectionService][us.neotechnica.panther.modules.common.services.AttributeDetectionService]
 * detects dates, links, and phone numbers in a message's text and
 * notifies its delegate when the user taps one. Register your own
 * conformance with `registerDelegate` to customize how selections are
 * handled; otherwise,
 * [DefaultAttributeDetectionServiceDelegate][us.neotechnica.panther.modules.common.services.DefaultAttributeDetectionServiceDelegate]
 * is used.
 */
interface AttributeDetectionServiceDelegate {
    /**
     * Tells the delegate that the user selected a date in the given
     * text.
     *
     * @param date The date that was selected.
     * @param substring The portion of the text in which the date was
     *   detected.
     * @param fullText The complete text containing the detected date.
     */
    fun didSelectDate(
        date: Date,
        substring: String,
        fullText: String,
    )

    /**
     * Tells the delegate that the user selected a phone number.
     *
     * @param phoneNumber The phone number that was selected.
     */
    fun didSelectPhoneNumber(phoneNumber: String)

    /**
     * Tells the delegate that the user selected a URL in the given
     * text.
     *
     * @param url The URL that was selected.
     * @param substring The portion of the text in which the URL was
     *   detected.
     * @param fullText The complete text containing the detected URL.
     */
    fun didSelectURL(
        url: String,
        substring: String,
        fullText: String,
    )
}
