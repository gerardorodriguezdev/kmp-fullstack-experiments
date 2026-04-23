package oneclick.client.apps.home.controllers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import oneclick.client.apps.home.dataSources.base.DevicesStore
import oneclick.client.apps.home.mappers.toDevice
import oneclick.client.apps.home.sensors.BluetoothSensor
import oneclick.client.apps.home.sensors.BluetoothSensor.Connection
import oneclick.shared.logging.AppLogger
import kotlin.time.Duration.Companion.milliseconds

internal class BluetoothDevicesController(
    private val appLogger: AppLogger,
    private val devicesStore: DevicesStore,
    private val backgroundScope: CoroutineScope,
    private val bluetoothSensorsProvider: suspend () -> List<BluetoothSensor>,
) : DevicesController {

    override suspend fun scan(): Boolean {
        val bluetoothSensors = bluetoothSensorsProvider()

        return try {
            if (bluetoothSensors.isEmpty()) {
                appLogger.e("No bluetooth sensors found")
                false
            } else {
                bluetoothSensors.forEach { bluetoothSensor ->
                    with(backgroundScope) {
                        launch {
                            var connectionDelay = STARTING_CONNECTION_DELAY

                            bluetoothSensor.connection.collect { connection ->
                                when (connection) {
                                    Connection.CONNECTED -> {
                                        connectionDelay = STARTING_CONNECTION_DELAY
                                    }

                                    Connection.DISCONNECTED -> {
                                        bluetoothSensor.connect()
                                        delay(connectionDelay.milliseconds)
                                        connectionDelay = (connectionDelay * 2).coerceAtMost(MAX_CONNECTION_DELAY)
                                    }
                                }
                            }
                        }

                        launch {
                            bluetoothSensor
                                .state
                                .collect { state ->
                                    val device = bluetoothSensor.toDevice(state)
                                    if (device == null) {
                                        appLogger.e("Invalid device with id '${bluetoothSensor.id}' with state '$state'")
                                    } else {
                                        devicesStore.updateDevice(device)
                                    }
                                }
                        }
                    }
                }
                true
            }
        } catch (error: Exception) {
            appLogger.e("Exception '${error.stackTraceToString()}' while scanning bluetooth sensors")
            false
        }
    }

    private companion object {
        const val STARTING_CONNECTION_DELAY = 1_000L
        const val MAX_CONNECTION_DELAY = 60_000L
    }
}