//
//  BatchFailureStrategy.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.common

/** The strategy that determines how a batch operation responds to a failure. */
enum class BatchFailureStrategy {
    /** Continue processing the remaining items after a failure. */
    CONTINUE_ON_FAILURE,

    /** Stop processing and return as soon as a failure occurs. */
    RETURN_ON_FAILURE,
}
