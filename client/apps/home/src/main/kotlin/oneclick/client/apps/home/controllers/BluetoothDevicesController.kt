package oneclick.client.apps.home.controllers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import oneclick.client.apps.home.dataSources.base.DevicesStore
import oneclick.client.apps.home.mappers.toDevice
import oneclick.client.apps.home.sensors.BluetoothSensor
import oneclick.client.apps.home.sensors.BluetoothSensor.Connection
import oneclick.shared.logging.AppLogger

internal class BluetoothDevicesController(
    private val appLogger: AppLogger,
    private val devicesStore: DevicesStore,
    private val backgroundScope: CoroutineScope,
    private val bluetoothSensorsProvider: Flow<BluetoothSensor>,
) : DevicesController {

    override suspend fun scan(): Boolean =
        try {
            bluetoothSensorsProvider.collect { bluetoothSensor ->
                with(backgroundScope) {
                    launch {
                        bluetoothSensor.connection.collect { connection ->
                            when (connection) {
                                Connection.CONNECTED -> Unit

                                Connection.DISCONNECTED -> {
                                    bluetoothSensor.connect()
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
        } catch (error: Exception) {
            appLogger.e("Exception '${error.stackTraceToString()}' while scanning bluetooth sensors")
            false
        }

    private companion object {
        const val STARTING_CONNECTION_DELAY = 1_000L
        const val MAX_CONNECTION_DELAY = 60_000L
    }
}