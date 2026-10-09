//
//  ForcedUpdateModalDelegate.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.interfaces

/**
 * A type that configures the full-screen modal requiring the
 * user to update the app.
 *
 * When a newer version of the app is required – for example,
 * because the server no longer supports the running version –
 * an undismissable modal can be presented that prompts the user
 * to install the update. Conform to this interface to control
 * where the install button redirects the user.
 *
 * To trigger presentation of the modal, set the
 * `isForcedUpdateRequired` shared state to `true`.
 */
interface ForcedUpdateModalDelegate {
    /**
     * The URL to open when the user taps the install button, or
     * `null` if no redirect is configured.
     *
     * Typically, this points to the app's store listing.
     */
    val installButtonRedirectURL: String?
}
