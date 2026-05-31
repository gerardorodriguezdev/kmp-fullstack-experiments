package oneclick.client.apps.home.sensors

import com.juul.kable.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import oneclick.client.apps.home.models.CommunicationType
import oneclick.client.apps.home.models.DeviceType
import oneclick.client.apps.home.sensors.BluetoothCommunicationDecoder.DecoderResult
import oneclick.client.apps.home.sensors.BluetoothSensor.Companion.bluetoothScanner
import oneclick.client.apps.home.sensors.BluetoothSensor.Companion.bluetoothSensors
import oneclick.client.apps.home.sensors.BluetoothSensor.Connection
import oneclick.client.apps.home.sensors.BluetoothSensor.State
import oneclick.shared.contracts.core.models.Uuid
import oneclick.shared.logging.AppLogger
import kotlin.uuid.ExperimentalUuidApi

internal class DSDBluetoothSensor(
    override val id: Uuid,
    private val peripheral: Peripheral,
    private val appLogger: AppLogger,
    private val bluetoothCommunicationDecoder: BluetoothCommunicationDecoder = BluetoothCommunicationDecoder(appLogger),
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
                }

                launch {
                    peripheral
                        .observe(customCharacteristic)
                        .collect { byteArray ->
                            when (val decoderResult = bluetoothCommunicationDecoder.decode(byteArray)) {
                                is DecoderResult.Success.MetaData -> {
                                    deviceType.value = decoderResult.deviceType
                                    setCommunicationType(CommunicationType.DATA)
                                }

                                is DecoderResult.Success.Data -> {
                                    rawData.value = decoderResult.data
                                }

                                is DecoderResult.Error -> Unit
                            }
                        }
                }
            }
        } catch (error: Exception) {
            appLogger.e("Exception '${error.stackTraceToString()}' while connecting to bluetooth sensor")
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
        fun dsdBluetoothSensors(appLogger: AppLogger): Flow<BluetoothSensor> =
            bluetoothSensors(dsdBluetoothScanner, ::DSDBluetoothSensor, appLogger)
    }
}