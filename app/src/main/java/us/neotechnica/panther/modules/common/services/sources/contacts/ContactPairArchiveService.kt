//
//  ContactPairArchiveService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services.sources.contacts

import org.json.JSONObject
import us.neotechnica.panther.bundle.contactImage
import us.neotechnica.panther.bundle.contactPairArchiveService
import us.neotechnica.panther.bundle.contacts
import us.neotechnica.panther.bundle.updatedContactPairArchive
import us.neotechnica.panther.modules.common.extensions.ContactPairArchiveServiceStorageKey
import us.neotechnica.panther.modules.common.extensions.compiledNumberStrings
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.common.services.sources.PhoneNumberService
import us.neotechnica.panther.notificationextension.NotificationExtensionConstants
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.CacheDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import java.util.Date

/**
 * Persists and queries the archive of contact pairs known to the app.
 *
 * The archive associates the user's device contacts with the users
 * registered under their phone numbers. It is cached in memory and
 * persisted across launches.
 */
object ContactPairArchiveService {
    // MARK: - Dependencies

    private val phoneNumberService: PhoneNumberService by Dependency { it.commonServices.phoneNumber }

    // MARK: - Properties

    private val cachedArchive = LockIsolated<List<ContactPair>?>(null)
    private val cachedContactPairsForPhoneNumbers = LockIsolated<Map<String, ContactPair>?>(null)

    // MARK: - Computed Properties

    /** The date of the last completed contact sync, or `null` if none. */
    var lastContactSyncDate: Date?
        get() = Persistent.long(key(ContactPairArchiveServiceStorageKey.LAST_CONTACT_SYNC_DATE))?.let { Date(it) }
        set(value) {
            Persistent.setLong(
                key(ContactPairArchiveServiceStorageKey.LAST_CONTACT_SYNC_DATE),
                value?.time,
            )
        }

    private var archive: List<ContactPair>
        get() = cachedArchive.wrappedValue ?: loadPersistedArchive() ?: emptyList()
        set(newValue) {
            cachedArchive.wrappedValue = newValue
            persist(newValue)
            persistValuesForNotificationExtension()
        }

    // MARK: - Init

    init {
        persistValuesForNotificationExtension()
    }

    // MARK: - Addition

    /**
     * Adds the given contact pairs to the archive.
     *
     * Pairs already present in the archive are skipped; otherwise, any
     * existing pair for the same contact identifier is replaced. Adding
     * values clears the contact image cache and notifies observers that
     * the archive changed.
     *
     * @param contactPairs The contact pairs to add.
     */
    fun addValues(contactPairs: List<ContactPair>) {
        val values = archive.toMutableList()

        for (contactPair in contactPairs) {
            if (values.contains(contactPair)) continue
            values.removeAll { it.contact.id == contactPair.contact.id }
            values.add(contactPair)

            Logger.log(
                Exception(
                    "Added contact pair to persisted archive.",
                    isReportable = false,
                    userInfo =
                        mapOf(
                            "FullName" to contactPair.contact.fullName,
                            "PhoneNumbers" to
                                contactPair.numberPairs
                                    .map { it.phoneNumber.formattedString() }
                                    .toString(),
                        ),
                    metadata = ExceptionMetadata(this),
                ),
                domain = LoggerDomain.contacts,
            )
        }

        archive = values
        cachedContactPairsForPhoneNumbers.wrappedValue =
            cachedContactPairsForPhoneNumbers.wrappedValue?.filterValues { !contactPairs.contains(it) }

        CoreUtilities.clearCaches(listOf(CacheDomain.contactImage))
        DependencyValues.current.sharedEvents.updatedContactPairArchive
            .send(Unit)
    }

    // MARK: - Removal

    /**
     * Removes every contact pair from the archive.
     *
     * Clearing the archive also clears the contact image cache.
     * Observers are not notified.
     */
    fun clearArchive() {
        archive = emptyList()
        cachedContactPairsForPhoneNumbers.wrappedValue = null
        CoreUtilities.clearCaches(listOf(CacheDomain.contactImage))
    }

    // MARK: - Retrieval

    /**
     * Returns the archived contact pair for the given phone number.
     *
     * Lookups match against each pair's compiled number strings and are
     * memoized per phone number.
     *
     * @param phoneNumber The phone number for which to retrieve a
     *   contact pair.
     *
     * @return The contact pair whose numbers include the given phone
     *   number; otherwise, `null` if no match exists.
     */
    fun getValue(phoneNumber: PhoneNumber): ContactPair? {
        cachedContactPairsForPhoneNumbers.wrappedValue?.get(phoneNumber.compiledNumberString)?.let { return it }

        val valueForPhoneNumber =
            archive.firstOrNull { contactPair ->
                contactPair.contact.phoneNumbers.any { it.compiledNumberString == phoneNumber.compiledNumberString }
            } ?: return null

        val newCacheValue = (cachedContactPairsForPhoneNumbers.wrappedValue ?: emptyMap()).toMutableMap()
        newCacheValue[phoneNumber.compiledNumberString] = valueForPhoneNumber
        cachedContactPairsForPhoneNumbers.wrappedValue = newCacheValue

        return valueForPhoneNumber
    }

    /** The full archive of contact pairs. */
    fun allValues(): List<ContactPair> = archive

    // MARK: - Auxiliary

    private fun key(storageKey: ContactPairArchiveServiceStorageKey): PersistentStorageKey =
        PersistentStorageKey.contactPairArchiveService(storageKey)

    private fun loadPersistedArchive(): List<ContactPair>? =
        Persistent.archive(key(ContactPairArchiveServiceStorageKey.CONTACT_PAIR_ARCHIVE)) { maps ->
            maps.mapNotNull { ContactPair.decode(it) }
        }

    private fun persist(values: List<ContactPair>) {
        Persistent.setArchive(
            key(ContactPairArchiveServiceStorageKey.CONTACT_PAIR_ARCHIVE),
            values.map { it.encoded },
        )
    }

    private fun persistValuesForNotificationExtension() {
        val json = JSONObject()
        for (contactPair in archive) {
            val phoneNumbers = contactPair.contact.phoneNumbers
            val numberStrings = phoneNumbers.compiledNumberStrings.distinct()
            val possibleHashes = phoneNumberService.possibleHashes(numberStrings) ?: emptyList()
            for (hash in possibleHashes) {
                json.put(hash, contactPair.contact.fullName)
            }
        }

        Persistent.setString(
            PersistentStorageKey(NotificationExtensionConstants.CONTACT_ARCHIVE_DEFAULTS_KEY_NAME),
            json.toString(),
        )
    }
}
