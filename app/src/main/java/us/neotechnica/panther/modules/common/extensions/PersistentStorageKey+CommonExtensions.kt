//
//  PersistentStorageKey+CommonExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 25/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.extensions

/** The persistent storage keys scoped to the application. */
enum class ApplicationStorageKey(
    val rawValue: String,
) {
    BUILD_MILESTONE_STRING("buildMilestoneString"),
    HAS_RUN_ONCE("hasRunOnce"),
    IS_IN_STAGING_MODE("isInStagingMode"),
}

/** The persistent storage keys scoped to the breadcrumbs capture service. */
enum class BreadcrumbsCaptureServiceStorageKey(
    val rawValue: String,
) {
    BREADCRUMBS_CAPTURE_FREQUENCY("breadcrumbsCaptureFrequency"),
    BREADCRUMBS_CAPTURE_HISTORY("breadcrumbsCaptureHistory"),
}

/** The persistent storage keys scoped to the contact pair archive service. */
enum class ContactPairArchiveServiceStorageKey(
    val rawValue: String,
) {
    CONTACT_PAIR_ARCHIVE("contactPairArchive"),
    LAST_CONTACT_SYNC_DATE("lastContactSyncDate"),
    UNKNOWN_CONTACT_PAIR_ARCHIVE("unknownContactPairArchive"),
}

/** The persistent storage keys scoped to the metadata service. */
enum class MetadataServiceStorageKey(
    val rawValue: String,
) {
    APP_SHARE_LINK("appShareLink"),
    APP_STORE_BUILD_NUMBER("appStoreBuildNumber"),
    GEMINI_API_KEY("geminiAPIKey"),
    IS_PREVARICATION_MODE_ENABLED("isPrevaricationModeEnabled"),
    REDIRECTION_KEY("redirectionKey"),
    SHOULD_FORCE_UPDATE("shouldForceUpdate"),
    STORAGE_REFERENCE_URL("storageReferenceURL"),
}

/** The persistent storage keys scoped to the review service. */
enum class ReviewServiceStorageKey(
    val rawValue: String,
) {
    APP_OPEN_COUNT("appOpenCount"),
    LAST_REQUESTED_REVIEW_FOR_BUILD_NUMBER("lastRequestedReviewForBuildNumber"),
}

/** The persistent storage keys scoped to the update service. */
enum class UpdateServiceStorageKey(
    val rawValue: String,
) {
    BUILD_NUMBER_WHEN_LAST_FORCED_TO_UPDATE("buildNumberWhenLastForcedToUpdate"),
    FIRST_POSTPONED_UPDATE("firstPostponedUpdate"),
    RELAUNCHES_SINCE_LAST_POSTPONED_UPDATE("relaunchesSinceLastPostponedUpdate"),
}
