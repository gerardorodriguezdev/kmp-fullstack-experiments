package oneclick.client.apps.home.extensions

import com.juul.kable.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal fun scanner(serviceUuid: Uuid): Scanner<PlatformAdvertisement> = Scanner {
    filters {
        match {
            services = listOf(serviceUuid)
        }
    }
}

@OptIn(ExperimentalUuidApi::class, ExperimentalApi::class)
internal fun humidityCharacteristic(serviceUuid: Uuid): Characteristic = characteristicOf(
    service = serviceUuid,
    characteristic = Uuid.characteristic("humidity")
)

@OptIn(ExperimentalUuidApi::class, ExperimentalApi::class)
internal fun serialNumberCharacteristic(serviceUuid: Uuid): Characteristic = characteristicOf(
    service = serviceUuid,
    characteristic = Uuid.characteristic("serial_number_string")
)

internal fun ByteArray.toSerialNumber(): String = decodeToString()

internal fun ByteArray.toHumidity(): Int {
    if (size < 2) throw IllegalArgumentException("Humidity data must be at least 2 bytes")
    val lowByte = get(0).toInt() and 0xFF
    val highByte = get(1).toInt() and 0xFF
    val value = lowByte or (highByte shl 8)
    return value / 100
}