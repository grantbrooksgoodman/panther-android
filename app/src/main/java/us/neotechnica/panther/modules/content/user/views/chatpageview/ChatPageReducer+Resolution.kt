//
//  ChatPageReducer+Resolution.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatpageview

import us.neotechnica.panther.modules.networking.message.models.AudioMessageReference
import us.neotechnica.panther.modules.networking.message.models.MediaFile
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.session.entity.extensions.cachedMediaFile
import us.neotechnica.panther.modules.session.entity.extensions.cachedTranslation
import us.neotechnica.panther.modules.session.entity.extensions.isAudioMessage
import us.neotechnica.panther.modules.session.entity.extensions.isMediaMessage
import us.neotechnica.panther.modules.session.entity.extensions.resolvedAudioReference
import us.neotechnica.panther.modules.session.entity.extensions.resolvedMediaFile
import us.neotechnica.panther.modules.session.entity.extensions.resolvedTranslation
import us.neotechnica.panther.translator.models.Translation

internal fun seedTranslations(
    messages: List<Message>,
    languageCode: String,
    existing: Map<String, Translation>,
): Map<String, Translation> {
    val seeded = mutableMapOf<String, Translation>()
    for (message in messages) {
        if (message.id in existing) continue
        message.cachedTranslation(languageCode)?.let { seeded[message.id] = it }
    }
    return seeded
}

internal fun seedMedia(
    messages: List<Message>,
    existingMedia: Map<String, MediaFile>,
): Map<String, MediaFile> {
    val seeded = mutableMapOf<String, MediaFile>()
    for (message in messages) {
        if (!message.isMediaMessage || message.id in existingMedia) continue
        message.cachedMediaFile?.let { seeded[message.id] = it }
    }
    return seeded
}

internal suspend fun resolveTranslations(
    messages: List<Message>,
    languageCode: String,
    existing: Map<String, Translation>,
): Map<String, Translation> {
    val resolved = mutableMapOf<String, Translation>()
    for (message in messages) {
        if (message.id in existing) continue
        message.resolvedTranslation(languageCode)?.let { resolved[message.id] = it }
    }
    return resolved
}

internal suspend fun resolveMedia(
    messages: List<Message>,
    existingMedia: Map<String, MediaFile>,
): Map<String, MediaFile> {
    val resolved = mutableMapOf<String, MediaFile>()
    for (message in messages) {
        if (!message.isMediaMessage || message.id in existingMedia) continue
        message.resolvedMediaFile()?.let { resolved[message.id] = it }
    }
    return resolved
}

internal suspend fun resolveAudio(
    messages: List<Message>,
    languageCode: String,
    existingAudio: Map<String, AudioMessageReference>,
): Map<String, AudioMessageReference> {
    val resolved = mutableMapOf<String, AudioMessageReference>()
    for (message in messages) {
        if (!message.isAudioMessage || message.id in existingAudio) continue
        message.resolvedAudioReference(languageCode)?.let { resolved[message.id] = it }
    }
    return resolved
}
