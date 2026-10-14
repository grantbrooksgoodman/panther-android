//
//  Application.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther

import android.content.Context
import us.neotechnica.panther.bundle.CacheDomainList
import us.neotechnica.panther.bundle.ExceptionMetadataService
import us.neotechnica.panther.bundle.LoggerDomainSubscription
import us.neotechnica.panther.bundle.PermanentKeyDelegate
import us.neotechnica.panther.bundle.application
import us.neotechnica.panther.bundle.developermode.AppActions
import us.neotechnica.panther.bundle.navigators.ChatRoute
import us.neotechnica.panther.bundle.navigators.SettingsRoute
import us.neotechnica.panther.bundle.navigators.UserContentRoute
import us.neotechnica.panther.bundle.populatedTemporaryCaches
import us.neotechnica.panther.bundle.userSessionService
import us.neotechnica.panther.designsystem.modules.alertkit.dependencies.alertKitConfig
import us.neotechnica.panther.designsystem.modules.alertkit.models.HUDConfig
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.SFSymbol
import us.neotechnica.panther.designsystem.modules.developermode.services.DevModeService
import us.neotechnica.panther.designsystem.modules.foundation.extensions.registerBuildInfoOverlayDotIndicatorColorDelegate
import us.neotechnica.panther.designsystem.modules.foundation.models.SystemAnimation
import us.neotechnica.panther.designsystem.modules.foundation.overlay.Overlay
import us.neotechnica.panther.designsystem.modules.foundation.rootsheet.RootSheets
import us.neotechnica.panther.designsystem.modules.foundation.services.ReportDelegate
import us.neotechnica.panther.designsystem.modules.foundation.services.SoundPlayer
import us.neotechnica.panther.designsystem.modules.foundation.services.TranslationLogger
import us.neotechnica.panther.modules.common.extensions.ApplicationStorageKey
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.common.services.sources.AnalyticsService
import us.neotechnica.panther.modules.common.services.sources.CommonPropertyLists
import us.neotechnica.panther.modules.common.services.sources.DefaultAttributeDetectionServiceDelegate
import us.neotechnica.panther.modules.common.services.sources.DocumentExportService
import us.neotechnica.panther.modules.common.services.sources.ErrorReportingService
import us.neotechnica.panther.modules.common.services.sources.InviteService
import us.neotechnica.panther.modules.common.services.sources.LoggerPresentationService
import us.neotechnica.panther.modules.common.services.sources.RegionDetailService
import us.neotechnica.panther.modules.common.services.sources.UpdateService
import us.neotechnica.panther.modules.common.services.sources.audio.TextToSpeechService
import us.neotechnica.panther.modules.common.services.sources.contacts.ContactService
import us.neotechnica.panther.modules.content.user.dependencies.chatPageViewService
import us.neotechnica.panther.modules.content.user.dependencies.settingsPageViewService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.AudioMessagePlaybackService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.MediaActionHandlerService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.contextmenu.sources.ContextMenuActionHandlerService
import us.neotechnica.panther.modules.networking.sources.translation.delegates.LocalTranslationArchiverDelegate
import us.neotechnica.panther.modules.networking.sources.user.models.DeviceID
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.entity.extensions.UserSessionServiceStorageKey
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.modules.session.state.services.MessageOutboxService
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.modules.session.sync.services.ConversationObserverService
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.RootRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.auth.interfaces.AuthDelegate
import us.neotechnica.panther.networking.modules.common.dependencies.networking
import us.neotechnica.panther.networking.modules.common.services.BuildInfoOverlayDotIndicatorColorDelegate
import us.neotechnica.panther.networking.modules.database.interfaces.DatabaseDelegate
import us.neotechnica.panther.networking.modules.storage.interfaces.StorageDelegate
import us.neotechnica.panther.subsystem.AppSubsystem
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.BuildInfoOverlay
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import us.neotechnica.panther.subsystem.modules.localization.services.LocalizedStringResolver
import us.neotechnica.panther.translator.Translator
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

/**
 * The app's bootstrap configuration and shared application
 * namespace.
 *
 * Use [Application] to perform one-time launch setup through
 * [initialize], to reset the app to a signed-out, freshly installed
 * state, and to dismiss presented sheets.
 */
object Application {
    // MARK: - Dependencies

    private val analyticsService: AnalyticsService by Dependency { it.commonServices.analytics }
    private val audioMessagePlaybackService: AudioMessagePlaybackService by Dependency { it.chatPageViewService.audioMessagePlayback }
    private val auth: AuthDelegate by Dependency { it.networking.auth }
    private val contactService: ContactService by Dependency { it.commonServices.contact }
    private val conversationObserver: ConversationObserverService by Dependency { it.clientSession.sync.conversationObserver }
    private val database: DatabaseDelegate by Dependency { it.networking.database }
    private val inviteService: InviteService by Dependency { it.commonServices.invite }
    private val outbox: MessageOutboxService by Dependency { it.clientSession.outbox }
    private val regionDetailService: RegionDetailService by Dependency { it.commonServices.regionDetail }
    private val sessionStore: SessionStore by Dependency { it.clientSession.store }
    private val storage: StorageDelegate by Dependency { it.networking.storage }
    private val textToSpeechService: TextToSpeechService by Dependency { it.commonServices.audio.textToSpeech }
    private val userSession: UserSessionService by Dependency { it.clientSession.entity.user }

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

    /**
     * A Boolean value that indicates whether the app is operating
     * in staging mode.
     */
    val isInStagingMode: Boolean
        get() =
            Persistent.booleanOrNull(
                PersistentStorageKey.application(ApplicationStorageKey.IS_IN_STAGING_MODE),
            ) ?: false

    /** The moment the current launch began loading. */
    var loadStartDate: Date = Date()

    private var appContext: Context? = null

    // The two-letter language code of the device, falling back to
    // "en" when unavailable.
    private val systemLanguageCode: String
        get() {
            val languageCode = Locale.getDefault().language
            if (languageCode.length < 2) return "en"
            return languageCode.take(2)
        }

    // MARK: - Initialization

    /**
     * Performs one-time application setup at launch.
     *
     * Selects the app's styling, registers the subsystem
     * delegates, configures build metadata, starts the networking
     * framework, prewarms the database and storage connections, and
     * selects the first-run environment.
     * Call this method exactly once, before any other subsystem API
     * is used.
     *
     * @param context A context used to resolve the application
     *   context.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        Font.usesSanFrancisco = USE_IOS_STYLING
        SFSymbol.usesSFSymbols = USE_IOS_STYLING
        SystemAnimation.usesAppleAnimations = USE_IOS_STYLING

        initializeServices(context)
        registerDelegates()
        configureBuild(context)
        setUpSubsystem()

        Logger.log("Application launched.")

        setUpNetworking(context)
        prewarmConnections()
        selectFirstRunEnvironment()
    }

    // MARK: - Methods

    /** Dismisses every presented sheet in the app. */
    fun dismissSheets() {
        // The chat and settings flows present sheets through their
        // navigators; the root sheet is presented above the modal content.
        val navigation = DependencyValues.current.navigation
        navigation.navigate(Route.Chat(ChatRoute.Sheet(null)))
        navigation.navigate(Route.Settings(SettingsRoute.Sheet(null)))
        RootSheets.dismiss()
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
        outbox.removeAll()
        sessionStore.advanceEpoch()
        conversationObserver.stopObserving()

        if (!preserveCurrentUserID) {
            userSession.stopObservingCurrentUserChanges()
        }

        CoreUtilities.clearCaches()
        eraseDirectories()

        Persistent.reset(
            preserving =
                Persistent.KeyPreservationStrategy.PermanentAndSubsystemKeys(
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

        if (!preserveCurrentUserID) {
            runCatching { auth.signOut() }
                .onFailure { Logger.log(it as? Exception ?: Exception.from(it, ExceptionMetadata(this))) }
        }

        val procedure = onCompletion ?: return
        dismissSheets()

        when (procedure) {
            ResetCompletionProcedure.EXIT_GRACEFULLY -> beginGracefulExit()

            ResetCompletionProcedure.NAVIGATE_TO_SPLASH -> {
                DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Stack(emptyList())))
                DependencyValues.current.navigation.navigate(
                    Route.Root(RootRoute.Modal(RootNavigatorState.ModalPaths.Splash)),
                )
            }
        }
    }

    /**
     * Presents the splash page behind an activity indicator overlay,
     * then terminates the app after a one-second delay.
     *
     * Use [beginGracefulExit] to complete a flow that requires the app
     * to restart – such as clearing caches – after the user has been
     * informed. The reset completion procedure and the settings page's
     * exit flows share this sequence.
     */
    fun beginGracefulExit() {
        Overlay.addOverlay(activityIndicator = Overlay.OverlayActivityIndicatorConfiguration.largeWhite)
        DependencyValues.current.navigation.navigate(
            Route.Root(RootRoute.Modal(RootNavigatorState.ModalPaths.Splash)),
        )
        CoreUtilities.exitGracefully()
    }

    // MARK: - Auxiliary

    private fun initializeServices(context: Context) {
        analyticsService.initialize(context)
        LocalizedStringResolver.initialize(context)
        RuntimeStorage.languageCodeDictionary = LocalizedStringResolver.languageDisplayNames()
        Persistent.initialize(context)
        FileStore.initialize(context)
        SoundPlayer.initialize(context)
        CommonPropertyLists.initialize(context)
        regionDetailService.initialize(context)
        contactService.initialize(context)
        DeviceID.initialize(context)
        inviteService.initialize(context)
        DependencyValues.current.settingsPageViewService.initialize(context)
        textToSpeechService.initialize(context)
        audioMessagePlaybackService.initialize(context)
        MediaActionHandlerService.initialize(context)
        ContextMenuActionHandlerService.initialize(context)
        DefaultAttributeDetectionServiceDelegate.initialize(context)
        DocumentExportService.initialize(context)
    }

    private fun registerDelegates() {
        Logger.setPresentationDelegate(LoggerPresentationService)
        AppSubsystem.delegates.registerBuildInfoOverlayDotIndicatorColorDelegate(BuildInfoOverlayDotIndicatorColorDelegate)
        AppSubsystem.delegates.registerCacheDomainListDelegate(CacheDomainList)
        AppSubsystem.delegates.registerExceptionMetadataDelegate(ExceptionMetadataService)
        AppSubsystem.delegates.registerForcedUpdateModalDelegate(UpdateService)
        AppSubsystem.delegates.registerLoggerDomainSubscriptionDelegate(LoggerDomainSubscription)
        AppSubsystem.delegates.registerPermanentPersistentStorageKeyDelegate(PermanentKeyDelegate)
        AppSubsystem.delegates.registerErrorReportDelegate(ErrorReportingService)
        DevModeService.registerAppActionDelegate(AppActions)
        LocalTranslationArchiverDelegate.registerWithDependencies()
        Translator.config.registerLoggerDelegate(TranslationLogger)
        ReportDelegate.registerWithDependencies()

        DependencyValues.current.alertKitConfig.overrideTranslationHUDConfig(
            HUDConfig(appearsAfter = 500.milliseconds, isModal = true),
        )
    }

    private fun setUpSubsystem() {
        CoreUtilities.setLanguageCode(systemLanguageCode)

        Task.delayed(by = BUILD_INFO_OVERLAY_SETUP_DELAY_MILLISECONDS.milliseconds) {
            val hidesBuildInfoOverlay = Persistent.booleanOrNull(PersistentStorageKey.hidesBuildInfoOverlay)
            if (hidesBuildInfoOverlay != null && Build.isDeveloperModeEnabled) {
                if (hidesBuildInfoOverlay) BuildInfoOverlay.hide() else BuildInfoOverlay.show()
            } else if (Build.milestone == Build.Milestone.GENERAL_RELEASE) {
                BuildInfoOverlay.hide()
            } else {
                BuildInfoOverlay.show()
            }
        }
    }

    private fun eraseDirectories() {
        val context = appContext ?: return
        context.filesDir?.listFiles()?.forEach { it.deleteRecursively() }
        context.noBackupFilesDir?.listFiles()?.forEach { it.deleteRecursively() }
        context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
    }

    // MARK: - Constants

    internal const val BUILD_INFO_ASSET = "build_info.properties"
    private const val BUILD_INFO_OVERLAY_SETUP_DELAY_MILLISECONDS = 50L
    internal const val CODE_NAME = "Panther"
    internal const val FINAL_NAME = "Hello"
    internal const val APP_STORE_BUILD_NUMBER = 0
    internal const val MILLIS_PER_SECOND = 1_000L

    // Whether the app uses its system-style animations, the San Francisco
    // font, and SF Symbols. Set this to `false` to use Android's default
    // animations, the Inter font, and Material Symbols instead.
    private const val USE_IOS_STYLING = true
}
