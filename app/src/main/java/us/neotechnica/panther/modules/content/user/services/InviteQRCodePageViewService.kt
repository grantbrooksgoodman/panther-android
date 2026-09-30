//
//  InviteQRCodePageViewService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 30/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import us.neotechnica.panther.modules.common.services.MetadataService
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

/** The service that generates the invite QR code page's content. */
object InviteQRCodePageViewService {
    // MARK: - Computed Properties

    /**
     * A QR code image encoding the app's share link, or `null` if the
     * link has not been resolved or generation fails.
     */
    val appShareQRCodeImage: Bitmap?
        get() {
            val appShareLink = MetadataService.appShareLink ?: return null
            return generateQRCode(from = appShareLink, outputSize = OUTPUT_SIZE)
        }

    // MARK: - Auxiliary

    private fun generateQRCode(
        from: String,
        outputSize: Int,
    ): Bitmap? {
        val hints =
            mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.Q,
                EncodeHintType.CHARACTER_SET to "ISO-8859-1",
                EncodeHintType.MARGIN to 0,
            )

        val matrix =
            try {
                QRCodeWriter().encode(from, BarcodeFormat.QR_CODE, outputSize, outputSize, hints)
            } catch (exception: WriterException) {
                Logger.log("Failed to generate QR code. ${exception.message}")
                return null
            }

        val pixels = IntArray(outputSize * outputSize)
        for (y in 0 until outputSize) {
            val offset = y * outputSize
            for (x in 0 until outputSize) {
                pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }

        val bitmap = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, outputSize, 0, 0, outputSize, outputSize)
        return bitmap
    }

    // MARK: - Companion

    private const val OUTPUT_SIZE = 500
}
