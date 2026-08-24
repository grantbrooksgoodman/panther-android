//
//  AppConstants.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 24/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.constants

/**
 * A namespace for app-level constants, organized by value type,
 * mirroring the iOS `AppConstants` enum.
 *
 * The empty [Floats], [Colors], and [Strings] domains anchor the naming
 * convention: because Kotlin cannot add nested types to an object from
 * another file, each consumer declares its own top-level group named
 * `(Consumer)Floats` / `(Consumer)Colors` / `(Consumer)Strings` in an
 * `AppConstants+(Consumer).kt` file — the analog of the iOS
 * `extension AppConstants.CGFloats { enum SomeView { … } }`. Consumers
 * alias their group under a `// MARK: - Constants Accessors` section,
 * for example `private typealias Floats = SomeViewFloats`.
 */
object AppConstants {
    object Floats

    object Colors

    object Strings
}
