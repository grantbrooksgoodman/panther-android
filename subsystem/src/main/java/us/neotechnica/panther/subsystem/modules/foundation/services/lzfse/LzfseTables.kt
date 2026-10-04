//
//  LzfseTables.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//
//  Ported from Apple's BSD-licensed LZFSE reference implementation
//  (`lzfse_internal.h`, `lzfse_decode_base.c`). See `Lzfse.kt` for the
//  license.

package us.neotechnica.panther.subsystem.modules.foundation.services.lzfse

/** The fixed value/frequency tables used by the FSE-compressed block decoder. */
internal object LzfseTables {
    // MARK: - Symbol and State Counts

    const val L_SYMBOLS = 20
    const val M_SYMBOLS = 20
    const val D_SYMBOLS = 64
    const val LITERAL_SYMBOLS = 256
    const val L_STATES = 64
    const val M_STATES = 64
    const val D_STATES = 256
    const val LITERAL_STATES = 1024

    // MARK: - L / M / D Extra Bits and Base Values

    val L_EXTRA_BITS = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 3, 5, 8)
    val L_BASE_VALUE = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 20, 28, 60)

    val M_EXTRA_BITS = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 5, 8, 11)
    val M_BASE_VALUE = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 24, 56, 312)

    val D_EXTRA_BITS =
        intArrayOf(
            0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3,
            4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7, 7,
            8, 8, 8, 8, 9, 9, 9, 9, 10, 10, 10, 10, 11, 11, 11, 11,
            12, 12, 12, 12, 13, 13, 13, 13, 14, 14, 14, 14, 15, 15, 15, 15,
        )
    val D_BASE_VALUE =
        intArrayOf(
            0, 1, 2, 3, 4, 6, 8, 10, 12, 16,
            20, 24, 28, 36, 44, 52, 60, 76, 92, 108,
            124, 156, 188, 220, 252, 316, 380, 444, 508, 636,
            764, 892, 1020, 1276, 1532, 1788, 2044, 2556, 3068, 3580,
            4092, 5116, 6140, 7164, 8188, 10236, 12284, 14332, 16380, 20476,
            24572, 28668, 32764, 40956, 49148, 57340, 65532, 81916, 98300, 114684,
            131068, 163836, 196604, 229372,
        )

    // MARK: - Frequency-Table Value Decoding

    val FREQ_NBITS_TABLE =
        intArrayOf(
            2, 3, 2, 5, 2, 3, 2, 8, 2, 3, 2, 5, 2, 3, 2, 14,
            2, 3, 2, 5, 2, 3, 2, 8, 2, 3, 2, 5, 2, 3, 2, 14,
        )
    val FREQ_VALUE_TABLE =
        intArrayOf(
            0, 2, 1, 4, 0, 3, 1, -1, 0, 2, 1, 5, 0, 3, 1, -1,
            0, 2, 1, 6, 0, 3, 1, -1, 0, 2, 1, 7, 0, 3, 1, -1,
        )
}
