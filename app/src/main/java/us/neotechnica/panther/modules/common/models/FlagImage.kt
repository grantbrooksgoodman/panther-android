//
//  FlagImage.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.models

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.DrawableRes

/**
 * A flag image bundled with the app.
 *
 * Flag images exist for region codes and for language codes. Use
 * [named] to look up the image for a code, and pass
 * [resourceID] to an image component to display it.
 */
@JvmInline
value class FlagImage(
    /** The drawable resource identifier of the image. */
    @param:DrawableRes val resourceID: Int,
) {
    // MARK: - Companion

    companion object {
        /**
         * Returns the flag image for the given region or language
         * code.
         *
         * @param code The region or language code whose flag to
         *   look up, in any letter case.
         * @param context A context used to resolve the image.
         *
         * @return The flag image; otherwise, `null` if no image
         *   exists for the code.
         */
        @SuppressLint("DiscouragedApi")
        fun named(
            code: String,
            context: Context,
        ): FlagImage? {
            if (code.isBlank()) return null
            val resourceName = "$RESOURCE_NAME_PREFIX${code.lowercase().replace("-", "_")}"
            val resourceID = context.resources.getIdentifier(resourceName, RESOURCE_TYPE, context.packageName)
            return if (resourceID == 0) null else FlagImage(resourceID)
        }

        private const val RESOURCE_NAME_PREFIX = "flag_"
        private const val RESOURCE_TYPE = "drawable"
    }
}
