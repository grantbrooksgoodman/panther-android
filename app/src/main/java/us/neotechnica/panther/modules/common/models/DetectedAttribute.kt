//
//  DetectedAttribute.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.models

/**
 * An attribute detected in text – a date, link, or phone number –
 * together with the character range it occupies.
 */
sealed interface DetectedAttribute {
    /** The character range the attribute occupies in its text. */
    val range: IntRange

    /**
     * A detected date.
     *
     * @property date The parsed date.
     */
    data class Date(
        override val range: IntRange,
        val date: java.util.Date,
    ) : DetectedAttribute

    /**
     * A detected phone number.
     *
     * @property phoneNumber The phone number as written.
     */
    data class PhoneNumber(
        override val range: IntRange,
        val phoneNumber: String,
    ) : DetectedAttribute

    /**
     * A detected link.
     *
     * @property url The absolute URL string.
     */
    data class URL(
        override val range: IntRange,
        val url: String,
    ) : DetectedAttribute
}
