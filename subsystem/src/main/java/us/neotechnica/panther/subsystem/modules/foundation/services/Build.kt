//
//  Build.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.services

import us.neotechnica.panther.subsystem.modules.foundation.models.Milestone
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The build configuration for the current app: version information,
 * milestone, and the derived identifiers shown in the build-info
 * overlay.
 *
 * Mirrors the iOS `Build`. It is populated once at startup via
 * [initialize] with values injected by the per-compile build-number
 * bump in the app module's Gradle script (which stamps the build
 * number and dates into a `build_info.properties` asset on every
 * compile, the analog of the iOS Run Script phase).
 */
object Build {
    // MARK: - Properties

    /** The build number of the most recent store release. */
    var appStoreBuildNumber: Int = 0
        private set

    /** The build number, incremented on every compile. */
    var buildNumber: Int = 0
        private set

    /** The internal code name for the current release. */
    var codeName: String = "Template"
        private set

    /** The public-facing product name used in general-release builds. */
    var finalName: String = "Template"
        private set

    /** The short bundle version string (for example, `"1.0"`). */
    var bundleVersion: String = "0.0.0"
        private set

    /** The release-cycle stage of this build. */
    var milestone: Milestone = Milestone.ALPHA
        private set

    /** The date this build was compiled. */
    var buildDate: Date = Date()
        private set

    /** The date this project was first compiled. */
    var firstCompileDate: Date = Date()
        private set

    /** Whether [initialize] has been called. */
    var isConfigured: Boolean = false
        private set

    // MARK: - Computed Properties

    /** The major version number extracted from [bundleVersion]. */
    val appStoreReleaseVersion: Int
        get() = bundleVersion.substringBefore(".").filter { it.isDigit() }.toIntOrNull() ?: 0

    /** The number of builds since the last store release. */
    val revisionBuildNumber: Int
        get() = (buildNumber - appStoreBuildNumber).coerceAtLeast(0)

    /** An alphabetic revision identifier derived from [revisionBuildNumber]. */
    val bundleRevision: String
        get() = bundleRevision(revisionBuildNumber)

    /**
     * A SKU encoding the build date, a three-letter code-name
     * abbreviation, the build number, and the milestone.
     */
    val buildSKU: String
        get() = buildSKU()

    /** The one-line build summary shown in the overlay button. */
    val buildInfoString: String
        get() = "$codeName $bundleVersion ($buildNumber${milestone.shortString}/${bundleRevision.lowercase()})"

    // MARK: - Initialization

    /** Populates the build configuration. Called once at startup. */
    @Suppress("LongParameterList")
    fun initialize(
        appStoreBuildNumber: Int,
        buildNumber: Int,
        codeName: String,
        finalName: String,
        bundleVersion: String,
        milestone: Milestone,
        buildDate: Date,
        firstCompileDate: Date,
    ) {
        this.appStoreBuildNumber = appStoreBuildNumber
        this.buildNumber = buildNumber
        this.codeName = codeName
        this.finalName = finalName
        this.bundleVersion = bundleVersion
        this.milestone = milestone
        this.buildDate = buildDate
        this.firstCompileDate = firstCompileDate
        isConfigured = true
    }

    // MARK: - Auxiliary

    private fun bundleRevision(revisionBuildNumber: Int): String {
        val alphabet = ('A'..'Z').toList()
        val revisionMilestone = revisionBuildNumber / REVISION_MILESTONE_DIVISOR
        if (revisionMilestone < alphabet.size) {
            return alphabet[revisionMilestone].toString()
        }

        var remainder = revisionMilestone
        val letters = StringBuilder("Z")
        while (remainder >= alphabet.size) {
            remainder -= alphabet.size
            letters.append(if (remainder < alphabet.size) alphabet[remainder] else 'Z')
        }

        val zCount = letters.count { it == 'Z' }
        return if (zCount > MAX_TRAILING_Z) "Z$zCount${letters.filter { it != 'Z' }}" else letters.toString()
    }

    private fun buildSKU(): String {
        val dateString = SimpleDateFormat("ddMMyy", Locale.US).format(buildDate)
        val threeLetterID =
            if (codeName.length > THREE_LETTER_ID_LENGTH) {
                "${codeName.first()}${codeName[codeName.length / 2]}${codeName.last()}".uppercase()
            } else {
                codeName.uppercase()
            }
        return "$dateString-$threeLetterID-${"%06d".format(buildNumber)}${milestone.shortString}"
    }

    // MARK: - Companion

    private const val REVISION_MILESTONE_DIVISOR = 150
    private const val MAX_TRAILING_Z = 3
    private const val THREE_LETTER_ID_LENGTH = 3
}
