//
//  LzfseDecoder.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 30/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//
//  Ported from Apple's BSD-licensed LZFSE reference implementation
//  (`lzfse_decode_base.c`, `lzvn_decode_base.c`). See `Lzfse.kt` for the
//  full license notice.

package us.neotechnica.panther.subsystem.modules.foundation.services.lzfse

import java.io.ByteArrayOutputStream

/** Decodes an LZFSE stream, dispatching each block to its block decoder. */
// MagicNumber suppressed: the literals are the LZVN opcode values and bit-field
// widths defined by Apple's wire format (`lzvn_decode_base.c`).
@Suppress("MagicNumber")
internal object LzfseDecoder {
    // MARK: - Types

    private enum class LzvnOpcode { SML_D, MED_D, LRG_D, PRE_D, SML_M, LRG_M, SML_L, LRG_L, NOP, EOS, UDEF }

    // MARK: - Properties

    private val lzvnOpcodeTable: Array<LzvnOpcode> by lazy { buildLzvnOpcodeTable() }

    // MARK: - Decode

    fun decode(source: ByteArray): ByteArray {
        val output = ByteArrayOutputStream(source.size)
        var offset = 0

        while (offset + UINT32_BYTES <= source.size) {
            val magic = readUInt32LE(source, offset)
            offset += UINT32_BYTES

            offset =
                when (magic) {
                    LzfseBlockMagic.ENDOFSTREAM -> return output.toByteArray()
                    LzfseBlockMagic.UNCOMPRESSED -> decodeUncompressedBlock(source, offset, output)
                    LzfseBlockMagic.COMPRESSED_LZVN -> decodeLzvnBlock(source, offset, output)

                    LzfseBlockMagic.COMPRESSED_V1,
                    LzfseBlockMagic.COMPRESSED_V2,
                    -> LzfseCompressedBlockDecoder.decode(magic, source, offset, output)

                    else -> throw LzfseException("Unknown LZFSE block magic: 0x${magic.toUInt().toString(RADIX_HEX)}.")
                }
        }

        throw LzfseException("Missing LZFSE end-of-stream marker.")
    }

    // MARK: - Block Decoders

    private fun decodeUncompressedBlock(
        source: ByteArray,
        offset: Int,
        output: ByteArrayOutputStream,
    ): Int {
        if (offset + UINT32_BYTES > source.size) throw LzfseException("Truncated LZFSE uncompressed block header.")
        val rawByteCount = readUInt32LE(source, offset)
        val payloadOffset = offset + UINT32_BYTES
        if (rawByteCount < 0 || payloadOffset + rawByteCount > source.size) {
            throw LzfseException("Truncated LZFSE uncompressed block.")
        }
        output.write(source, payloadOffset, rawByteCount)
        return payloadOffset + rawByteCount
    }

    private fun decodeLzvnBlock(
        source: ByteArray,
        offset: Int,
        output: ByteArrayOutputStream,
    ): Int {
        if (offset + 2 * UINT32_BYTES > source.size) throw LzfseException("Truncated LZVN block header.")
        val rawByteCount = readUInt32LE(source, offset)
        val payloadByteCount = readUInt32LE(source, offset + UINT32_BYTES)
        val payloadOffset = offset + 2 * UINT32_BYTES
        if (rawByteCount < 0 || payloadByteCount < 0 || payloadOffset + payloadByteCount > source.size) {
            throw LzfseException("Truncated LZVN block.")
        }

        output.write(decodeLzvnPayload(source, payloadOffset, payloadByteCount, rawByteCount))
        return payloadOffset + payloadByteCount
    }

    // MARK: - LZVN

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun decodeLzvnPayload(
        source: ByteArray,
        payloadOffset: Int,
        payloadByteCount: Int,
        rawByteCount: Int,
    ): ByteArray {
        val dst = ByteArray(rawByteCount)
        var dstPos = 0
        var src = payloadOffset
        val end = payloadOffset + payloadByteCount
        var distance = 0

        fun byte(index: Int): Int {
            if (index >= end) throw LzfseException("Truncated LZVN payload.")
            return source[index].toInt() and BYTE_MASK
        }

        while (src < end && dstPos < rawByteCount) {
            val opcode = byte(src)
            var literalLength = 0
            var matchLength = 0

            when (lzvnOpcodeTable[opcode]) {
                LzvnOpcode.EOS -> return dst
                LzvnOpcode.NOP -> { src += 1; continue }
                LzvnOpcode.UDEF -> throw LzfseException("Invalid LZVN opcode: $opcode.")

                LzvnOpcode.SML_D -> {
                    literalLength = extract(opcode, LITERAL_LENGTH_SHIFT, 2)
                    matchLength = extract(opcode, MATCH_LENGTH_SHIFT, 3) + MATCH_LENGTH_BIAS
                    distance = (extract(opcode, 0, 3) shl BITS_PER_BYTE) or byte(src + 1)
                    src += 2
                }
                LzvnOpcode.MED_D -> {
                    literalLength = extract(opcode, MATCH_LENGTH_SHIFT, 2)
                    val opc23 = byte(src + 1) or (byte(src + 2) shl BITS_PER_BYTE)
                    matchLength = ((extract(opcode, 0, 3) shl 2) or extract(opc23, 0, 2)) + MATCH_LENGTH_BIAS
                    distance = extract(opc23, 2, 14)
                    src += 3
                }
                LzvnOpcode.LRG_D -> {
                    literalLength = extract(opcode, LITERAL_LENGTH_SHIFT, 2)
                    matchLength = extract(opcode, MATCH_LENGTH_SHIFT, 3) + MATCH_LENGTH_BIAS
                    distance = byte(src + 1) or (byte(src + 2) shl BITS_PER_BYTE)
                    src += 3
                }
                LzvnOpcode.PRE_D -> {
                    literalLength = extract(opcode, LITERAL_LENGTH_SHIFT, 2)
                    matchLength = extract(opcode, MATCH_LENGTH_SHIFT, 3) + MATCH_LENGTH_BIAS
                    src += 1
                }
                LzvnOpcode.SML_M -> { matchLength = extract(opcode, 0, 4); src += 1 }
                LzvnOpcode.LRG_M -> { matchLength = byte(src + 1) + LARGE_BIAS; src += 2 }
                LzvnOpcode.SML_L -> { literalLength = extract(opcode, 0, 4); src += 1 }
                LzvnOpcode.LRG_L -> { literalLength = byte(src + 1) + LARGE_BIAS; src += 2 }
            }

            repeat(literalLength) {
                if (src >= end || dstPos >= rawByteCount) throw LzfseException("Truncated LZVN literal.")
                dst[dstPos++] = source[src++]
            }
            if (matchLength > 0) {
                if (distance <= 0 || distance > dstPos) throw LzfseException("Invalid LZVN match distance: $distance.")
                repeat(matchLength) {
                    if (dstPos >= rawByteCount) throw LzfseException("LZVN match overruns output.")
                    dst[dstPos] = dst[dstPos - distance]
                    dstPos++
                }
            }
        }

        return dst
    }

    private fun buildLzvnOpcodeTable(): Array<LzvnOpcode> {
        val table = Array(OPCODE_COUNT) { LzvnOpcode.UDEF }
        for (base in intArrayOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 72, 80, 88, 96, 104, 128, 136, 144, 152, 192, 200)) {
            for (index in 0..5) table[base + index] = LzvnOpcode.SML_D
        }
        for (value in 160..191) table[value] = LzvnOpcode.MED_D
        for (value in intArrayOf(7, 15, 23, 31, 39, 47, 55, 63, 71, 79, 87, 95, 103, 111, 135, 143, 151, 159, 199, 207)) {
            table[value] = LzvnOpcode.LRG_D
        }
        for (value in intArrayOf(70, 78, 86, 94, 102, 110, 134, 142, 150, 158, 198, 206)) table[value] = LzvnOpcode.PRE_D
        for (value in 241..255) table[value] = LzvnOpcode.SML_M
        table[240] = LzvnOpcode.LRG_M
        for (value in 225..239) table[value] = LzvnOpcode.SML_L
        table[224] = LzvnOpcode.LRG_L
        table[14] = LzvnOpcode.NOP
        table[22] = LzvnOpcode.NOP
        table[6] = LzvnOpcode.EOS
        return table
    }

    // MARK: - Auxiliary

    private fun extract(
        container: Int,
        lsb: Int,
        width: Int,
    ): Int = (container ushr lsb) and ((1 shl width) - 1)

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

    // MARK: - Companion

    private const val BITS_PER_BYTE = 8
    private const val BYTE_MASK = 0xFF
    private const val LARGE_BIAS = 16
    private const val LITERAL_LENGTH_SHIFT = 6
    private const val MATCH_LENGTH_BIAS = 3
    private const val MATCH_LENGTH_SHIFT = 3
    private const val OPCODE_COUNT = 256
    private const val RADIX_HEX = 16
    private const val UINT32_BYTES = 4
}
