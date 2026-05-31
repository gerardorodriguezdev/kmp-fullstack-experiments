package oneclick.client.apps.home.sensors

import oneclick.client.apps.home.models.CommunicationType
import oneclick.client.apps.home.models.CommunicationType.Companion.toCommunicationType
import oneclick.client.apps.home.models.DeviceType
import oneclick.client.apps.home.models.DeviceType.Companion.toDeviceType
import oneclick.client.apps.home.sensors.BluetoothCommunicationDecoder.DecoderResult.Success
import oneclick.shared.logging.AppLogger

internal class BluetoothCommunicationDecoder(private val appLogger: AppLogger) {

    fun decode(input: ByteArray): DecoderResult {
        val inputString = input.decodeToString()

        val communicationTypeString = inputString.substringBefore(":")
        val communicationType = communicationTypeString.toCommunicationType()

        val entriesString = inputString.substringAfter(":")
        val entries = entriesString.entries()

        return when (communicationType) {
            null -> DecoderResult.Error
            CommunicationType.META_DATA -> entries.metaData()
            CommunicationType.DATA -> entries.data()
        }
    }

    private fun String.entries(): List<Entry> {
        return split(",").map { entry ->
            Entry(
                code = entry.substringBefore("="),
                value = entry.substringAfter("="),
            )
        }
    }

    private fun List<Entry>.findEntry(code: String): Entry? {
        return firstOrNull { entry -> entry.code == code }
    }

    private fun List<Entry>.metaData(): DecoderResult {
        val deviceType = findEntry(DEVICE_TYPE_CODE)?.value?.toDeviceType()

        if (deviceType == null) {
            appLogger.e("DeviceType was null")
            return DecoderResult.Error
        }

        return Success.MetaData(deviceType = deviceType)
    }

    private fun List<Entry>.data(): DecoderResult {
        val data = findEntry(DATA_CODE)?.value

        if (data == null) {
            appLogger.e("Data was null")
            return DecoderResult.Error
        }

        return Success.Data(data = data)
    }

    private companion object {
        const val DEVICE_TYPE_CODE = "T"
        const val DATA_CODE = "D"
    }

    private data class Entry(val code: String, val value: String)

    sealed interface DecoderResult {
        sealed interface Success : DecoderResult {
            data class MetaData(val deviceType: DeviceType) : Success
            data class Data(val data: String) : Success
        }

        object Error : DecoderResult
    }
}