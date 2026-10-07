//
//  ByteArray+UserContentExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Decodes the byte array into an image bitmap.
 *
 * @return The decoded image, or `null` if the bytes are empty or
 *   not a decodable image.
 */
fun ByteArray.decodedImageBitmap(): ImageBitmap? {
    if (isEmpty()) return null
    return runCatching { BitmapFactory.decodeByteArray(this, 0, size)?.asImageBitmap() }.getOrNull()
}
