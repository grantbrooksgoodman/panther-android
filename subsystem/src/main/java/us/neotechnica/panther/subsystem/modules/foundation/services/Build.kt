//
//  Build.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * The build configuration for the current app: version information,
 * milestone, and the derived identifiers shown in the build-info
 * overlay.
 *
 * It is populated once at startup via
 * [initialize] with values injected by the per-compile build-number
 * bump in the app module's Gradle script (which stamps the build
 * number and dates into a `build_info.properties` asset on every
 * compile).
 */
object Build {
    // MARK: - Types

    /**
     * The release-cycle stage of a build.
     *
     * Each milestone has a single-character [shortString] that is
     * appended to build numbers in the build-info overlay (for
     * example, `"b"` for beta).
     */
    enum class Milestone(
        /** The underlying raw value. */
        val rawValue: String,
        /** A single-character abbreviation for this milestone. */
        val shortString: String,
    ) {
        /** A very early development build. */
        PRE_ALPHA("pre-alpha", "p"),

        /** An early development build with core features in progress. */
        ALPHA("alpha", "a"),

        /** A feature-complete build undergoing testing. */
        BETA("beta", "b"),

        /** A build that is a candidate for general release. */
        RELEASE_CANDIDATE("release candidate", "c"),

        /** A production release distributed through the store. */
        GENERAL_RELEASE("general", "g"),
        ;

        // MARK: - Companion

        companion object {
            /** The milestone matching [rawValue], or `null` if unrecognized. */
            fun from(rawValue: String): Milestone? = entries.firstOrNull { it.rawValue == rawValue }
        }
    }

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

    /** The short bundle version string (for example, `"1.0.0"`). */
    var bundleVersion: String = "0.0.0"
        private set

    /** The network environment this build targets (for example, `"development"`). */
    var environment: String = "production"
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

    @Volatile
    private var appContext: Context? = null

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

    /**
     * The six-digit code required to bypass build expiry.
     *
     * This code is derived deterministically from [codeName] and
     * remains stable across launches of the same build.
     */
    val expirationOverrideCode: String
        get() = deriveExpirationOverrideCode()

    /**
     * A Boolean value that indicates whether logging is enabled.
     *
     * Logging is enabled for every build except general releases.
     */
    val loggingEnabled: Boolean
        get() = milestone != Milestone.GENERAL_RELEASE

    /**
     * An eight-character alphanumeric identifier for the project,
     * derived deterministically from [codeName] and [firstCompileDate].
     */
    val projectID: String
        get() = deriveProjectID()

    /**
     * A Boolean value that indicates whether the device currently
     * has network connectivity.
     */
    val isOnline: Boolean
        get() = getNetworkStatus()

    /**
     * A Boolean value that indicates whether developer mode is
     * enabled.
     *
     * Developer mode is always disabled in general-release builds.
     */
    val isDeveloperModeEnabled: Boolean
        get() =
            if (milestone == Milestone.GENERAL_RELEASE) {
                false
            } else {
                Persistent.booleanOrNull(PersistentStorageKey.isDeveloperModeEnabled) ?: false
            }

    // MARK: - Initialization

    /** Populates the build configuration. Called once at startup. */
    @Suppress("LongParameterList")
    fun initialize(
        context: Context? = null,
        appStoreBuildNumber: Int,
        buildNumber: Int,
        codeName: String,
        finalName: String,
        bundleVersion: String,
        environment: String,
        milestone: Milestone,
        buildDate: Date,
        firstCompileDate: Date,
    ) {
        this.appContext = context?.applicationContext
        this.appStoreBuildNumber = appStoreBuildNumber
        this.buildNumber = buildNumber
        this.codeName = codeName
        this.finalName = finalName
        this.bundleVersion = bundleVersion
        this.environment = environment
        this.milestone = milestone
        this.buildDate = buildDate
        this.firstCompileDate = firstCompileDate
        isConfigured = true
    }

    // MARK: - Setters

    /** Enables or disables developer mode, persisting the change. */
    fun setIsDeveloperModeEnabled(isDeveloperModeEnabled: Boolean) {
        if (!isDeveloperModeEnabled &&
            Persistent.booleanOrNull(PersistentStorageKey.hidesBuildInfoOverlay) == true
        ) {
            BuildInfoOverlay.show()
        }

        Persistent.setBoolean(PersistentStorageKey.isDeveloperModeEnabled, isDeveloperModeEnabled)
    }

    // MARK: - Auxiliary

    private fun getNetworkStatus(): Boolean {
        val manager =
            appContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun alphabeticalPosition(character: Char): Int? {
        val lowercased = character.lowercaseChar()
        if (lowercased !in 'a'..'z') return null
        return lowercased - 'a' + 1
    }

    private fun bundleRevision(revisionBuildNumber: Int): String {
        val alphabet = ('A'..'Z').toList()
        var remainder = revisionBuildNumber / REVISION_MILESTONE_DIVISOR
        val revisionLetters = StringBuilder()
        do {
            revisionLetters.insert(0, alphabet[remainder % alphabet.size])
            remainder = remainder / alphabet.size - 1
        } while (remainder >= 0)
        return revisionLetters.toString()
    }

    private fun deriveExpirationOverrideCode(): String {
        if (codeName.isEmpty()) return "000000"

        val firstCharacter = codeName.first()
        val lastCharacter = codeName.last()
        val middleCharacter = codeName[codeName.length / 2]

        return listOf(firstCharacter, middleCharacter, lastCharacter)
            .mapNotNull { character -> alphabeticalPosition(character)?.let { "%02d".format(it) } }
            .joinToString("")
    }

    private fun deriveProjectID(): String {
        // Normalize code name
        val rawName = codeName.trim().lowercase().ifEmpty { "template" }

        // Get code name letter positions
        val firstLetterPosition = rawName.firstOrNull()?.let { alphabeticalPosition(it) } ?: PROJECT_ID_DEFAULT_LETTER_POSITION
        val lastLetterPosition = rawName.lastOrNull()?.let { alphabeticalPosition(it) } ?: PROJECT_ID_DEFAULT_LETTER_POSITION
        val middleLetter = rawName.getOrNull(rawName.length / 2)?.toString() ?: "A"
        val middleLetterPosition = middleLetter.firstOrNull()?.let { alphabeticalPosition(it) } ?: PROJECT_ID_DEFAULT_LETTER_POSITION

        // Calculate numeric ID
        val calendar = Calendar.getInstance().apply { time = firstCompileDate }
        val dateProduct = calendar.get(Calendar.DAY_OF_MONTH) * (calendar.get(Calendar.MONTH) + 1) * calendar.get(Calendar.YEAR)
        val letterProduct = firstLetterPosition * middleLetterPosition * lastLetterPosition
        val numericID = (letterProduct.toLong() * dateProduct.toLong()).toString().filter { it.isDigit() }

        // Build alphanumeric ID
        val components = mutableListOf<String>()
        numericID.mapNotNull { it.toString().toIntOrNull() }.forEach { digit ->
            components.add(digit.toString())
            components.add(ciphered(middleLetter, digit).uppercase())
        }

        val result = components.distinct().take(PROJECT_ID_LENGTH).toMutableList()

        // If the ID is too short, continuously add ciphered middle letter until eight characters
        var currentLetter = middleLetter
        while (result.size < PROJECT_ID_LENGTH) {
            val position = currentLetter.firstOrNull()?.let { alphabeticalPosition(it) } ?: break
            currentLetter = ciphered(currentLetter, position)
            if (!result.contains(currentLetter)) result.add(currentLetter)
        }

        return result.joinToString("")
    }

    private fun ciphered(
        string: String,
        modifier: Int,
    ): String =
        buildString {
            string.toByteArray(Charsets.UTF_8).forEach { byte ->
                val shiftedValue = (byte.toInt() and BYTE_MASK) + modifier
                val wrapAroundBy =
                    when {
                        shiftedValue > CIPHER_LOWERCASE_A + CIPHER_ALPHABET_LENGTH - 1 -> -CIPHER_ALPHABET_LENGTH
                        shiftedValue < CIPHER_LOWERCASE_A -> CIPHER_ALPHABET_LENGTH
                        else -> 0
                    }
                append((shiftedValue + wrapAroundBy).toChar())
            }
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
    private const val THREE_LETTER_ID_LENGTH = 3
    private const val PROJECT_ID_LENGTH = 8
    private const val PROJECT_ID_DEFAULT_LETTER_POSITION = 13
    private const val CIPHER_LOWERCASE_A = 97
    private const val CIPHER_ALPHABET_LENGTH = 26
    private const val BYTE_MASK = 0xFF
}
