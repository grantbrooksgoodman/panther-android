//
//  CommonServices.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

/**
 * The container that exposes the app's common services.
 *
 * Reach a common service through
 * [DependencyValues.commonServices] rather than referencing the
 * service singletons directly.
 */
object CommonServices {
    /** The account deletion service. */
    val accountDeletion get() = AccountDeletionService

    /** The analytics service. */
    val analytics get() = AnalyticsService

    /** The umbrella service for audio functionality. */
    val audio get() = AudioService

    /** The connection status service. */
    val connectionStatus get() = ConnectionStatusService

    /** The contact service. */
    val contact get() = ContactService

    /** The haptic feedback service. */
    val haptics get() = HapticsService

    /** The invite service. */
    val invite get() = InviteService

    /** The metadata service. */
    val metadata get() = MetadataService

    /** The network activity indicator service. */
    val networkActivityIndicator get() = NetworkActivityIndicatorService

    /** The notification service. */
    val notification get() = NotificationService

    /** The permission service. */
    val permission get() = PermissionService

    /** The phone number service. */
    val phoneNumber get() = PhoneNumberService

    /** The property lists service. */
    val propertyLists get() = CommonPropertyLists

    /** The push token service. */
    val pushToken get() = PushTokenService

    /** The region detail service. */
    val regionDetail get() = RegionDetailService

    /** The remote cache service. */
    val remoteCache get() = RemoteCacheService

    /** The review service. */
    val review get() = ReviewService

    /** The update service. */
    val update get() = UpdateService
}

// MARK: - Dependency

private object CommonServicesDependency : DependencyKey<CommonServices> {
    override fun resolve(dependencies: DependencyValues): CommonServices = CommonServices
}

/** The container that exposes the app's common services. */
val DependencyValues.commonServices: CommonServices
    get() = this[CommonServicesDependency]
