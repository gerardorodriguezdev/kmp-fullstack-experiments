package oneclick.client.apps.home.sensors

import com.juul.kable.Filter
import com.juul.kable.Peripheral
import com.juul.kable.PlatformAdvertisement
import com.juul.kable.Scanner
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.timeout
import oneclick.client.apps.home.models.DeviceType
import oneclick.shared.contracts.core.models.Uuid
import oneclick.shared.contracts.core.models.Uuid.Companion.toUuid
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi

internal interface BluetoothSensor {
    val id: Uuid
    val connection: Flow<Connection>
    val state: Flow<State>

    suspend fun connect()

    data class State(
        val deviceType: DeviceType,
        val data: String,
    )

    enum class Connection {
        CONNECTED, DISCONNECTED,
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


        @OptIn(FlowPreview::class)
        fun bluetoothSensors(
            scanner: Scanner<PlatformAdvertisement>,
            bluetoothSensorProvider: (id: Uuid, peripheral: Peripheral) -> BluetoothSensor,
        ): Flow<BluetoothSensor> {
            val bluetoothSensorsIds = mutableSetOf<Uuid>()

            return scanner
                .advertisements
                .mapNotNull { advertisement ->
                    val bluetoothSensorId = advertisement.identifier.toString().toUuid() ?: return@mapNotNull null

                    if (bluetoothSensorsIds.contains(bluetoothSensorId)) return@mapNotNull null

                    bluetoothSensorsIds.add(bluetoothSensorId)
                    val peripheral = Peripheral(advertisement)
                    bluetoothSensorProvider(bluetoothSensorId, peripheral)
                }
                .timeout(SCAN_TIMEOUT.milliseconds)
                .catch { _ -> }
        }
    }
}