//
//  Throwable+HealthExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health.extensions

import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

// Whether the error indicates a network-level failure – evidence
// about the connection itself rather than the request's outcome.
internal val Throwable.isNetworkLevelFailure: Boolean
    get() =
        this is UnknownHostException ||
            this is ConnectException ||
            this is SocketTimeoutException ||
            this is SocketException
