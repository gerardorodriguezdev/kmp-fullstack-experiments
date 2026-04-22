package oneclick.client.apps.home.devices

import kotlinx.coroutines.delay
import oneclick.client.apps.home.dataSources.base.DevicesStore
import oneclick.shared.contracts.core.models.NonNegativeInt
import oneclick.shared.contracts.core.models.PositiveIntRange
import oneclick.shared.contracts.core.models.Uuid
import oneclick.shared.contracts.homes.models.Device
import kotlin.time.Duration.Companion.milliseconds

internal class FakeDevicesController(
    private val devicesStore: DevicesStore,
) : DevicesController {
    override suspend fun scan(): Boolean {
        delay(10_000.milliseconds)
        devicesStore.updateDevice(
            Device.WaterSensor.unsafe(
                id = Uuid.unsafe("7c0b3f78-0844-418a-827d-8a64e8d3d761"),
                range = PositiveIntRange.unsafe(
                    start = NonNegativeInt.unsafe(1),
                    end = NonNegativeInt.unsafe(100)
                ),
                level = NonNegativeInt.unsafe(50)
            )
        )
        return true
    }
}