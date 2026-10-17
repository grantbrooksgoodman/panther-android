//
//  Throwable+FoundationExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.extensions

// A deterministic identifier derived from the throwable's type and,
// when given, its error code: `[domain]` or `[domain:code]`.
internal fun Throwable.staticIdentifier(code: Any? = null): String {
    val domain = javaClass.name
    return if (code == null) "[$domain]" else "[$domain:$code]"
}
