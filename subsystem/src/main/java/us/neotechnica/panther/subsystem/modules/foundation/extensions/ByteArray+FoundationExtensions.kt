//
//  ByteArray+FoundationExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.extensions

/**
 * Returns the little-endian, unsigned 32-bit integer that begins at
 * the given offset.
 *
 * @param offset The index of the integer's least significant byte.
 *
 * @return The integer's value, as a signed integer of the same bits.
 */
fun ByteArray.readUInt32LE(offset: Int): Int {
    var result = 0
    for (index in 0 until UINT32_BYTES) {
        result = result or ((this[offset + index].toInt() and BYTE_MASK) shl (BITS_PER_BYTE * index))
    }
    return result
}

private const val BITS_PER_BYTE = 8
private const val BYTE_MASK = 0xFF
private const val UINT32_BYTES = 4
