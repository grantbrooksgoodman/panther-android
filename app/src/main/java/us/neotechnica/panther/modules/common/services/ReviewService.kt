//
//  ReviewService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import com.google.android.play.core.review.ReviewManagerFactory
import us.neotechnica.panther.bundle.reviewService
import us.neotechnica.panther.modules.common.extensions.ReviewServiceStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.translator.Translator

/**
 * Requests Play Store reviews at appropriate moments.
 *
 * Review prompts are limited to at most one per build, on
 * qualifying app launches.
 */
object ReviewService {
    // MARK: - Computed Properties

    /** The persisted count of app launches. */
    val appOpenCount: Int
        get() = Persistent.int(scopedKey(ReviewServiceStorageKey.APP_OPEN_COUNT)) ?: 0

    private val canPromptToReview: Boolean
        get() {
            val appOpenCount = Persistent.int(scopedKey(ReviewServiceStorageKey.APP_OPEN_COUNT))
            if (lastRequestedReviewForBuildNumber == Build.buildNumber) return false
            return appOpenCount == REVIEW_PROMPT_FIRST_THRESHOLD ||
                appOpenCount == REVIEW_PROMPT_SECOND_THRESHOLD ||
                (appOpenCount ?: 0) % REVIEW_PROMPT_RECURRING_INTERVAL == 0
        }

    private val lastRequestedReviewForBuildNumber: Int
        get() {
            val defaultsValue = Persistent.int(scopedKey(ReviewServiceStorageKey.LAST_REQUESTED_REVIEW_FOR_BUILD_NUMBER))
            if (defaultsValue == null) {
                val buildNumber = Build.buildNumber - 1
                Persistent.setInt(
                    scopedKey(ReviewServiceStorageKey.LAST_REQUESTED_REVIEW_FOR_BUILD_NUMBER),
                    if (buildNumber < 0) 0 else buildNumber,
                )
                return buildNumber
            }

            return defaultsValue
        }

    // MARK: - Methods

    /** Increments the persisted count of app launches. */
    fun incrementAppOpenCount() {
        Persistent.setInt(
            scopedKey(ReviewServiceStorageKey.APP_OPEN_COUNT),
            (Persistent.int(scopedKey(ReviewServiceStorageKey.APP_OPEN_COUNT)) ?: 0) + 1,
        )
    }

    /**
     * Requests a Play Store review if the necessary conditions are
     * met.
     *
     * A review may be requested once per build, when the app launch
     * count reaches a qualifying value. If a review was already
     * requested for the current build, or the launch count does not
     * qualify, this method does nothing.
     */
    fun promptToReview() {
        if (!canPromptToReview) return
        val activity = Translator.config.currentActivityProvider?.invoke() ?: return

        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) {
                manager.launchReviewFlow(activity, request.result)
            }
        }

        Persistent.setInt(
            scopedKey(ReviewServiceStorageKey.LAST_REQUESTED_REVIEW_FOR_BUILD_NUMBER),
            Build.buildNumber,
        )
    }

    // MARK: - Auxiliary

    private fun scopedKey(key: ReviewServiceStorageKey): PersistentStorageKey =
        PersistentStorageKey.reviewService(key)
}

private const val REVIEW_PROMPT_FIRST_THRESHOLD = 10
private const val REVIEW_PROMPT_RECURRING_INTERVAL = 100
private const val REVIEW_PROMPT_SECOND_THRESHOLD = 50
