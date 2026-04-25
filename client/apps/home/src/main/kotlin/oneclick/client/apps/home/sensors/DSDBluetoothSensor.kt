package oneclick.client.apps.home.sensors

import com.juul.kable.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import oneclick.client.apps.home.models.CommunicationType
import oneclick.client.apps.home.models.CommunicationType.Companion.toCommunicationType
import oneclick.client.apps.home.models.DeviceType
import oneclick.client.apps.home.models.DeviceType.Companion.toDeviceType
import oneclick.client.apps.home.sensors.BluetoothSensor.Companion.bluetoothScanner
import oneclick.client.apps.home.sensors.BluetoothSensor.Companion.bluetoothSensors
import oneclick.client.apps.home.sensors.BluetoothSensor.Connection
import oneclick.client.apps.home.sensors.BluetoothSensor.State
import oneclick.shared.contracts.core.models.Uuid
import kotlin.uuid.ExperimentalUuidApi

internal class DSDBluetoothSensor(
    override val id: Uuid,
    private val peripheral: Peripheral,
) : BluetoothSensor {
    private val deviceType = MutableStateFlow<DeviceType?>(null)
    private val rawData = MutableStateFlow<String?>(null)

    override val connection: Flow<Connection> =
        peripheral.state.map { state ->
            when (state) {
                is State.Connected -> Connection.CONNECTED
                is State.Disconnected -> Connection.DISCONNECTED
                else -> Connection.DISCONNECTED
            }
        }

    override val state: Flow<State> = combine(
        deviceType.filterNotNull(),
        rawData.filterNotNull(),
        ::State,
    )

    @OptIn(ExperimentalApi::class)
    override suspend fun connect() {
        try {
            val scope = peripheral.connect()
            with(scope) {
                launch {
                    if (deviceType.value == null) {
                        setCommunicationType(CommunicationType.META_DATA)
                    } else {
                        setCommunicationType(CommunicationType.DATA)
                    }

                    peripheral
                        .observe(customCharacteristic)
                        .collect { byteArray ->
                            val response = byteArray.decodeToString()

                            val communicationTypeString = response.substringBefore(":")
                            val communicationType = communicationTypeString.toCommunicationType()
                            when (communicationType) {
                                null -> Unit
                                CommunicationType.META_DATA -> {
                                    val metaData = response.substringAfter(":")
                                    val entries = metaData
                                        .split(",")
                                        .map { entry ->
                                            entry.substringBefore("=") to entry.substringAfter("=")
                                        }

                                    val deviceType = entries
                                        .firstOrNull { (code, _) -> code == "T" }
                                        ?.second
                                        ?.toDeviceType()

                                    deviceType?.let {
                                        this@DSDBluetoothSensor.deviceType.value = deviceType
                                        setCommunicationType(CommunicationType.DATA)
                                    }
                                }

                                CommunicationType.DATA -> {
                                    val dat = response.substringAfter(":")
                                    val entries = dat
                                        .split(",")
                                        .map { entry ->
                                            entry.substringBefore("=") to entry.substringAfter("=")
                                        }
                                    val data = entries
                                        .firstOrNull { (code, _) -> code == "D" }
                                        ?.second

                                    data?.let {
                                        this@DSDBluetoothSensor.rawData.value = data
                                    }
                                }
                            }
                        }
                }
            }
        } catch (_: Exception) {
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
        fun dsdBluetoothSensors(): Flow<BluetoothSensor> =
            bluetoothSensors(dsdBluetoothScanner, ::DSDBluetoothSensor)
    }
}