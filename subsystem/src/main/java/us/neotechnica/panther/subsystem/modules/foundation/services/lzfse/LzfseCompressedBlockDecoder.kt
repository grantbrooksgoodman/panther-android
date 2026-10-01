//
//  LzfseCompressedBlockDecoder.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 30/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//
//  Ported from Apple's BSD-licensed LZFSE reference implementation
//  (`lzfse_decode_base.c`, `lzfse_fse.h`, `lzfse_fse.c`). See `Lzfse.kt`
//  for the license.

package us.neotechnica.panther.subsystem.modules.foundation.services.lzfse

import java.io.ByteArrayOutputStream

/** Decodes an FSE-compressed LZFSE block (v2; v1 is legacy and unsupported). */
// MagicNumber suppressed: the literals are the FSE header bit-field offsets/widths
// and code lengths defined by Apple's wire format (`lzfse_decode_base.c`, `lzfse_fse.c`).
@Suppress("MagicNumber")
internal object LzfseCompressedBlockDecoder {
    // MARK: - Types

    private class V2Header(
        val nRawBytes: Int,
        val nLiterals: Int,
        val nLiteralPayloadBytes: Int,
        val literalBits: Int,
        val literalState: IntArray,
        val nMatches: Int,
        val nLmdPayloadBytes: Int,
        val lmdBits: Int,
        val lState: Int,
        val mState: Int,
        val dState: Int,
        val headerSize: Int,
        val lFreq: IntArray,
        val mFreq: IntArray,
        val dFreq: IntArray,
        val literalFreq: IntArray,
    )

    /** A backward-reading FSE bit stream, per the reference `fse_in_stream64`. */
    private class FseInStream(
        private val source: ByteArray,
    ) {
        private var accum = 0L
        private var accumBits = 0
        var position = 0
            private set

        fun init(
            bits: Int,
            bufferEnd: Int,
        ) {
            if (bits != 0) {
                position = bufferEnd - BYTES_PER_LONG
                accum = load(position, BYTES_PER_LONG)
                accumBits = bits + Long.SIZE_BITS
            } else {
                position = bufferEnd - (BYTES_PER_LONG - 1)
                accum = load(position, BYTES_PER_LONG - 1)
                accumBits = Long.SIZE_BITS - BITS_PER_BYTE
            }
        }

        fun flush() {
            val bits = (MAX_ACCUM_BITS - accumBits) and -BITS_PER_BYTE
            if (bits <= 0) return
            position -= bits / BITS_PER_BYTE
            accum = (accum shl bits) or maskLsb(load(position, BYTES_PER_LONG), bits)
            accumBits += bits
        }

        fun pull(count: Int): Int {
            accumBits -= count
            val result = accum ushr accumBits
            accum = maskLsb(accum, accumBits)
            return result.toInt()
        }

        private fun load(
            offset: Int,
            count: Int,
        ): Long {
            var result = 0L
            for (index in 0 until count) {
                result = result or ((source[offset + index].toLong() and BYTE_MASK.toLong()) shl (BITS_PER_BYTE * index))
            }
            return result
        }
    }

    /** An FSE value decoder for the L, M, or D stream. */
    private class ValueDecoder(
        nstates: Int,
        freq: IntArray,
        valueBitsBySymbol: IntArray,
        valueBaseBySymbol: IntArray,
    ) {
        private val totalBits = IntArray(nstates)
        private val valueBits = IntArray(nstates)
        private val delta = IntArray(nstates)
        private val valueBase = IntArray(nstates)
        var state = 0

        init {
            val leadingZeros = Integer.numberOfLeadingZeros(nstates)
            var index = 0
            for (symbol in freq.indices) {
                val frequency = freq[symbol]
                if (frequency == 0) continue
                val k = Integer.numberOfLeadingZeros(frequency) - leadingZeros
                val j0 = ((2 * nstates) shr k) - frequency
                for (j in 0 until frequency) {
                    if (j < j0) {
                        totalBits[index] = k + valueBitsBySymbol[symbol]
                        delta[index] = ((frequency + j) shl k) - nstates
                    } else {
                        totalBits[index] = (k - 1) + valueBitsBySymbol[symbol]
                        delta[index] = (j - j0) shl (k - 1)
                    }
                    valueBits[index] = valueBitsBySymbol[symbol]
                    valueBase[index] = valueBaseBySymbol[symbol]
                    index++
                }
            }
        }

        fun decode(stream: FseInStream): Int {
            val bits = stream.pull(totalBits[state])
            val extraBits = valueBits[state]
            val value = valueBase[state] + (bits and ((1 shl extraBits) - 1))
            state = delta[state] + (bits ushr extraBits)
            return value
        }
    }

    // MARK: - Decode

    fun decode(
        magic: Int,
        source: ByteArray,
        offset: Int,
        output: ByteArrayOutputStream,
    ): Int {
        if (magic == LzfseBlockMagic.COMPRESSED_V1) {
            throw LzfseException("LZFSE v1 (legacy uncompressed-table) blocks are unsupported.")
        }

        val blockStart = offset - UINT32_BYTES
        val header = parseV2Header(source, blockStart)
        val literalPayloadStart = blockStart + header.headerSize
        val lmdPayloadStart = literalPayloadStart + header.nLiteralPayloadBytes
        val nextBlock = lmdPayloadStart + header.nLmdPayloadBytes
        if (nextBlock > source.size) throw LzfseException("Truncated LZFSE compressed block.")

        val literals = decodeLiterals(source, header, lmdPayloadStart)
        output.write(decodeLmd(source, header, literals, lmdPayloadStart))
        return nextBlock
    }

    // MARK: - Header

    @Suppress("MagicNumber") // The field offsets and widths are the documented v2 header bit layout.
    private fun parseV2Header(
        source: ByteArray,
        blockStart: Int,
    ): V2Header {
        if (blockStart + FIXED_HEADER_BYTES > source.size) throw LzfseException("Truncated LZFSE v2 header.")
        val nRawBytes = readUInt32LE(source, blockStart + 4)
        val v0 = readUInt64LE(source, blockStart + 8)
        val v1 = readUInt64LE(source, blockStart + 16)
        val v2 = readUInt64LE(source, blockStart + 24)

        val headerSize = field(v2, 0, 32)
        if (blockStart + headerSize > source.size) throw LzfseException("Truncated LZFSE v2 header.")

        val freq = decodeFrequencyTables(source, blockStart + FIXED_HEADER_BYTES, blockStart + headerSize)
        return V2Header(
            nRawBytes = nRawBytes,
            nLiterals = field(v0, 0, 20),
            nLiteralPayloadBytes = field(v0, 20, 20),
            literalBits = field(v0, 60, 3) - FSE_BITS_BIAS,
            literalState = intArrayOf(field(v1, 0, 10), field(v1, 10, 10), field(v1, 20, 10), field(v1, 30, 10)),
            nMatches = field(v0, 40, 20),
            nLmdPayloadBytes = field(v1, 40, 20),
            lmdBits = field(v1, 60, 3) - FSE_BITS_BIAS,
            lState = field(v2, 32, 10),
            mState = field(v2, 42, 10),
            dState = field(v2, 52, 10),
            headerSize = headerSize,
            lFreq = freq.copyOfRange(0, LzfseTables.L_SYMBOLS),
            mFreq = freq.copyOfRange(LzfseTables.L_SYMBOLS, LzfseTables.L_SYMBOLS + LzfseTables.M_SYMBOLS),
            dFreq =
                freq.copyOfRange(
                    LzfseTables.L_SYMBOLS + LzfseTables.M_SYMBOLS,
                    LzfseTables.L_SYMBOLS + LzfseTables.M_SYMBOLS + LzfseTables.D_SYMBOLS,
                ),
            literalFreq = freq.copyOfRange(LzfseTables.L_SYMBOLS + LzfseTables.M_SYMBOLS + LzfseTables.D_SYMBOLS, TOTAL_SYMBOLS),
        )
    }

    private fun decodeFrequencyTables(
        source: ByteArray,
        start: Int,
        end: Int,
    ): IntArray {
        val freq = IntArray(TOTAL_SYMBOLS)
        if (end <= start) return freq

        var accum = 0
        var accumBits = 0
        var position = start
        for (index in freq.indices) {
            while (position < end && accumBits + BITS_PER_BYTE <= FREQ_ACCUM_BITS) {
                accum = accum or ((source[position].toInt() and BYTE_MASK) shl accumBits)
                accumBits += BITS_PER_BYTE
                position++
            }
            val bits = decodeFrequencyValue(accum)
            if (bits.second > accumBits) throw LzfseException("Malformed LZFSE frequency table.")
            freq[index] = bits.first
            accum = accum ushr bits.second
            accumBits -= bits.second
        }
        return freq
    }

    @Suppress("MagicNumber") // Constants are the reference `lzfse_decode_v1_freq_value` code lengths.
    private fun decodeFrequencyValue(bits: Int): Pair<Int, Int> {
        val index = bits and 31
        return when (val nbits = LzfseTables.FREQ_NBITS_TABLE[index]) {
            8 -> Pair(8 + ((bits ushr 4) and 0xf), 8)
            14 -> Pair(24 + ((bits ushr 4) and 0x3ff), 14)
            else -> Pair(LzfseTables.FREQ_VALUE_TABLE[index], nbits)
        }
    }

    // MARK: - Literal and LMD Decode

    private fun decodeLiterals(
        source: ByteArray,
        header: V2Header,
        lmdPayloadStart: Int,
    ): ByteArray {
        val literals = ByteArray(header.nLiterals)
        if (header.nLiterals == 0) return literals

        val literalDecoder = buildDecoderTable(LzfseTables.LITERAL_STATES, header.literalFreq)
        val stream = FseInStream(source)
        stream.init(header.literalBits, lmdPayloadStart)
        val states = header.literalState.copyOf()

        var index = 0
        while (index < header.nLiterals) {
            stream.flush()
            for (lane in 0 until LITERAL_LANES) {
                val entry = literalDecoder[states[lane]]
                states[lane] = (entry shr 16) + stream.pull(entry and BYTE_MASK)
                literals[index + lane] = ((entry shr 8) and BYTE_MASK).toByte()
            }
            index += LITERAL_LANES
        }
        return literals
    }

    private fun decodeLmd(
        source: ByteArray,
        header: V2Header,
        literals: ByteArray,
        lmdPayloadStart: Int,
    ): ByteArray {
        val dst = ByteArray(header.nRawBytes)
        val lDecoder = ValueDecoder(LzfseTables.L_STATES, header.lFreq, LzfseTables.L_EXTRA_BITS, LzfseTables.L_BASE_VALUE)
        val mDecoder = ValueDecoder(LzfseTables.M_STATES, header.mFreq, LzfseTables.M_EXTRA_BITS, LzfseTables.M_BASE_VALUE)
        val dDecoder = ValueDecoder(LzfseTables.D_STATES, header.dFreq, LzfseTables.D_EXTRA_BITS, LzfseTables.D_BASE_VALUE)
        lDecoder.state = header.lState
        mDecoder.state = header.mState
        dDecoder.state = header.dState

        val stream = FseInStream(source)
        stream.init(header.lmdBits, lmdPayloadStart + header.nLmdPayloadBytes)

        var dstPos = 0
        var litPos = 0
        var distance = 0
        repeat(header.nMatches) {
            stream.flush()
            val literalLength = lDecoder.decode(stream)
            val matchLength = mDecoder.decode(stream)
            val newDistance = dDecoder.decode(stream)
            if (newDistance != 0) distance = newDistance

            repeat(literalLength) { dst[dstPos++] = literals[litPos++] }
            if (matchLength > 0) {
                if (distance <= 0 || distance > dstPos) throw LzfseException("Invalid LZFSE match distance: $distance.")
                repeat(matchLength) {
                    dst[dstPos] = dst[dstPos - distance]
                    dstPos++
                }
            }
        }
        return dst
    }

    // MARK: - Tables

    private fun buildDecoderTable(
        nstates: Int,
        freq: IntArray,
    ): IntArray {
        val table = IntArray(nstates)
        val leadingZeros = Integer.numberOfLeadingZeros(nstates)
        var index = 0
        for (symbol in freq.indices) {
            val frequency = freq[symbol]
            if (frequency == 0) continue
            val k = Integer.numberOfLeadingZeros(frequency) - leadingZeros
            val j0 = ((2 * nstates) shr k) - frequency
            for (j in 0 until frequency) {
                val bitsToRead: Int
                val delta: Int
                if (j < j0) {
                    bitsToRead = k
                    delta = ((frequency + j) shl k) - nstates
                } else {
                    bitsToRead = k - 1
                    delta = (j - j0) shl (k - 1)
                }
                table[index++] = (bitsToRead and BYTE_MASK) or ((symbol and BYTE_MASK) shl 8) or ((delta and 0xFFFF) shl 16)
            }
        }
        return table
    }

    // MARK: - Auxiliary

    private fun field(
        value: Long,
        offset: Int,
        width: Int,
    ): Int = ((value ushr offset) and ((1L shl width) - 1)).toInt()

    private fun maskLsb(
        value: Long,
        bits: Int,
    ): Long = if (bits <= 0) 0L else value and ((1L shl bits) - 1)

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

    private fun readUInt64LE(
        bytes: ByteArray,
        offset: Int,
    ): Long {
        var result = 0L
        for (index in 0 until BYTES_PER_LONG) {
            result = result or ((bytes[offset + index].toLong() and BYTE_MASK.toLong()) shl (BITS_PER_BYTE * index))
        }
        return result
    }

    // MARK: - Companion

    private const val BITS_PER_BYTE = 8
    private const val BYTES_PER_LONG = 8
    private const val BYTE_MASK = 0xFF
    private const val FIXED_HEADER_BYTES = 32
    private const val FREQ_ACCUM_BITS = 32
    private const val FSE_BITS_BIAS = 7
    private const val LITERAL_LANES = 4
    private const val MAX_ACCUM_BITS = 63
    private const val TOTAL_SYMBOLS =
        LzfseTables.L_SYMBOLS + LzfseTables.M_SYMBOLS + LzfseTables.D_SYMBOLS + LzfseTables.LITERAL_SYMBOLS
    private const val UINT32_BYTES = 4
}
