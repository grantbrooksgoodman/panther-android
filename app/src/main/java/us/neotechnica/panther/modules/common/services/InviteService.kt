//
//  InviteService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 25/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import android.content.Context
import android.content.Intent

/**
 * Invites the user's contacts to the app.
 *
 * [initialize] must be called once with the application context before
 * [presentInvitationPrompt].
 */
object InviteService {
    // MARK: - Properties

    private var appContext: Context? = null

    // MARK: - Init

    /** Prepares the service with the application context. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    // MARK: - Present Invitation Prompt

    /**
     * Presents the system share sheet for sending an app invitation.
     */
    fun presentInvitationPrompt() {
        val context = appContext ?: return
        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "$INVITE_MESSAGE\n${playStoreUrl(context)}")
            }
        val chooser = Intent.createChooser(shareIntent, INVITE_FRIENDS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        runCatching { context.startActivity(chooser) }
    }

    // MARK: - Auxiliary

    private fun playStoreUrl(context: Context): String = "https://play.google.com/store/apps/details?id=${context.packageName}"

    // MARK: - Companion

    private const val INVITE_FRIENDS = "Invite friends"
    private const val INVITE_MESSAGE = "Come chat with me on Hello!"
}
