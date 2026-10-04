//
//  ContactService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.contacts.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import us.neotechnica.panther.bundle.contactPairArchiveService
import us.neotechnica.panther.modules.common.contacts.models.DeviceContact
import us.neotechnica.panther.modules.common.extensions.ContactPairArchiveServiceStorageKey
import us.neotechnica.panther.modules.common.extensions.emptyContactList
import us.neotechnica.panther.modules.common.models.Contact
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.common.models.NumberPair
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.content.user.extensions.displayName
import us.neotechnica.panther.modules.content.user.extensions.uniquedByPhoneNumber
import us.neotechnica.panther.modules.content.user.extensions.userIDs
import us.neotechnica.panther.modules.content.user.extensions.withUser
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewDataCache
import us.neotechnica.panther.modules.content.user.models.QueriedContactPairCache
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.networking.user.services.UserService
import us.neotechnica.panther.subsystem.modules.foundation.models.AppException
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.models.SingleSlotCoalescer
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

/**
 * Matches the user's device contacts with registered users.
 *
 * Queries the device's address book for contacts whose phone numbers
 * belong to registered users, maintains the contact pair archive with
 * the results, and caches the fetched contacts in memory.
 */
object ContactService {
    // MARK: - Properties

    /** The service that persists and queries the contact pair archive. */
    val contactPairArchive: ContactPairArchiveService
        get() = ContactPairArchiveService

    private val cachedDeviceContacts = LockIsolated<List<DeviceContact>?>(null)
    private val coalescer = SingleSlotCoalescer<Unit>()

    @Volatile
    private var appContext: Context? = null

    // MARK: - Initialization

    /** Prepares the service with the application context. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    // MARK: - Sync Contact Pair Archive

    /**
     * Rebuilds the contact pair archive by matching every registered
     * user against the device's contacts.
     *
     * Fetches all registered users, queries the device's address book
     * for contacts matching their phone numbers, and replaces the
     * archive's contents with the results. Users without a matching
     * device contact are persisted separately to the unknown contact
     * pair archive. Related caches are cleared before the archive is
     * repopulated. If no device contact matches any registered user,
     * this method returns without modifying the archive.
     *
     * Concurrent calls coalesce onto a single in-flight sync.
     *
     * @throws Exception if contact permission has not been granted, or
     *   if fetching users or contacts fails.
     */
    suspend fun syncContactPairArchive() {
        coalescer { syncContactPairArchiveInternal() }
    }

    // MARK: - Clear Cache

    /** Removes every cached device contact. */
    fun clearCache() {
        cachedDeviceContacts.wrappedValue = null
    }

    // MARK: - Device Contact Lookup

    /**
     * Returns the lookup URI of the device contact matching the given
     * compiled number string, suitable for a system contact-view
     * intent, or `null` when no device contact matches or contact
     * permission has not been granted.
     *
     * @param compiledNumberString The compiled number string to match.
     */
    fun deviceContactLookupUri(compiledNumberString: String): Uri? {
        val resolver = appContext?.contentResolver ?: return null
        if (!hasContactPermission()) return null

        val filterUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(compiledNumberString))
        val projection = arrayOf(ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.LOOKUP_KEY)
        resolver.query(filterUri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val idIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
            val keyIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.LOOKUP_KEY)
            if (idIndex < 0 || keyIndex < 0) return null
            val lookupKey = cursor.getString(keyIndex) ?: return null
            return ContactsContract.Contacts.getLookupUri(cursor.getLong(idIndex), lookupKey)
        }
        return null
    }

    /** A Boolean value that indicates whether read access to the device's contacts is held. */
    fun hasContactPermission(): Boolean {
        val context = appContext ?: return false
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
    }

    // MARK: - Auxiliary

    private suspend fun syncContactPairArchiveInternal() {
        try {
            val users = UserService.getAllUsers()
            val contactPairs = fetchContactPairs(users)

            ConversationCellViewDataCache.clearCache()
            QueriedContactPairCache.clearCache()

            contactPairArchive.clearArchive()
            contactPairArchive.addValues(contactPairs)

            val contactPairUserIDs = contactPairs.userIDs
            val unknownContactPairArchive =
                users
                    .filter { it.id !in contactPairUserIDs }
                    .map { ContactPair.withUser(it, name = it.displayName) }
            Persistent.setArchive(
                PersistentStorageKey.contactPairArchiveService(ContactPairArchiveServiceStorageKey.UNKNOWN_CONTACT_PAIR_ARCHIVE),
                unknownContactPairArchive.map { it.encoded },
            )

            Logger.log("Successfully updated contact pair archive.", domain = LoggerDomain.contacts)
        } catch (exception: Exception) {
            if (exception.isEqual(to = AppException.emptyContactList)) return
            throw exception
        }
    }

    private fun fetchContactPairs(users: List<User>): List<ContactPair> {
        val resolver = appContext?.contentResolver
        if (!hasContactPermission() || resolver == null) {
            throw Exception("Not authorized for contacts.", isReportable = false, metadata = ExceptionMetadata(this))
        }

        val matchedDeviceContacts = mutableListOf<DeviceContact>()
        val contactPairs =
            DeviceContactReader.read(resolver).mapNotNull { deviceContact ->
                val numberPairs =
                    users
                        .filter { deviceContact.matches(it.phoneNumber) }
                        .map { NumberPair(phoneNumber = it.phoneNumber, userIDs = listOf(it.id)) }
                        .distinct()
                        .sortedBy { it.phoneNumber.callingCode }
                if (numberPairs.isEmpty()) return@mapNotNull null

                matchedDeviceContacts.add(deviceContact)
                val (firstName, lastName) = ContactNameService.name(deviceContact)
                ContactPair(
                    contact =
                        Contact(
                            id = deviceContact.id,
                            firstName = firstName,
                            lastName = lastName,
                            phoneNumbers = deviceContact.phoneNumbers,
                            imageData = null,
                        ),
                    numberPairs = numberPairs,
                )
            }

        cachedDeviceContacts.wrappedValue = ((cachedDeviceContacts.wrappedValue ?: emptyList()) + matchedDeviceContacts).distinct()

        if (contactPairs.isEmpty()) {
            throw Exception("Empty contact list.", isReportable = false, metadata = ExceptionMetadata(this))
        }

        return contactPairs.distinct().sortedBy { it.contact.firstName }.uniquedByPhoneNumber
    }

    private fun DeviceContact.matches(phoneNumber: PhoneNumber): Boolean {
        val compiledNumberString = phoneNumber.compiledNumberString
        val nationalNumberString = phoneNumber.nationalNumberString
        return phoneNumbers.any { number ->
            val numberCompiledNumberString = number.compiledNumberString
            numberCompiledNumberString == compiledNumberString ||
                (nationalNumberString.isNotEmpty() && number.nationalNumberString == nationalNumberString) ||
                (compiledNumberString.isNotEmpty() && numberCompiledNumberString.endsWith(compiledNumberString)) ||
                (nationalNumberString.isNotEmpty() && numberCompiledNumberString.endsWith(nationalNumberString))
        }
    }
}
