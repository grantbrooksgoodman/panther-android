//
//  TestUri.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package android.net

import android.os.Parcel
import sun.misc.Unsafe

/**
 * A URI for JVM tests, where the platform stub's constructor throws.
 *
 * Instances are allocated without running a constructor and compare
 * by identity; the components all read as empty.
 */
class TestUri private constructor() : Uri() {
    // MARK: - Uri Conformance

    override fun buildUpon(): Builder = throw UnsupportedOperationException()

    override fun getAuthority(): String? = null

    override fun getEncodedAuthority(): String? = null

    override fun getEncodedFragment(): String? = null

    override fun getEncodedPath(): String? = null

    override fun getEncodedQuery(): String? = null

    override fun getEncodedSchemeSpecificPart(): String? = null

    override fun getEncodedUserInfo(): String? = null

    override fun getFragment(): String? = null

    override fun getHost(): String? = null

    override fun getLastPathSegment(): String? = null

    override fun getPath(): String? = null

    override fun getPathSegments(): List<String> = emptyList()

    override fun getPort(): Int = -1

    override fun getQuery(): String? = null

    override fun getScheme(): String? = null

    override fun getSchemeSpecificPart(): String? = null

    override fun getUserInfo(): String? = null

    override fun isHierarchical(): Boolean = false

    override fun isRelative(): Boolean = false

    override fun toString(): String = "test://${System.identityHashCode(this)}"

    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)

    override fun compareTo(other: Uri): Int = toString().compareTo(other.toString())

    // MARK: - Parcelable Conformance

    override fun describeContents(): Int = 0

    override fun writeToParcel(
        destination: Parcel,
        flags: Int,
    ) = Unit

    // MARK: - Companion

    companion object {
        /** Allocates a new test URI. */
        fun allocate(): Uri {
            val field = Unsafe::class.java.getDeclaredField("theUnsafe")
            field.isAccessible = true
            return (field.get(null) as Unsafe).allocateInstance(TestUri::class.java) as Uri
        }
    }
}
