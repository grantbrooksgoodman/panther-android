//
//  Lzfse.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 30/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

// The LZFSE stream format (block magics and layout) is defined by Apple's
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
 * compression ratio. [decode] reads uncompressed and end-of-stream
 * blocks; decoding the FSE- and LZVN-compressed block types is pending
 * cross-platform test-vector validation and throws
 * [LzfseException] until then.
 */
object Lzfse {
    // MARK: - Block Magics

    private const val ENDOFSTREAM_BLOCK_MAGIC = 0x24787662 // 'bvx$'
    private const val UNCOMPRESSED_BLOCK_MAGIC = 0x2d787662 // 'bvx-'
    private const val COMPRESSED_V1_BLOCK_MAGIC = 0x31787662 // 'bvx1'
    private const val COMPRESSED_V2_BLOCK_MAGIC = 0x32787662 // 'bvx2'
    private const val COMPRESSED_LZVN_BLOCK_MAGIC = 0x6e787662 // 'bvxn'

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
        writeUInt32LE(output, UNCOMPRESSED_BLOCK_MAGIC)
        writeUInt32LE(output, source.size)
        output.write(source)
        writeUInt32LE(output, ENDOFSTREAM_BLOCK_MAGIC)
        return output.toByteArray()
    }

    /**
     * Returns the bytes decoded from the LZFSE stream [source].
     *
     * @param source The LZFSE-framed bytes to decode.
     *
     * @return The decoded bytes.
     *
     * @throws LzfseException if the stream is malformed, truncated, or
     *   contains a compressed block whose decoding is not yet supported.
     */
    fun decode(source: ByteArray): ByteArray {
        val output = ByteArrayOutputStream(source.size)
        var offset = 0

        while (offset + UINT32_BYTES <= source.size) {
            val magic = readUInt32LE(source, offset)
            offset += UINT32_BYTES

            when (magic) {
                ENDOFSTREAM_BLOCK_MAGIC -> return output.toByteArray()

                UNCOMPRESSED_BLOCK_MAGIC -> {
                    if (offset + UINT32_BYTES > source.size) {
                        throw LzfseException("Truncated LZFSE uncompressed block header.")
                    }
                    val rawByteCount = readUInt32LE(source, offset)
                    offset += UINT32_BYTES
                    if (rawByteCount < 0 || offset + rawByteCount > source.size) {
                        throw LzfseException("Truncated LZFSE uncompressed block.")
                    }
                    output.write(source, offset, rawByteCount)
                    offset += rawByteCount
                }

                COMPRESSED_V1_BLOCK_MAGIC,
                COMPRESSED_V2_BLOCK_MAGIC,
                COMPRESSED_LZVN_BLOCK_MAGIC,
                ->
                    throw LzfseException("LZFSE compressed-block decoding is pending cross-platform vector validation.")

                else -> throw LzfseException("Unknown LZFSE block magic: 0x${magic.toUInt().toString(RADIX_HEX)}.")
            }
        }

        throw LzfseException("Missing LZFSE end-of-stream marker.")
    }

    // MARK: - Auxiliary

    private fun readUInt32LE(
        bytes: ByteArray,
        offset: Int,
    ): Int {
        var result = 0
        for (index in 0 until UINT32_BYTES) {
            result = result or ((bytes[offset + index].toInt() and BYTE_MASK) shl (BITS_PER_BYTE * index))
        }
        return result
    }

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
    private const val RADIX_HEX = 16
    private const val UINT32_BYTES = 4
}

// MARK: - LzfseException

/** An error thrown while decoding an LZFSE stream. */
class LzfseException(
    message: String,
) : Exception(message)
