//
//  TimestampDateFormatterDependency.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.dependencies

import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.services.TimestampDateFormatter

/**
 * The dependency key that provides a [TimestampDateFormatter]
 * instance.
 */
object TimestampDateFormatterDependency : DependencyKey<TimestampDateFormatter> {
    override fun resolve(dependencies: DependencyValues): TimestampDateFormatter = TimestampDateFormatter()
}

/** The shared [TimestampDateFormatter] instance. */
val DependencyValues.timestampDateFormatter: TimestampDateFormatter
    get() = this[TimestampDateFormatterDependency]
