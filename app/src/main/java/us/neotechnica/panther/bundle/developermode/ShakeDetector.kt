//
//  ShakeDetector.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle.developermode

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlin.math.sqrt

/**
 * Detects a device shake gesture and invokes [onShake].
 *
 * Registers an accelerometer listener for the lifetime of the
 * composition, reporting a shake when the net g-force exceeds the shake
 * threshold. Successive shakes are debounced.
 *
 * @param onShake The closure to invoke when a shake is detected.
 */
@Composable
fun ShakeDetector(onShake: () -> Unit) {
    val context = LocalContext.current
    val currentOnShake by rememberUpdatedState(onShake)

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensorManager == null || accelerometer == null) {
            return@DisposableEffect onDispose {}
        }

        var lastShakeTime = 0L
        val listener =
            object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
                    if (gForce <= SHAKE_THRESHOLD) return

                    val now = System.currentTimeMillis()
                    if (now - lastShakeTime < SHAKE_DEBOUNCE_MILLIS) return
                    lastShakeTime = now
                    currentOnShake()
                }

                override fun onAccuracyChanged(
                    sensor: Sensor?,
                    accuracy: Int,
                ) = Unit
            }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }
}

private const val SHAKE_THRESHOLD = 2.7f
private const val SHAKE_DEBOUNCE_MILLIS = 1_000L
