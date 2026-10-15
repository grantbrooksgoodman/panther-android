//
//  AppConstants+MessageSessionService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.constants

// MARK: - Float

/** The values of the message session service. */
object MessageSessionServiceFloats {
    /** The delivery progress added when the message is added to the conversation. */
    const val ADD_MESSAGE_DELIVERY_PROGRESS_INCREMENT = 0.2f

    /** The delivery progress added when the conversation is created. */
    @Suppress("VariableMaxLength")
    const val CREATE_CONVERSATION_DELIVERY_PROGRESS_INCREMENT = 0.2f

    /** The delivery progress added when the message is created. */
    @Suppress("VariableMaxLength")
    const val CREATE_MESSAGE_DELIVERY_PROGRESS_INCREMENT = 0.2f

    /** The confidence above which language recognition counts as a match. */
    @Suppress("VariableMaxLength")
    const val LANGUAGE_RECOGNITION_SERVICE_MATCH_CONFIDENCE_THRESHOLD = 0.8f

    /** The delivery progress added when the recipients are notified. */
    const val NOTIFY_DELIVERY_PROGRESS_INCREMENT = 0.2f

    /** The delivery progress added when the content is read to a file. */
    const val READ_TO_FILE_DELIVERY_PROGRESS_INCREMENT = 0.05f

    /** The delivery progress added when the message is translated. */
    const val TRANSLATION_DELIVERY_PROGRESS_INCREMENT = 0.05f

    /** The delivery progress added when the hosted values are updated. */
    const val UPDATE_VALUE_DELIVERY_PROGRESS_INCREMENT = 0.2f
}
