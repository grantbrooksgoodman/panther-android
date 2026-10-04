//
//  LzfseTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.services.lzfse

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Verifies the LZFSE codec. */
class LzfseTest {
    @Test
    fun `round-trips empty input`() {
        val source = ByteArray(0)
        assertArrayEquals(source, Lzfse.decode(Lzfse.encode(source)))
    }

    @Test
    fun `round-trips arbitrary bytes`() {
        val source = "The quick brown fox jumps over the lazy dog. 🦊".toByteArray(Charsets.UTF_8)
        assertArrayEquals(source, Lzfse.decode(Lzfse.encode(source)))
    }

    @Test
    fun `round-trips a large buffer`() {
        val source = ByteArray(100_000) { (it * 31 + 7).toByte() }
        assertArrayEquals(source, Lzfse.decode(Lzfse.encode(source)))
    }

    @Test
    fun `encode frames an uncompressed block and end-of-stream marker`() {
        val source = byteArrayOf(1, 2, 3)
        val encoded = Lzfse.encode(source)
        // magic (4) + n_raw_bytes (4) + raw (3) + end-of-stream (4).
        assertEquals(15, encoded.size)
        // 'bvx-' little-endian.
        assertArrayEquals(byteArrayOf(0x62, 0x76, 0x78, 0x2d), encoded.copyOfRange(0, 4))
        // n_raw_bytes = 3 little-endian.
        assertArrayEquals(byteArrayOf(3, 0, 0, 0), encoded.copyOfRange(4, 8))
        // 'bvx$' little-endian end-of-stream.
        assertArrayEquals(byteArrayOf(0x62, 0x76, 0x78, 0x24), encoded.copyOfRange(11, 15))
    }

    @Test
    fun `decode throws on an unknown block magic`() {
        assertThrows(LzfseException::class.java) {
            Lzfse.decode(byteArrayOf(0, 0, 0, 0))
        }
    }

    @Test
    fun `decode throws on a compressed block pending vector validation`() {
        // 'bvx2' little-endian – the LZFSE v2 compressed block magic.
        assertThrows(LzfseException::class.java) {
            Lzfse.decode(byteArrayOf(0x62, 0x76, 0x78, 0x32))
        }
    }
}
