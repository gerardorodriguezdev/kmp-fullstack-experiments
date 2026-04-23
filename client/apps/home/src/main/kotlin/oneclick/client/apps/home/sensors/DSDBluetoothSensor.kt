package oneclick.client.apps.home.sensors

import com.juul.kable.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import oneclick.client.apps.home.models.CommunicationType
import oneclick.client.apps.home.models.DeviceType
import oneclick.client.apps.home.sensors.BluetoothSensor.Companion.bluetoothScanner
import oneclick.client.apps.home.sensors.BluetoothSensor.Companion.bluetoothSensors
import oneclick.client.apps.home.sensors.BluetoothSensor.Connection
import oneclick.shared.contracts.core.models.Uuid
import kotlin.uuid.ExperimentalUuidApi

internal class DSDBluetoothSensor(
    override val id: Uuid,
    private val peripheral: Peripheral,
) : BluetoothSensor {
    private val _connection = MutableStateFlow(Connection.DISCONNECTED)
    override val connection: StateFlow<Connection> = _connection

    private val deviceType = MutableStateFlow<DeviceType?>(null)
    private val rawState = MutableStateFlow<String?>(null)

    override val state: Flow<BluetoothSensor.State> = combine(
        deviceType.filterNotNull(),
        rawState.filterNotNull(),
    ) { deviceType, state ->
        BluetoothSensor.State(deviceType, state)
    }

    @OptIn(ExperimentalApi::class)
    override suspend fun connect() {
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

    }

    private suspend fun setCommunicationType(communicationType: CommunicationType) {
        peripheral.write(
            customCharacteristic,
            communicationType.code.encodeToByteArray(),
            WriteType.WithoutResponse
        )
    }

    @OptIn(ExperimentalUuidApi::class, ExperimentalApi::class)
    companion object {
        private val customServiceUuid = Bluetooth.BaseUuid + 0xFFE0
        private val customCharacteristicUuid = Bluetooth.BaseUuid + 0xFFE1
        private val customCharacteristic = characteristicOf(
            service = customServiceUuid,
            characteristic = customCharacteristicUuid,
        )
        private val dsdBluetoothScanner = bluetoothScanner(customServiceUuid)
        suspend fun dsdBluetoothSensors(): List<BluetoothSensor> =
            bluetoothSensors(dsdBluetoothScanner) { id, peripheral ->
                DSDBluetoothSensor(id, peripheral)
            }
    }
}