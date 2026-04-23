package oneclick.client.apps.home.mappers

import oneclick.client.apps.home.devices.BluetoothSensor
import oneclick.client.apps.home.models.DeviceType
import oneclick.shared.contracts.core.models.NonNegativeInt
import oneclick.shared.contracts.core.models.NonNegativeInt.Companion.toNonNegativeInt
import oneclick.shared.contracts.core.models.PositiveIntRange
import oneclick.shared.contracts.homes.models.Device

internal fun BluetoothSensor.toDevice(state: BluetoothSensor.State): Device? =
    when (state.deviceType) {
        DeviceType.WATER_LEVEL -> {
            val level = state.value.toIntOrNull()?.toNonNegativeInt() ?: return null
            Device.WaterSensor.waterSensor(id, waterSensorRange, level)
        }

        DeviceType.SOUND_ALARM -> null
    }

private val waterSensorRange = PositiveIntRange.unsafe(
    start = NonNegativeInt.zero,
    end = NonNegativeInt.unsafe(100)
)