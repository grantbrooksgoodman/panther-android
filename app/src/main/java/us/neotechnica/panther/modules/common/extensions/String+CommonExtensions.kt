//
//  String+CommonExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 25/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.extensions

/**
 * The first 32 characters of the string.
 *
 * If the string contains 32 or fewer characters, this property
 * returns the string unchanged.
 */
val String.shortened: String
    get() = take(32)
