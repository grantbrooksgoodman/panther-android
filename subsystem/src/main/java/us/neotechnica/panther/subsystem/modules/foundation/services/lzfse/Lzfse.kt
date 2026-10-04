//
//  Lzfse.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

// The LZFSE stream format and decoder algorithm are defined by Apple's
// LZFSE reference implementation, released under the following license:
//
// Copyright (c) 2015-2016, Apple Inc. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holder(s) nor the names of any
//    contributors may be used to endorse or promote products derived from this
//    software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES ARE DISCLAIMED.

package us.neotechnica.panther.subsystem.modules.foundation.services.lzfse

import java.io.ByteArrayOutputStream

/**
 * Encodes and decodes LZFSE-framed byte streams.
 *
 * Use [Lzfse] to compress and decompress data in the LZFSE container
 * format, interoperating with Apple's `compression_encode_buffer` /
 * `compression_decode_buffer` (`.lzfse`).
 *
 * **Important:** [encode] emits a single uncompressed block – a valid
 * LZFSE stream that Apple's decoder reads back verbatim, but without a
 * compression ratio. [decode] reads uncompressed, end-of-stream, LZVN,
 * and FSE-compressed (v2) blocks; legacy v1 compressed blocks throw.
 */
object Lzfse {
    // MARK: - Methods

    /**
     * Returns [source] encoded as an LZFSE stream.
     *
     * The stream is a single uncompressed block followed by an
     * end-of-stream marker, which Apple's LZFSE decoder reads back
     * verbatim.
     *
     * @param source The bytes to encode.
     *
     * @return The LZFSE-framed bytes.
     */
    fun encode(source: ByteArray): ByteArray {
        val output = ByteArrayOutputStream(source.size + BLOCK_OVERHEAD_BYTES)
        writeUInt32LE(output, LzfseBlockMagic.UNCOMPRESSED)
        writeUInt32LE(output, source.size)
        output.write(source)
        writeUInt32LE(output, LzfseBlockMagic.ENDOFSTREAM)
        return output.toByteArray()
    }

    /**
     * Returns the bytes decoded from the LZFSE stream [source].
     *
     * @param source The LZFSE-framed bytes to decode.
     *
     * @return The decoded bytes.
     *
     * @throws LzfseException if the stream is malformed or truncated,
     *   or contains a legacy v1 compressed block.
     */
    fun decode(source: ByteArray): ByteArray = LzfseDecoder.decode(source)

    // MARK: - Auxiliary

    private fun writeUInt32LE(
        output: ByteArrayOutputStream,
        value: Int,
    ) {
        for (index in 0 until UINT32_BYTES) {
            output.write((value ushr (BITS_PER_BYTE * index)) and BYTE_MASK)
        }
    }

    // MARK: - Companion

    private const val BITS_PER_BYTE = 8
    private const val BLOCK_OVERHEAD_BYTES = 12
    private const val BYTE_MASK = 0xFF
    private const val UINT32_BYTES = 4
}

// MARK: - LzfseBlockMagic

/** The four-byte magics that tag each LZFSE block. */
internal object LzfseBlockMagic {
    const val ENDOFSTREAM = 0x24787662 // 'bvx$'
    const val UNCOMPRESSED = 0x2d787662 // 'bvx-'
    const val COMPRESSED_V1 = 0x31787662 // 'bvx1'
    const val COMPRESSED_V2 = 0x32787662 // 'bvx2'
    const val COMPRESSED_LZVN = 0x6e787662 // 'bvxn'
}

// MARK: - LzfseException

/** An error thrown while decoding an LZFSE stream. */
class LzfseException(
    message: String,
) : Exception(message)
