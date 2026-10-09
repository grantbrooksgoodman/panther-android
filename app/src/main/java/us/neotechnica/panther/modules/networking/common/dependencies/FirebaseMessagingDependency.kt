//
//  FirebaseMessagingDependency.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.common.dependencies

import com.google.firebase.messaging.FirebaseMessaging
import us.neotechnica.panther.subsystem.modules.dependencyinjection.interfaces.DependencyKey
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

// MARK: - Dependency

private object FirebaseMessagingDependency : DependencyKey<FirebaseMessaging> {
    override fun resolve(dependencies: DependencyValues): FirebaseMessaging = FirebaseMessaging.getInstance()
}

/** The Firebase Cloud Messaging entry point. */
val DependencyValues.firebaseMessaging: FirebaseMessaging
    get() = this[FirebaseMessagingDependency]
