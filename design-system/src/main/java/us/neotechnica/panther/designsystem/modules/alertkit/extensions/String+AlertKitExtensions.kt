//
//  String+AlertKitExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 02/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.extensions

/**
 * The string with AlertKit's emphasis sentinels (`⌘`, `⁂`, `※`)
 * removed.
 */
val String.sanitized: String
    get() =
        replace("⌘", "")
            .replace("⁂", "")
            .replace("※", "")
