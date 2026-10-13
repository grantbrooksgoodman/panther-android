//
//  GestureRecognizerState.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.models

/**
 * The states through which a continuous gesture, such as a long press,
 * passes.
 *
 * A gesture begins once it is recognized, changes as its pointer
 * moves, and finishes by either ending, when the pointer lifts, or
 * being cancelled.
 */
enum class GestureRecognizerState {
    /** The gesture was recognized. */
    BEGAN,

    /** The gesture's pointer moved. */
    CHANGED,

    /** The gesture's pointer lifted. */
    ENDED,

    /** The gesture was cancelled before its pointer lifted. */
    CANCELLED,
}
