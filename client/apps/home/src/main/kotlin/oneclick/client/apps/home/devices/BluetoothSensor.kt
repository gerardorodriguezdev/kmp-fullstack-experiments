package oneclick.client.apps.home.devices

import com.juul.kable.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import oneclick.client.apps.home.models.CommunicationType
import oneclick.client.apps.home.models.DeviceType
import oneclick.shared.contracts.core.models.Uuid
import oneclick.shared.contracts.core.models.Uuid.Companion.toUuid
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi

internal class BluetoothSensor(
    val id: Uuid,
    private val peripheral: Peripheral,
) {
    private val _connection = MutableStateFlow(Connection.DISCONNECTED)
    val connection: StateFlow<Connection> = _connection

    private val deviceType = MutableStateFlow<DeviceType?>(null)
    private val rawState = MutableStateFlow<String?>(null)

    val state: Flow<State?> = combine(
        deviceType.filterNotNull(),
        rawState.filterNotNull(),
    ) { deviceType, state ->
        State(deviceType, state)
    }

    @OptIn(ExperimentalApi::class)
    suspend fun connect() =
        try {
            peripheral.connect().launch {
                peripheral
                    .observe(customCharacteristic)
                    .collect { byteArray ->
                        val abc = byteArray.decodeToString()
                        //TODO: if type update type
                        //TODO: if state update state
                    }

                //TODO: Update/Review
                if (deviceType.value == null) {
                    setCommunicationType(CommunicationType.META_DATA)
                } else {
                    setCommunicationType(CommunicationType.DATA)
                }
            }
        } catch (_: Exception) {
            //TODO: Log
            peripheral.disconnect()
        }

    private suspend fun setCommunicationType(communicationType: CommunicationType) {
        peripheral.write(
            customCharacteristic,
            communicationType.code.encodeToByteArray(),
            WriteType.WithoutResponse
        )
    }

    data class State(
        val deviceType: DeviceType,
        val value: String,
    )

    enum class Connection {
        CONNECTED,
        DISCONNECTED,
    }

    @OptIn(ExperimentalUuidApi::class, ExperimentalApi::class)
    companion object {
        private const val NAME_PREFIX = "OneClick"
        private val customServiceUuid = Bluetooth.BaseUuid + 0xFFE0
        private val customCharacteristicUuid = Bluetooth.BaseUuid + 0xFFE1
        private val customCharacteristic = characteristicOf(
            service = customServiceUuid,
            characteristic = customCharacteristicUuid,
        )
        private const val SCAN_TIMEOUT = 10_000L
        private val scanner = Scanner {
            filters {
                match {
                    services = listOf(customServiceUuid)
                    name = Filter.Name.Prefix(NAME_PREFIX)
                }
            }
        }

        suspend fun bluetoothSensors(): List<BluetoothSensor> =
            buildList {
                withTimeoutOrNull(SCAN_TIMEOUT.milliseconds) {
                    scanner
                        .advertisements
                        .mapNotNull { advertisement ->
                            val peripheral = Peripheral(advertisement)
                            val uuid = peripheral.identifier.toString().toUuid()
                            //TODO: Log bad
                            uuid?.let {
                                BluetoothSensor(uuid, peripheral)
                            }
                        }
                        .collect { bluetoothSensor ->
                            add(bluetoothSensor)
                        }
                }
            }
    }
}