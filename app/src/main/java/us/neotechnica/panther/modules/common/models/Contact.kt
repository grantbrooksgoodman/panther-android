//
//  Contact.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.models

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.EncodedHashable
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import java.util.Base64

/**
 * A contact from the user's address book.
 *
 * [Contact] captures the subset of a system contact the app uses –
 * identifier, name, phone numbers, and thumbnail image data. Contact
 * images are decoded once and cached in memory; use
 * [ContactImageCache.clearCache] to release them.
 */
class Contact(
    /** The contact's unique identifier. */
    val id: String,
    /** The contact's first name. */
    val firstName: String,
    /** The contact's last name. */
    val lastName: String,
    /** The contact's phone numbers. */
    val phoneNumbers: List<PhoneNumber>,
    /** The contact's thumbnail image data, or `null` if the contact has no image. */
    val imageData: ByteArray? = null,
) : Serializable<Map<String, Any?>>,
    EncodedHashable {
    // MARK: - Types

    private enum class Keys(
        val rawValue: String,
    ) {
        FIRST_NAME("firstName"),
        ID("id"),
        IMAGE_DATA("imageData"),
        LAST_NAME("lastName"),
        PHONE_NUMBERS("phoneNumbers"),
    }

    // MARK: - Init

    init {
        if (imageData != null) ContactImageCache.setImage(decodeImage(imageData), forContactID = id)
    }

    // MARK: - Computed Properties

    /**
     * The contact's full name, composed from the non-blank components
     * of their first and last names.
     */
    val fullName: String
        get() =
            when {
                firstName.isNotBlank() && lastName.isNotBlank() -> "$firstName $lastName"
                firstName.isNotBlank() -> firstName
                lastName.isNotBlank() -> lastName
                else -> ""
            }

    /**
     * The contact's image, decoded from [imageData] and cached in
     * memory, or `null` if the contact has no image.
     */
    val image: Bitmap?
        get() {
            ContactImageCache.image(forContactID = id)?.let { return it }
            val data = imageData ?: return null
            val image = decodeImage(data) ?: return null
            ContactImageCache.setImage(image, forContactID = id)
            return image
        }

    /** The uppercased first letters of each word of the contact's full name. */
    val initials: String
        get() = fullName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")

    override val encoded: Map<String, Any?>
        get() =
            mapOf(
                Keys.FIRST_NAME.rawValue to firstName,
                Keys.ID.rawValue to id,
                Keys.IMAGE_DATA.rawValue to imageData?.let { Base64.getEncoder().encodeToString(it) },
                Keys.LAST_NAME.rawValue to lastName,
                Keys.PHONE_NUMBERS.rawValue to phoneNumbers.map { it.archived },
            )

    /**
     * The strings that collectively define this instance's identity
     * for hashing purposes, sorted alphabetically.
     *
     * Contains the contact's identifier, name components, phone number
     * hashes, and encoded image data.
     */
    override val hashFactors: List<String>
        get() =
            listOf(
                firstName,
                id,
                lastName,
                phoneNumbers.joinToString("") { it.encodedHash },
                imageData?.let { Base64.getEncoder().encodeToString(it) } ?: "",
            ).sorted()

    // MARK: - Equatable Conformance

    override fun equals(other: Any?): Boolean = other is Contact && hashFactors == other.hashFactors

    override fun hashCode(): Int = hashFactors.hashCode()

    // MARK: - Companion

    companion object {
        /**
         * Reconstructs a contact from its stored encoded map, or `null`
         * if the map is malformed.
         */
        fun decode(data: Map<String, Any?>): Contact? {
            val id = data[Keys.ID.rawValue] as? String ?: return null
            val firstName = data[Keys.FIRST_NAME.rawValue] as? String ?: return null
            val lastName = data[Keys.LAST_NAME.rawValue] as? String ?: return null
            val phoneNumbers =
                (data[Keys.PHONE_NUMBERS.rawValue] as? List<*>)
                    .orEmpty()
                    .mapNotNull { PhoneNumber.decodeSynchronously(it) }
            val imageData = (data[Keys.IMAGE_DATA.rawValue] as? String)?.let { Base64.getDecoder().decode(it) }
            return Contact(
                id = id,
                firstName = firstName,
                lastName = lastName,
                phoneNumbers = phoneNumbers,
                imageData = imageData,
            )
        }

        private fun decodeImage(data: ByteArray): Bitmap? {
            if (data.isEmpty()) return null
            return runCatching { BitmapFactory.decodeByteArray(data, 0, data.size) }.getOrNull()
        }
    }
}

/** Manages the in-memory contact image cache. */
object ContactImageCache {
    // MARK: - Properties

    private val cachedImagesForContactIDs = LockIsolated(mapOf<String, Bitmap>())

    // MARK: - Methods

    /** Removes every cached contact image. */
    fun clearCache() {
        cachedImagesForContactIDs.wrappedValue = emptyMap()
    }

    internal fun image(forContactID: String): Bitmap? = cachedImagesForContactIDs.wrappedValue[forContactID]

    /**
     * Caches the given image for the contact, or evicts the contact's
     * cached image when `null` is passed.
     */
    internal fun setImage(
        image: Bitmap?,
        forContactID: String,
    ) {
        cachedImagesForContactIDs.withValue { reference ->
            reference.value =
                if (image == null) {
                    reference.value - forContactID
                } else {
                    reference.value + (forContactID to image)
                }
        }
    }
}
