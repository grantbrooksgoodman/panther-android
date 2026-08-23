//
//  Milestone.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.models

/**
 * The release-cycle stage of a build.
 *
 * Mirrors the iOS `Build.Milestone`. Each milestone has a single-
 * character [shortString] that is appended to build numbers in the
 * build-info overlay (for example, `"b"` for beta).
 */
enum class Milestone(
    /** The underlying raw value, matching the iOS `rawValue`. */
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
        /** The milestone matching [rawValue], or [ALPHA] if unrecognized. */
        fun from(rawValue: String): Milestone = entries.firstOrNull { it.rawValue == rawValue } ?: ALPHA
    }
}
