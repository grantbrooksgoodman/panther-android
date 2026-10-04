//
//  BuildInfoButtonStrings.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.models

import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.entity.extensions.currentUserID
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import java.util.Calendar

/**
 * A build information string displayed by the build info button.
 *
 * The build info button cycles through a fixed sequence of strings
 * describing the running build; use [next] to advance to the following
 * entry.
 *
 * @property key The kind of string this instance represents.
 */
data class BuildInfoButtonStrings(
    val key: BuildInfoButtonStringKey,
) {
    // MARK: - Types

    /** A kind of build information string. */
    enum class BuildInfoButtonStringKey {
        /** The bundle version with its build number and revision. */
        BUNDLE_VERSION_AND_BUILD_NUMBER,

        /** The build SKU. */
        BUILD_SKU,

        /** The project ID. */
        PROJECT_ID,

        /** The current user's ID with the network environment. */
        USER_ID_AND_NETWORK_ENVIRONMENT,

        /** The copyright notice. */
        COPYRIGHT,
    }

    // MARK: - Properties

    /** The text the build info button displays. */
    val labelText: String =
        when (key) {
            BuildInfoButtonStringKey.BUNDLE_VERSION_AND_BUILD_NUMBER ->
                "${LocalizedStringKey.Version.localized()} ${Build.bundleVersion} " +
                    "(${Build.buildNumber}${Build.milestone.shortString}/${Build.bundleRevision.lowercase()})"

            BuildInfoButtonStringKey.BUILD_SKU -> Build.buildSKU

            BuildInfoButtonStringKey.PROJECT_ID -> "$PROJECT_ID_PREFIX | ${Build.projectID}"

            BuildInfoButtonStringKey.USER_ID_AND_NETWORK_ENVIRONMENT ->
                "${User.currentUserID ?: "�"} | ${Networking.config.environment.shortString}"

            BuildInfoButtonStringKey.COPYRIGHT ->
                "Copyright © ${Calendar.getInstance().get(Calendar.YEAR)} NEOTechnica Corp."
        }

    // MARK: - Computed Properties

    /** The strings for the next entry in the display sequence. */
    val next: BuildInfoButtonStrings
        get() =
            when (key) {
                BuildInfoButtonStringKey.BUNDLE_VERSION_AND_BUILD_NUMBER ->
                    BuildInfoButtonStrings(BuildInfoButtonStringKey.BUILD_SKU)

                BuildInfoButtonStringKey.BUILD_SKU ->
                    BuildInfoButtonStrings(BuildInfoButtonStringKey.PROJECT_ID)

                BuildInfoButtonStringKey.PROJECT_ID ->
                    BuildInfoButtonStrings(BuildInfoButtonStringKey.USER_ID_AND_NETWORK_ENVIRONMENT)

                BuildInfoButtonStringKey.USER_ID_AND_NETWORK_ENVIRONMENT ->
                    BuildInfoButtonStrings(BuildInfoButtonStringKey.COPYRIGHT)

                BuildInfoButtonStringKey.COPYRIGHT ->
                    BuildInfoButtonStrings(BuildInfoButtonStringKey.BUNDLE_VERSION_AND_BUILD_NUMBER)
            }

    // MARK: - Companion

    private companion object {
        private const val PROJECT_ID_PREFIX = "7B0U3X1V"
    }
}
