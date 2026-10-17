//
//  TranslationArchiverDelegate.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.interfaces

/**
 * A delegate that clears the local translation archive on the
 * subsystem's behalf.
 *
 * The subsystem cannot reach the translation layer directly, so
 * the `localTranslationArchive` cache domain clears the archive
 * through this delegate. Register the app's archiver once at launch
 * with [AppSubsystem.Delegates.registerTranslationArchiverDelegate].
 *
 * **Important:** [clearArchive] may be invoked from any thread.
 */
interface TranslationArchiverDelegate {
    /** Removes all cached translations from the archive. */
    fun clearArchive()
}
