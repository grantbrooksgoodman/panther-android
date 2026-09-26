//
//  Application.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 25/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle

import android.content.Context
import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.modules.session.entity.extensions.UserSessionServiceStorageKey
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.modules.session.state.services.MessageOutboxService
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.modules.session.sync.services.ConversationObserverService
import us.neotechnica.panther.navigation.ChatRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.RootRoute
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import us.neotechnica.panther.translator.Translator
import java.util.Date
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

/**
 * The app's shared application namespace.
 *
 * Use [Application] to reset the app to a signed-out, freshly
 * installed state and to dismiss presented sheets.
 */
object Application {
    // MARK: - Types

    /** A follow-up action to perform after a reset completes. */
    enum class ResetCompletionProcedure {
        /**
         * Presents the splash page behind an activity indicator
         * overlay, then terminates the app after a one-second delay.
         */
        EXIT_GRACEFULLY,

        /** Clears the user content navigation stack and presents the splash page. */
        NAVIGATE_TO_SPLASH,
    }

    // MARK: - Properties

    /** The moment the current launch began loading. */
    var loadStartDate: Date = Date()

    private var appContext: Context? = null

    // MARK: - Initialization

    /** Prepares the application namespace with the application context. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    // MARK: - Methods

    /** Dismisses every presented sheet in the app. */
    fun dismissSheets() {
        // Only the chat flow presents a sheet on Android; the other
        // navigators have no sheet route to dismiss.
        DependencyValues.current.navigation.navigate(Route.Chat(ChatRoute.Sheet(null)))
    }

    /**
     * Resets the app to a signed-out, freshly installed state.
     *
     * Tears down the session – clearing the outbox, the session
     * store, and conversation observation – then clears all caches,
     * erases the app's on-disk directories, resets persisted
     * defaults, and signs the current user out. Permanent persistent
     * storage keys survive the reset.
     *
     * @param preserveCurrentUserID Whether the current user's
     *   identifier should survive the reset.
     * @param onCompletion The follow-up action to perform once the
     *   reset completes, or `null` to leave the interface untouched.
     */
    fun reset(
        preserveCurrentUserID: Boolean = false,
        onCompletion: ResetCompletionProcedure? = null,
    ) {
        MessageOutboxService.removeAll()
        SessionStore.clear()
        ConversationObserverService.stopObserving()

        if (!preserveCurrentUserID) {
            UserSessionService.stopObservingCurrentUserChanges()
        }

        CoreUtilities.clearCaches()
        eraseDirectories()

        Persistent.reset(
            preserving =
                Persistent.permanentAndSubsystemKeys(
                    plus =
                        if (preserveCurrentUserID) {
                            listOf(
                                PersistentStorageKey.userSessionService(UserSessionServiceStorageKey.CURRENT_USER_ID),
                            )
                        } else {
                            null
                        },
                ),
        )
        RuntimeStorage.remove(StoredItemKey.populatedTemporaryCaches)

        runCatching { Networking.config.authDelegate.signOut() }
            .onFailure { Logger.log("Failed to sign out during reset. ${it.message}") }

        val procedure = onCompletion ?: return
        dismissSheets()

        when (procedure) {
            ResetCompletionProcedure.EXIT_GRACEFULLY -> {
                Overlay.show()
                DependencyValues.current.navigation.navigate(
                    Route.Root(RootRoute.SetModal(RootNavigatorState.ModalPath.Splash)),
                )
                Task.delayed(by = 1.seconds) { exitGracefully() }
            }

            ResetCompletionProcedure.NAVIGATE_TO_SPLASH -> {
                DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Stack(emptyList())))
                DependencyValues.current.navigation.navigate(
                    Route.Root(RootRoute.SetModal(RootNavigatorState.ModalPath.Splash)),
                )
            }
        }
    }

    // MARK: - Auxiliary

    private fun eraseDirectories() {
        val context = appContext ?: return
        context.filesDir?.listFiles()?.forEach { it.deleteRecursively() }
        context.noBackupFilesDir?.listFiles()?.forEach { it.deleteRecursively() }
        context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
    }

    private fun exitGracefully() {
        Translator.config.currentActivityProvider?.invoke()?.finishAffinity()
        exitProcess(0)
    }
}
