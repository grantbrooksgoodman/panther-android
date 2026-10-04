//
//  InstructionViewStrings.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.components

/**
 * The resolved title and subtitle strings for an [InstructionView].
 */
data class InstructionViewStrings(
    /** The bold title line. */
    val titleLabelText: String,
    /** The subtitle line. */
    val subtitleLabelText: String,
) {
    companion object {
        /** Empty strings, shown before resolution completes. */
        val empty = InstructionViewStrings(titleLabelText = "", subtitleLabelText = "")
    }
}
