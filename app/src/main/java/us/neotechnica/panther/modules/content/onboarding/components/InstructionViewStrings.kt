//
//  InstructionViewStrings.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.components

/**
 * The resolved title and subtitle strings for an
 * [InstructionView][us.neotechnica.panther.modules.content.onboarding.components.InstructionView].
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
