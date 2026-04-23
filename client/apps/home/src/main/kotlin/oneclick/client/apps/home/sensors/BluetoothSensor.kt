package oneclick.client.apps.home.sensors

import com.juul.kable.Filter
import com.juul.kable.Peripheral
import com.juul.kable.PlatformAdvertisement
import com.juul.kable.Scanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeoutOrNull
import oneclick.client.apps.home.models.DeviceType
import oneclick.shared.contracts.core.models.Uuid
import oneclick.shared.contracts.core.models.Uuid.Companion.toUuid
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi

internal sealed interface BluetoothSensor {
    val id: Uuid
    val connection: StateFlow<Connection>
    val state: Flow<State>

    suspend fun connect()

    data class State(
        val deviceType: DeviceType,
        val value: String,
    )

    enum class Connection {
        CONNECTED,
        DISCONNECTED,
    }

    companion object {
        private const val NAME_PREFIX = "OneClick"
        private const val SCAN_TIMEOUT = 10_000L

        @OptIn(ExperimentalUuidApi::class)
        fun bluetoothScanner(serviceUuid: kotlin.uuid.Uuid): Scanner<PlatformAdvertisement> =
            Scanner {
                filters {
                    match {
                        services = listOf(serviceUuid)
                        name = Filter.Name.Prefix(NAME_PREFIX)
                    }
                }
            }


        suspend fun bluetoothSensors(
            scanner: Scanner<PlatformAdvertisement>,
            bluetoothSensorProvider: (id: Uuid, peripheral: Peripheral) -> BluetoothSensor,
        ): List<BluetoothSensor> =
            buildList {
                withTimeoutOrNull(SCAN_TIMEOUT.milliseconds) {
                    scanner
                        .advertisements
                        .mapNotNull { advertisement ->
                            val peripheral = Peripheral(advertisement)
                            val id = peripheral.identifier.toString().toUuid()
                            id?.let {
                                bluetoothSensorProvider(id, peripheral)
                            }
                        }
                        .collect { bluetoothSensor ->
                            add(bluetoothSensor)
                        }
                }
            }
    }
}