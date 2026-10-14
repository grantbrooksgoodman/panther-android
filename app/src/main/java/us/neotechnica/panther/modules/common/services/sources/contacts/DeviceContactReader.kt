//
//  DeviceContactReader.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services.sources.contacts

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import us.neotechnica.panther.modules.common.models.DeviceContact
import us.neotechnica.panther.modules.common.models.PhoneNumber

/**
 * Reads the device's address book, grouping its records by contact
 * identifier.
 *
 * Collects phone numbers, thumbnail images, structured names,
 * nicknames, and organization names, and assembles a [DeviceContact]
 * for every contact that has at least one phone number.
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

    private class PhoneRecords(
        val phoneNumbers: MutableList<PhoneNumber> = mutableListOf(),
        var photoThumbnailURI: String? = null,
    )

    // MARK: - Read

    /** Returns every device contact that has at least one phone number. */
    fun read(resolver: ContentResolver): List<DeviceContact> {
        val phoneRecordsForContactIDs = readPhoneRecords(resolver)
        if (phoneRecordsForContactIDs.isEmpty()) return emptyList()

        val nameFieldsForContactIDs = readNameFields(resolver)
        val nicknamesForContactIDs = readNicknames(resolver)
        val organizationFieldsForContactIDs = readOrganizationFields(resolver)

        return phoneRecordsForContactIDs.map { (contactID, phoneRecords) ->
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
                phoneNumbers = phoneRecords.phoneNumbers.distinct(),
                imageData = phoneRecords.photoThumbnailURI?.let { readImageData(resolver, it) },
            )
        }
    }

    // MARK: - Auxiliary

    private fun readPhoneRecords(resolver: ContentResolver): Map<String, PhoneRecords> {
        val projection =
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.LABEL,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
            )

        val phoneRecordsForContactIDs = linkedMapOf<String, PhoneRecords>()
        query(resolver, ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null) { cursor ->
            val contactID = cursor.string(ContactsContract.CommonDataKinds.Phone.CONTACT_ID) ?: return@query
            val number = cursor.string(ContactsContract.CommonDataKinds.Phone.NUMBER) ?: return@query
            if (number.isBlank()) return@query
            val label = cursor.string(ContactsContract.CommonDataKinds.Phone.LABEL)
            val records = phoneRecordsForContactIDs.getOrPut(contactID) { PhoneRecords() }
            records.phoneNumbers.add(PhoneNumber(number, label))
            if (records.photoThumbnailURI == null) {
                records.photoThumbnailURI = cursor.string(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
            }
        }
        return phoneRecordsForContactIDs
    }

    private fun readImageData(
        resolver: ContentResolver,
        photoThumbnailURI: String,
    ): ByteArray? =
        runCatching {
            resolver.openInputStream(Uri.parse(photoThumbnailURI))?.use { it.readBytes() }
        }.getOrNull()?.takeIf { it.isNotEmpty() }

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
        uri: Uri,
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
