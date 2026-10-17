//
//  ApplicationSuspensionDelegate.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.interfaces

/**
 * A delegate that returns the app to the Home screen on the
 * subsystem's behalf.
 *
 * The subsystem cannot reach the app's activity directly, so
 * [CoreUtilities.exitGracefully][us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities.exitGracefully]
 * suspends the app through this delegate before it terminates the
 * process. Register the app's activity once it is created with
 * [AppSubsystem.Delegates.registerApplicationSuspensionDelegate].
 *
 * **Important:** [suspendToHome] may be invoked from any thread.
 */
interface ApplicationSuspensionDelegate {
    /** Moves the app's task to the background, showing the Home screen. */
    fun suspendToHome()
}
