//
//  HostedTranslationArchiverTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.database.interfaces.DatabaseDelegate
import us.neotechnica.panther.networking.modules.database.models.QueryStrategy
import us.neotechnica.panther.networking.modules.translation.services.HostedTranslationArchiver
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.TranslationInput
import kotlin.time.Duration

class HostedTranslationArchiverTest {
    // MARK: - Types

    // Serves an empty translations node and counts every read.
    private class EmptyArchiveDatabase : DatabaseDelegate {
        val readPaths = mutableListOf<String>()

        override suspend fun awaitRealtimeConnection(timeout: Duration): Boolean = true

        override fun generateKey(path: String): String? = null

        override suspend fun getValues(
            path: String,
            prependingEnvironment: Boolean,
            cacheStrategy: CacheStrategy,
            timeout: Duration,
        ): Any {
            readPaths.add(path)
            return emptyMap<String, Any>()
        }

        override suspend fun increment(
            path: String,
            delta: Int,
            prependingEnvironment: Boolean,
            timeout: Duration,
        ) = Unit

        override fun isEncodable(value: Any?): Boolean = true

        override fun observe(
            path: String,
            prependingEnvironment: Boolean,
        ): Flow<Any> = emptyFlow()

        override fun prewarm() = Unit

        override suspend fun queryValues(
            path: String,
            strategy: QueryStrategy,
            prependingEnvironment: Boolean,
            cacheStrategy: CacheStrategy,
            timeout: Duration,
        ): Any = emptyMap<String, Any>()

        override suspend fun runTransaction(
            path: String,
            prependingEnvironment: Boolean,
            timeout: Duration,
            block: (Any?) -> Any?,
        ): Any? = null

        override fun setGlobalCacheStrategy(globalCacheStrategy: CacheStrategy?) = Unit

        override suspend fun setValue(
            value: Any?,
            key: String,
            prependingEnvironment: Boolean,
            timeout: Duration,
        ) = Unit

        override suspend fun updateChildValues(
            key: String,
            data: Map<String, Any?>,
            prependingEnvironment: Boolean,
            timeout: Duration,
        ) = Unit
    }

    // MARK: - Tests

    @Test
    fun `an empty but fresh snapshot answers lookups without a network read`() =
        runTest {
            val database = EmptyArchiveDatabase()
            Networking.config.register(databaseDelegate = database)
            val archiver = HostedTranslationArchiver()

            archiver.refreshTranslationDataSnapshot()
            val readsAfterSnapshot = database.readPaths.size

            val failure =
                runCatching {
                    archiver.findArchivedTranslation(TranslationInput("Hello"), LanguagePair(from = "en", to = "es"))
                }.exceptionOrNull()

            assertTrue(failure is Exception)
            assertEquals(1, readsAfterSnapshot)
            assertEquals(readsAfterSnapshot, database.readPaths.size)
        }
}
