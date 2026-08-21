//
//  ContactService.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.contacts.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import us.neotechnica.panther.modules.common.contacts.models.ContactMatch
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.user.services.UserService
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

/**
 * Matches the device's contacts with registered users, persisting the
 * results so conversation titles and the contact selector can show names
 * instead of phone numbers.
 */
object ContactService {
    // MARK: - Properties

    @Volatile
    private var appContext: Context? = null

    private val matchesRef = LockIsolated(emptyList<ContactMatch>())

    @Volatile
    private var didLoad = false

    // MARK: - Initialization

    /** Prepares the service with the application context. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    // MARK: - Access

    /** The matched contacts, loaded from the archive on first access. */
    fun matches(): List<ContactMatch> {
        loadIfNeeded()
        return matchesRef.wrappedValue
    }

    /** Returns the contact matched to the given user, or `null`. */
    fun match(userID: String): ContactMatch? = matches().firstOrNull { it.userID == userID }

    // MARK: - Sync

    /**
     * Rebuilds the contact archive when it is empty and contact
     * permission is granted. Concurrent syncs are not coalesced; callers
     * invoke this at boot.
     */
    suspend fun syncIfNeeded() {
        loadIfNeeded()
        if (matchesRef.wrappedValue.isNotEmpty()) return
        if (!hasContactPermission()) return
        sync()
    }

    /** Rebuilds the contact archive by matching device contacts to users. */
    suspend fun sync() {
        if (!hasContactPermission()) return

        val users = runCatching { UserService.getAllUsers() }.getOrNull() ?: return
        val deviceContacts = queryDeviceContacts()
        if (deviceContacts.isEmpty()) return

        val matches = mutableListOf<ContactMatch>()
        for (user in users) {
            val name = matchName(user, deviceContacts) ?: continue
            matches.add(ContactMatch(user.id, name, user.phoneNumber.compiledNumberString))
        }

        matchesRef.wrappedValue = matches
        persist(matches)
        Logger.log("Updated contact archive (${matches.size} matches).")
    }

    // MARK: - Auxiliary

    private fun matchName(
        user: User,
        deviceContacts: List<Pair<String, String>>,
    ): String? {
        val compiled = user.phoneNumber.compiledNumberString
        val national = user.phoneNumber.nationalNumberString
        return deviceContacts
            .firstOrNull { (_, digits) ->
                digits == compiled || digits == national || digits.endsWith(compiled) || digits.endsWith(national)
            }?.first
    }

    private fun queryDeviceContacts(): List<Pair<String, String>> {
        val resolver = appContext?.contentResolver ?: return emptyList()
        val projection =
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            )

        val results = mutableListOf<Pair<String, String>>()
        resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            if (nameIndex < 0 || numberIndex < 0) return emptyList()

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex) ?: continue
                val digits = (cursor.getString(numberIndex) ?: "").filter { it.isDigit() }
                if (digits.isNotEmpty()) results.add(name to digits)
            }
        }
        return results
    }

    private fun hasContactPermission(): Boolean {
        val context = appContext ?: return false
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun loadIfNeeded() {
        if (didLoad) return
        synchronized(this) {
            if (didLoad) return
            didLoad = true
            val archive = Persistent.string(PersistentStorageKey.contactArchive) ?: return
            matchesRef.wrappedValue = runCatching { decode(archive) }.getOrDefault(emptyList())
        }
    }

    private fun persist(matches: List<ContactMatch>) {
        val array = JSONArray()
        for (match in matches) {
            array.put(
                JSONObject()
                    .put(KEY_USER_ID, match.userID)
                    .put(KEY_FULL_NAME, match.fullName)
                    .put(KEY_NUMBER, match.compiledNumberString),
            )
        }
        Persistent.setString(PersistentStorageKey.contactArchive, array.toString())
    }

    private fun decode(archive: String): List<ContactMatch> {
        val array = JSONArray(archive)
        return (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            ContactMatch(obj.getString(KEY_USER_ID), obj.getString(KEY_FULL_NAME), obj.getString(KEY_NUMBER))
        }
    }

    // MARK: - Companion

    private const val KEY_USER_ID = "userID"
    private const val KEY_FULL_NAME = "fullName"
    private const val KEY_NUMBER = "number"
}
