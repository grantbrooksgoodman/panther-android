//
//  DeviceContactReader.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.contacts.services

import android.content.ContentResolver
import android.database.Cursor
import android.provider.ContactsContract
import us.neotechnica.panther.modules.common.contacts.models.DeviceContact
import us.neotechnica.panther.modules.common.models.PhoneNumber

/**
 * Reads the device's address book, grouping its records by contact
 * identifier.
 *
 * Collects phone numbers, structured names, nicknames, and
 * organization names, and assembles a [DeviceContact] for every
 * contact that has at least one phone number.
 */
internal object DeviceContactReader {
    // MARK: - Types

    private data class NameFields(
        val givenName: String = "",
        val familyName: String = "",
        val phoneticGivenName: String = "",
        val phoneticFamilyName: String = "",
    )

    private data class OrganizationFields(
        val organizationName: String = "",
        val phoneticOrganizationName: String = "",
    )

    // MARK: - Read

    /** Returns every device contact that has at least one phone number. */
    fun read(resolver: ContentResolver): List<DeviceContact> {
        val phoneNumbersForContactIDs = readPhoneNumbers(resolver)
        if (phoneNumbersForContactIDs.isEmpty()) return emptyList()

        val nameFieldsForContactIDs = readNameFields(resolver)
        val nicknamesForContactIDs = readNicknames(resolver)
        val organizationFieldsForContactIDs = readOrganizationFields(resolver)

        return phoneNumbersForContactIDs.map { (contactID, phoneNumbers) ->
            val nameFields = nameFieldsForContactIDs[contactID] ?: NameFields()
            val organizationFields = organizationFieldsForContactIDs[contactID] ?: OrganizationFields()
            DeviceContact(
                id = contactID,
                givenName = nameFields.givenName,
                familyName = nameFields.familyName,
                phoneticGivenName = nameFields.phoneticGivenName,
                phoneticFamilyName = nameFields.phoneticFamilyName,
                nickname = nicknamesForContactIDs[contactID] ?: "",
                organizationName = organizationFields.organizationName,
                phoneticOrganizationName = organizationFields.phoneticOrganizationName,
                phoneNumbers = phoneNumbers,
            )
        }
    }

    // MARK: - Auxiliary

    private fun readPhoneNumbers(resolver: ContentResolver): Map<String, List<PhoneNumber>> {
        val projection =
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.LABEL,
            )

        val phoneNumbersForContactIDs = linkedMapOf<String, MutableList<PhoneNumber>>()
        query(resolver, ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null) { cursor ->
            val contactID = cursor.string(ContactsContract.CommonDataKinds.Phone.CONTACT_ID) ?: return@query
            val number = cursor.string(ContactsContract.CommonDataKinds.Phone.NUMBER) ?: return@query
            if (number.isBlank()) return@query
            val label = cursor.string(ContactsContract.CommonDataKinds.Phone.LABEL)
            phoneNumbersForContactIDs.getOrPut(contactID) { mutableListOf() }.add(PhoneNumber(number, label))
        }
        return phoneNumbersForContactIDs.mapValues { (_, numbers) -> numbers.distinct() }
    }

    private fun readNameFields(resolver: ContentResolver): Map<String, NameFields> {
        val projection =
            arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME,
                ContactsContract.CommonDataKinds.StructuredName.PHONETIC_FAMILY_NAME,
            )

        val nameFieldsForContactIDs = hashMapOf<String, NameFields>()
        query(resolver, ContactsContract.Data.CONTENT_URI, projection, mimeTypeSelection(StructuredNameMimeType)) { cursor ->
            val contactID = cursor.string(ContactsContract.Data.CONTACT_ID) ?: return@query
            nameFieldsForContactIDs[contactID] =
                NameFields(
                    givenName = cursor.string(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME).orEmpty(),
                    familyName = cursor.string(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME).orEmpty(),
                    phoneticGivenName = cursor.string(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME).orEmpty(),
                    phoneticFamilyName = cursor.string(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_FAMILY_NAME).orEmpty(),
                )
        }
        return nameFieldsForContactIDs
    }

    private fun readNicknames(resolver: ContentResolver): Map<String, String> {
        val projection = arrayOf(ContactsContract.Data.CONTACT_ID, ContactsContract.CommonDataKinds.Nickname.NAME)

        val nicknamesForContactIDs = hashMapOf<String, String>()
        query(resolver, ContactsContract.Data.CONTENT_URI, projection, mimeTypeSelection(NicknameMimeType)) { cursor ->
            val contactID = cursor.string(ContactsContract.Data.CONTACT_ID) ?: return@query
            val nickname = cursor.string(ContactsContract.CommonDataKinds.Nickname.NAME) ?: return@query
            nicknamesForContactIDs[contactID] = nickname
        }
        return nicknamesForContactIDs
    }

    private fun readOrganizationFields(resolver: ContentResolver): Map<String, OrganizationFields> {
        val projection =
            arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.CommonDataKinds.Organization.COMPANY,
                ContactsContract.CommonDataKinds.Organization.PHONETIC_NAME,
            )

        val organizationFieldsForContactIDs = hashMapOf<String, OrganizationFields>()
        query(resolver, ContactsContract.Data.CONTENT_URI, projection, mimeTypeSelection(OrganizationMimeType)) { cursor ->
            val contactID = cursor.string(ContactsContract.Data.CONTACT_ID) ?: return@query
            organizationFieldsForContactIDs[contactID] =
                OrganizationFields(
                    organizationName = cursor.string(ContactsContract.CommonDataKinds.Organization.COMPANY).orEmpty(),
                    phoneticOrganizationName = cursor.string(ContactsContract.CommonDataKinds.Organization.PHONETIC_NAME).orEmpty(),
                )
        }
        return organizationFieldsForContactIDs
    }

    private fun mimeTypeSelection(mimeType: String): Pair<String, Array<String>> =
        "${ContactsContract.Data.MIMETYPE} = ?" to arrayOf(mimeType)

    private inline fun query(
        resolver: ContentResolver,
        uri: android.net.Uri,
        projection: Array<String>,
        selection: Pair<String, Array<String>>?,
        onRow: (Cursor) -> Unit,
    ) {
        resolver
            .query(uri, projection, selection?.first, selection?.second, null)
            ?.use { cursor -> while (cursor.moveToNext()) onRow(cursor) }
    }

    private fun Cursor.string(column: String): String? {
        val index = getColumnIndex(column)
        if (index < 0) return null
        return getString(index)
    }

    // MARK: - Companion

    private val NicknameMimeType = ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE
    private val OrganizationMimeType = ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
    private val StructuredNameMimeType = ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
}
