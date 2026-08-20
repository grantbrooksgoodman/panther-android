//
//  NetworkClient.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.neotechnica.panther.translator.models.TranslationError
import java.net.HttpURLConnection
import java.net.URL

/**
 * A minimal HTTP client for the translators' API fast paths.
 *
 * Mirrors the iOS translators' use of `URLSession` for the Google and
 * Reverso JSON endpoints. Requests run on [Dispatchers.IO] and return
 * the response body, throwing [TranslationError] on a non-200 status.
 */
internal object NetworkClient {
    // MARK: - Constants

    private const val STATUS_OK = 200
    private const val DEFAULT_TIMEOUT_MILLIS = 5_000

    // MARK: - Methods

    /** Performs a GET request and returns the response body. */
    suspend fun get(
        urlString: String,
        timeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS,
    ): String =
        withContext(Dispatchers.IO) {
            val connection =
                (URL(urlString).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = timeoutMillis
                    readTimeout = timeoutMillis
                }
            connection.readResponse()
        }

    /** Performs a JSON POST request and returns the response body. */
    suspend fun postJson(
        urlString: String,
        body: String,
        headers: Map<String, String>,
        timeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS,
    ): String =
        withContext(Dispatchers.IO) {
            val connection =
                (URL(urlString).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = timeoutMillis
                    readTimeout = timeoutMillis
                    doOutput = true
                    for ((key, value) in headers) setRequestProperty(key, value)
                }
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            connection.readResponse()
        }

    // MARK: - Auxiliary

    private fun HttpURLConnection.readResponse(): String {
        try {
            if (responseCode != STATUS_OK) {
                throw TranslationError.Unknown("Translation API returned an unexpected response.")
            }
            return inputStream.bufferedReader().use { it.readText() }
        } finally {
            disconnect()
        }
    }
}
