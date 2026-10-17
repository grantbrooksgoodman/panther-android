//
//  CacheDomainListTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import us.neotechnica.panther.subsystem.AppSubsystem
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.TranslationArchiverDelegate
import us.neotechnica.panther.subsystem.modules.foundation.models.CacheDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import java.lang.reflect.Modifier

class CacheDomainListTest {
    // MARK: - Tests

    @Test
    fun `every declared app cache domain is listed`() {
        val declared =
            Class
                .forName("us.neotechnica.panther.bundle.CacheDomainsKt")
                .methods
                .filter { Modifier.isStatic(it.modifiers) && it.name.startsWith("get") && it.returnType == CacheDomain::class.java }
                .map { (it.invoke(null, CacheDomain) as CacheDomain).rawValue }
                .toSet()
        val listed = CacheDomainList.appCacheDomains.map { it.rawValue }.toSet()

        // The list also carries the networking module's own domains.
        assertTrue((declared - listed).toString(), listed.containsAll(declared))
        assertTrue(listed.contains("contactImage"))
    }

    @Test
    fun `the local translation archive is a subsystem cache domain`() {
        assertTrue(CacheDomain.subsystemCases.any { it.rawValue == "localTranslationArchive" })
    }

    @Test
    fun `clearing the local translation archive domain clears the registered archive`() {
        var cleared = 0
        AppSubsystem.delegates.registerTranslationArchiverDelegate(
            object : TranslationArchiverDelegate {
                override fun clearArchive() {
                    cleared += 1
                }
            },
        )

        CoreUtilities.clearCaches(listOf(CacheDomain.localTranslationArchive))

        assertEquals(1, cleared)
    }

    @Test
    fun `clearing the contact image domain succeeds`() {
        CoreUtilities.clearCaches(listOf(CacheDomain.contactImage))
    }
}
