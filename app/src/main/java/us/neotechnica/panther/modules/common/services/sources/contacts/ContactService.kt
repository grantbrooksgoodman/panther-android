//
//  ContactService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services.sources.contacts

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import us.neotechnica.panther.bundle.contactPairArchiveService
import us.neotechnica.panther.bundle.contacts
import us.neotechnica.panther.bundle.conversationCellViewData
import us.neotechnica.panther.bundle.emptyContactList
import us.neotechnica.panther.bundle.queriedContactPairs
import us.neotechnica.panther.modules.common.dependencies.contactNameService
import us.neotechnica.panther.modules.common.extensions.ContactPairArchiveServiceStorageKey
import us.neotechnica.panther.modules.common.extensions.codePointOrder
import us.neotechnica.panther.modules.common.models.Contact
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.common.models.DeviceContact
import us.neotechnica.panther.modules.common.models.NumberPair
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.common.services.sources.PermissionService
import us.neotechnica.panther.modules.content.user.extensions.displayName
import us.neotechnica.panther.modules.content.user.extensions.uniquedByPhoneNumber
import us.neotechnica.panther.modules.content.user.extensions.userIDs
import us.neotechnica.panther.modules.content.user.extensions.withUser
import us.neotechnica.panther.modules.networking.sources.user.models.User
import us.neotechnica.panther.modules.networking.sources.user.services.UserService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.foundation.models.AppException
import us.neotechnica.panther.subsystem.modules.foundation.models.CacheDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.models.SingleSlotCoalescer
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

/**
 * Use [ContactService] to match the user's device contacts with
 * registered users.
 *
 * The service queries the device's address book for contacts
 * whose phone numbers belong to registered users, maintains the
 * contact pair archive with the results, and caches the fetched
 * contacts in memory.
 */
object ContactService {
    // MARK: - Dependencies

    private val contactNameService: ContactNameService by Dependency { it.contactNameService }
    private val permissionService: PermissionService by Dependency { it.commonServices.permission }

    // MARK: - Properties

    /**
     * The service that persists and queries the contact pair
     * archive.
     */
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
     * Rebuilds the contact pair archive by matching every
     * registered user against the device's contacts.
     *
     * This method fetches all registered users, queries the
     * device's address book for contacts matching their phone
     * numbers, and replaces the archive's contents with the
     * results. Users without a matching device contact are
     * persisted separately to the unknown contact pair archive.
     * Related caches are cleared before the archive is
     * repopulated.
     *
     * If no device contact matches any registered user, this
     * method returns without modifying the archive.
     *
     * Concurrent calls coalesce onto a single in-flight sync.
     *
     * @throws Exception if contact permission has not been
     *   granted, or if fetching users or contacts fails.
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
     * Returns the lookup URI of the device contact matching the
     * given compiled number string, suitable for a system
     * contact-view intent, or `null` when no device contact
     * matches or contact permission has not been granted.
     *
     * @param compiledNumberString The compiled number string to
     *   match.
     */
    fun deviceContactLookupUri(compiledNumberString: String): Uri? {
        val resolver = appContext?.contentResolver ?: return null
        if (permissionService.contactPermissionStatus != PermissionService.PermissionStatus.GRANTED) return null

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

    // MARK: - Auxiliary

    private suspend fun syncContactPairArchiveInternal() {
        try {
            val users = UserService.getAllUsers()
            val contactPairs = fetchContactPairs(users)

            CoreUtilities.clearCaches(
                listOf(
                    CacheDomain.conversationCellViewData,
                    CacheDomain.queriedContactPairs,
                ),
            )

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
        val context = appContext
        if (permissionService.contactPermissionStatus != PermissionService.PermissionStatus.GRANTED || context == null) {
            throw Exception("Not authorized for contacts.", isReportable = false, metadata = ExceptionMetadata(this))
        }

        val deviceContacts = DeviceContactReader.read(context)
        val contactPairs = contactPairs(users, deviceContacts)

        val matchedDeviceContacts = deviceContacts.filter { deviceContact -> contactPairs.any { it.contact.id == deviceContact.id } }
        cachedDeviceContacts.wrappedValue = ((cachedDeviceContacts.wrappedValue ?: emptyList()) + matchedDeviceContacts).distinct()

        if (contactPairs.isEmpty()) {
            throw Exception("Empty contact list.", isReportable = false, metadata = ExceptionMetadata(this))
        }

        return contactPairs
    }

    /**
     * Pairs every device contact whose numbers match a registered
     * user with the users it matches.
     *
     * Each matching user contributes a number pair; a contact
     * matched by several users carries their pairs de-duplicated
     * and sorted by calling code. The result is unique, sorted by
     * first name, and de-duplicated by phone number.
     *
     * @param users The registered users to match.
     * @param deviceContacts The device contacts to match against.
     *
     * @return The matched contact pairs.
     */
    internal fun contactPairs(
        users: List<User>,
        deviceContacts: List<DeviceContact>,
    ): List<ContactPair> {
        // Users are visited in order, and a contact several users
        // match keeps its first position with their pairs merged.
        val contactPairs = mutableListOf<ContactPair>()
        for (user in users) {
            for (deviceContact in deviceContacts.filter { it.matches(user.phoneNumber) }) {
                val (firstName, lastName) = contactNameService.name(deviceContact)
                val contactPair =
                    ContactPair(
                        contact =
                            Contact(
                                id = deviceContact.id,
                                firstName = firstName,
                                lastName = lastName,
                                phoneNumbers = deviceContact.phoneNumbers,
                                imageData = deviceContact.imageData,
                            ),
                        numberPairs = listOf(NumberPair(phoneNumber = user.phoneNumber, userIDs = listOf(user.id))),
                    )

                val existingIndex = contactPairs.indexOfFirst { it.contact == contactPair.contact }
                if (existingIndex < 0) {
                    contactPairs.add(contactPair)
                    continue
                }

                val existingPair = contactPairs[existingIndex]
                contactPairs[existingIndex] =
                    ContactPair(
                        contact = contactPair.contact,
                        numberPairs =
                            (existingPair.numberPairs + contactPair.numberPairs)
                                .distinct()
                                .sortedBy { it.phoneNumber.callingCode },
                    )
            }
        }

        return contactPairs
            .distinct()
            .sortedWith(compareBy(codePointOrder) { it.contact.firstName })
            .uniquedByPhoneNumber
    }
}
