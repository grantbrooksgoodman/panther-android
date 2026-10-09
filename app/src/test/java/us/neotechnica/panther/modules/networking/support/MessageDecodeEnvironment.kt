//
//  MessageDecodeEnvironment.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.support

import us.neotechnica.panther.bundle.currentUserID
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.FileStore
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.translator.services.LocalTranslationArchiver
import java.io.File

/**
 * The environment a message decode needs: an isolated file store, the
 * fake networking delegates, an empty local translation archive, and
 * an optional current user.
 */
class MessageDecodeEnvironment {
    // MARK: - Properties

    val database = FakeDatabaseDelegate()
    val hostedTranslation = FakeHostedTranslationDelegate()
    val storage = FakeStorageDelegate()

    lateinit var directory: File
        private set

    // MARK: - Methods

    /** Installs the environment, replacing any previous state. */
    fun install(name: String) {
        directory = File(System.getProperty("java.io.tmpdir"), "$name-${System.nanoTime()}")
        directory.mkdirs()
        FileStore.initializeForTesting(directory)
        Persistent.initializeForTesting()
        LocalTranslationArchiver.clearArchive()
        SessionStore.reloadForTesting()

        Networking.config.register(
            databaseDelegate = database,
            hostedTranslationDelegate = hostedTranslation,
            storageDelegate = storage,
        )
    }

    /** Makes [user] the current user, upserting it into the session store. */
    fun signIn(user: User) {
        Persistent.setString(PersistentStorageKey.currentUserID, user.id)
        SessionStore.upsertUser(user)
    }

    /** Writes a non-empty placeholder file at [relativePath] in the file store. */
    fun writeFile(relativePath: String): File = checkNotNull(FileStore.write(relativePath, byteArrayOf(1, 2, 3)))

    // MARK: - Companion

    companion object {
        /** A user with the given identifier and language codes. */
        fun user(
            id: String,
            languageCode: String,
            previousLanguageCodes: List<String>? = null,
        ): User =
            User(
                id = id,
                aiEnhancedTranslationsEnabled = false,
                blockedUserIDs = null,
                conversationIDs = null,
                deviceID = "device",
                isPenPalsParticipant = false,
                languageCode = languageCode,
                messageRecipientConsentRequired = false,
                phoneNumber = PhoneNumber("5551234567"),
                previousLanguageCodes = previousLanguageCodes,
                pushTokens = null,
            )
    }
}
