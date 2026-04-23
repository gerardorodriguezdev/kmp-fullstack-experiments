package oneclick.client.apps.home.mappers

import oneclick.client.apps.home.models.DeviceType.SOUND_ALARM
import oneclick.client.apps.home.models.DeviceType.WATER_LEVEL
import oneclick.client.apps.home.sensors.BluetoothSensor
import oneclick.client.apps.home.sensors.BluetoothSensor.State
import oneclick.shared.contracts.core.models.NonNegativeInt
import oneclick.shared.contracts.core.models.NonNegativeInt.Companion.toNonNegativeInt
import oneclick.shared.contracts.core.models.PositiveIntRange
import oneclick.shared.contracts.homes.models.Device

internal fun BluetoothSensor.toDevice(state: State): Device? =
    when (state.deviceType) {
        WATER_LEVEL -> {
            val level = state.value.toIntOrNull()?.toNonNegativeInt() ?: return null
            Device.WaterSensor.waterSensor(id, waterSensorRange, level)
        }

        SOUND_ALARM -> null
    }

private val waterSensorRange = PositiveIntRange.unsafe(
    start = NonNegativeInt.zero,
    end = NonNegativeInt.unsafe(100)
)