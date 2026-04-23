package oneclick.client.apps.home.controllers

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import oneclick.client.apps.home.dataSources.base.DevicesStore
import oneclick.client.apps.home.devices.BluetoothSensor
import oneclick.client.apps.home.devices.BluetoothSensor.Connection
import oneclick.client.apps.home.mappers.toDevice
import oneclick.shared.dispatchers.platform.DispatchersProvider
import oneclick.shared.logging.AppLogger
import kotlin.time.Duration.Companion.milliseconds

internal class BluetoothDevicesController(
    private val appLogger: AppLogger,
    private val devicesStore: DevicesStore,
    private val dispatchersProvider: DispatchersProvider,
) : DevicesController {

    override suspend fun scan(): Boolean {
        val bluetoothSensors = BluetoothSensor.bluetoothSensors()

        return withContext(dispatchersProvider.io()) {
            try {
                if (bluetoothSensors.isEmpty()) {
                    appLogger.e("No bluetooth sensors found")
                    false
                } else {
                    bluetoothSensors.forEach { bluetoothSensor ->
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
                                        connectionDelay *= 2
                                    }
                                }
                            }
                        }

                        launch {
                            bluetoothSensor
                                .state
                                .filterNotNull()
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
                    true
                }
            } catch (error: Exception) {
                appLogger.e("Exception '${error.stackTraceToString()}' while scanning bluetooth sensors")
                false
            }
        }
    }

    companion object {
        private const val STARTING_CONNECTION_DELAY = 1_000L
    }
}