//
//  Validatable.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.interfaces

/**
 * A type that can report whether it is valid for translation.
 *
 * Translation models adopt [Validatable] so that
 * [TranslationService][us.neotechnica.panther.translator.services.TranslationService]
 * can reject malformed inputs, language pairs, and results before
 * spending network work on them.
 */
interface Validatable {
    /** A Boolean value that indicates whether this value is well-formed. */
    val isWellFormed: Boolean
}
