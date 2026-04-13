package oneclick.client.apps.home.devices

import com.juul.kable.Advertisement
import com.juul.kable.State
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import oneclick.client.apps.home.dataSources.base.DevicesStore
import oneclick.shared.contracts.core.models.NonNegativeInt
import oneclick.shared.contracts.core.models.PositiveIntRange
import oneclick.shared.contracts.core.models.Uuid
import oneclick.shared.contracts.homes.models.Device
import oneclick.shared.dispatchers.platform.DispatchersProvider
import oneclick.shared.logging.AppLogger
import kotlin.time.Duration.Companion.milliseconds

internal interface DevicesController {
    suspend fun scan(): Boolean
}

internal class FakeDevicesController(
    private val devicesStore: DevicesStore,
) : DevicesController {
    override suspend fun scan(): Boolean {
        delay(10_000.milliseconds)
        devicesStore.updateDevice(
            Device.WaterSensor.unsafe(
                id = Uuid.unsafe("7c0b3f78-0844-418a-827d-8a64e8d3d761"),
                range = PositiveIntRange.unsafe(
                    start = NonNegativeInt.unsafe(1),
                    end = NonNegativeInt.unsafe(100)
                ),
                level = NonNegativeInt.unsafe(50)
            )
        )
        return true
    }
}

internal class BluetoothDevicesController(
    private val appLogger: AppLogger,
    private val devicesStore: DevicesStore,
    private val dispatchersProvider: DispatchersProvider,
) : DevicesController {

    override suspend fun scan(): Boolean {
        return withContext(dispatchersProvider.io()) {
            try {
                val advertisements = mutableListOf<Advertisement>()
                withTimeoutOrNull(SCAN_TIMEOUT.milliseconds) {
                    WaterSensor.scanner.advertisements.collect { advertisement ->
                        advertisements.add(advertisement)
                    }
                }

                if (advertisements.isEmpty()) {
                    appLogger.i("No advertisements found")
                    return@withContext false
                } else {
                    advertisements.forEach { advertisement ->
                        val waterSensor = WaterSensor(advertisement)
                        launch {
                            var connectionDelay = STARTING_CONNECTION_DELAY
                            waterSensor.connection.collect { state ->
                                when (state) {
                                    is State.Connected -> connectionDelay = STARTING_CONNECTION_DELAY
                                    is State.Disconnected -> {
                                        waterSensor.connect()
                                        delay(connectionDelay.milliseconds)
                                        connectionDelay *= 2
                                    }

                                    else -> Unit
                                }
                            }
                        }

                        launch {
                            waterSensor.state.collect { device ->
                                devicesStore.updateDevice(device)
                            }
                        }
                    }
                    return@withContext true
                }
            } catch (error: Exception) {
                appLogger.e("Exception '${error.stackTraceToString()}' while scanning devices")
                return@withContext false
            }
        }
    }

    private companion object {
        const val SCAN_TIMEOUT = 10_000L
        const val STARTING_CONNECTION_DELAY = 1_000L
    }
}
