//
//  Application+Setup.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle

import android.content.Context
import us.neotechnica.panther.BuildConfig
import us.neotechnica.panther.modules.common.extensions.ApplicationStorageKey
import us.neotechnica.panther.modules.common.extensions.isEmulator
import us.neotechnica.panther.modules.common.services.NetworkActivityIndicatorService
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.dependencies.networking
import us.neotechnica.panther.networking.modules.common.models.NetworkEnvironment
import us.neotechnica.panther.networking.modules.health.models.NetworkHealthConfiguration
import us.neotechnica.panther.networking.modules.health.models.NetworkHealthProbeConfiguration
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import java.util.Date
import java.util.Properties

// MARK: - Setup

internal fun Application.configureBuild(context: Context) {
    val (buildNumber, buildDate, firstCompileDate) = readBuildInfo(context)
    Build.initialize(
        context = context,
        appStoreBuildNumber = APP_STORE_BUILD_NUMBER,
        buildNumber = buildNumber,
        codeName = CODE_NAME,
        finalName = FINAL_NAME,
        bundleVersion = BuildConfig.VERSION_NAME,
        // TODO: FIX BEFORE PLAY STORE RELEASE.
        milestone = Build.Milestone.BETA, // resolveBuildMilestone(),
        buildDate = Date(buildDate * MILLIS_PER_SECOND),
        firstCompileDate = Date(firstCompileDate * MILLIS_PER_SECOND),
    )
}

internal fun Application.resolveBuildMilestone(): Build.Milestone {
    val key = PersistentStorageKey.application(ApplicationStorageKey.BUILD_MILESTONE_STRING)
    val persisted = Persistent.string(key)
    val milestone =
        persisted?.let { Build.Milestone.from(it) }
            ?: if (Build.isEmulator) Build.Milestone.BETA else Build.Milestone.GENERAL_RELEASE
    Persistent.setString(key, milestone.rawValue)
    return milestone
}

private fun readBuildInfo(context: Context): Triple<Int, Long, Long> =
    runCatching {
        context.assets.open(Application.BUILD_INFO_ASSET).use { stream ->
            val properties = Properties().apply { load(stream) }
            Triple(
                properties.getProperty("buildNumber", "0").toInt(),
                properties.getProperty("buildDate", "0").toLong(),
                properties.getProperty("firstCompileDate", "0").toLong(),
            )
        }
    }.getOrDefault(Triple(0, 0L, 0L))

internal fun Application.setUpNetworking(context: Context) {
    Networking.initialize(
        context = context,
        defaultEnvironment = NetworkEnvironment.from(BuildConfig.NETWORK_ENVIRONMENT),
        useDebugAppCheckProvider = BuildConfig.DEBUG,
    )
    Networking.config.registerActivityIndicatorDelegate(NetworkActivityIndicatorService)
    Networking.config.setNetworkHealthConfiguration(
        NetworkHealthConfiguration.default.copy(
            probeConfiguration = NetworkHealthProbeConfiguration(url = "https://www.apple.com"),
        ),
    )
}

internal fun Application.prewarmConnections() {
    DependencyValues.current.networking.database
        .prewarm()
    DependencyValues.current.networking.storage
        .prewarm()
}

internal fun Application.selectFirstRunEnvironment() {
    val hasRunOnceKey = PersistentStorageKey.application(ApplicationStorageKey.HAS_RUN_ONCE)
    if (Build.isEmulator && Persistent.booleanOrNull(hasRunOnceKey) == null) {
        Networking.config.setEnvironment(NetworkEnvironment.DEVELOPMENT)
        Persistent.setBoolean(hasRunOnceKey, true)
    } else if (Build.milestone == Build.Milestone.GENERAL_RELEASE) {
        Networking.config.setEnvironment(NetworkEnvironment.PRODUCTION)
    }
}
