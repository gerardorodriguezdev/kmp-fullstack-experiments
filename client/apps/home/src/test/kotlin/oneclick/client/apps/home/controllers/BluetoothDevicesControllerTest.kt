package oneclick.client.apps.home.controllers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import oneclick.client.apps.home.dataSources.MemoryDevicesStore
import oneclick.client.apps.home.models.DeviceType
import oneclick.client.apps.home.sensors.BluetoothSensor
import oneclick.client.apps.home.sensors.BluetoothSensor.Connection
import oneclick.client.apps.home.sensors.BluetoothSensor.State
import oneclick.shared.contracts.core.models.NonNegativeInt
import oneclick.shared.contracts.core.models.PositiveIntRange
import oneclick.shared.contracts.core.models.Uuid
import oneclick.shared.contracts.homes.models.Device
import oneclick.shared.logging.EmptyAppLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BluetoothDevicesControllerTest {
    private val devicesStore = MemoryDevicesStore()
    private var bluetoothSensorsProvider = { emptyList<BluetoothSensor>() }

    @Test
    fun `GIVEN scan WHEN no sensors THEN returns false`() = runTest(UnconfinedTestDispatcher()) {
        val bluetoothDevicesController = bluetoothDevicesController(backgroundScope)

        val result = bluetoothDevicesController.scan()

        assertFalse(result)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `GIVEN scan WHEN sensor available THEN returns true and connect is called`() =
        runTest(UnconfinedTestDispatcher()) {
            val bluetoothSensor = bluetoothSensor(connection = Connection.DISCONNECTED, value = "1")
            bluetoothSensorsProvider = { listOf(bluetoothSensor) }
            val expectedDevices = listOf(waterSensor())
            val bluetoothDevicesController = bluetoothDevicesController(backgroundScope)

            val result = bluetoothDevicesController.scan()

            assertTrue(result)
            assertEquals(expected = 1, actual = bluetoothSensor.connectCalls)
            assertEquals(expected = expectedDevices, actual = devicesStore.getDevices().elements)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `GIVEN scan WHEN with connected sensor THEN no call to connect`() =
        runTest(UnconfinedTestDispatcher()) {
            val bluetoothSensor = bluetoothSensor(connection = Connection.CONNECTED, value = "1")
            bluetoothSensorsProvider = { listOf(bluetoothSensor) }
            val bluetoothDevicesController = bluetoothDevicesController(backgroundScope)

            val result = bluetoothDevicesController.scan()

            assertTrue(result)
            assertEquals(expected = 0, actual = bluetoothSensor.connectCalls)
        }

    private fun bluetoothDevicesController(backgroundScope: CoroutineScope): BluetoothDevicesController =
        BluetoothDevicesController(
            appLogger = EmptyAppLogger(),
            devicesStore = devicesStore,
            backgroundScope = backgroundScope,
            bluetoothSensorsProvider = bluetoothSensorsProvider,
        )

    //TODO: Reuse
    private class FakeBluetoothSensor(
        val fakeConnection: MutableStateFlow<Connection>,
        val fakeState: MutableStateFlow<State>,
    ) : BluetoothSensor {
        var connectCalls = 0

        override val id: Uuid = Companion.id
        override val connection: StateFlow<Connection> = fakeConnection
        override val state: Flow<State> = fakeState
        override suspend fun connect() {
            connectCalls++
        }
    }

    //TODO: Reuse
    private companion object {
        val id = Uuid.unsafe("7c0b3f78-0844-418a-827d-8a64e8d3d761")
        fun bluetoothSensor(
            connection: Connection = Connection.DISCONNECTED,
            deviceType: DeviceType = DeviceType.WATER_LEVEL,
            value: String = "1",
        ): FakeBluetoothSensor =
            FakeBluetoothSensor(
                fakeConnection = MutableStateFlow(connection),
                fakeState = MutableStateFlow(State(deviceType = deviceType, value = value)),
            )

        fun waterSensor(
            id: Uuid = Companion.id,
            level: NonNegativeInt = NonNegativeInt.unsafe(1),
        ): Device.WaterSensor =
            Device.WaterSensor.unsafe(
                id = id,
                range = waterSensorRange,
                level = level
            )

        val waterSensorRange = PositiveIntRange.unsafe(
            start = NonNegativeInt.zero,
            end = NonNegativeInt.unsafe(100)
        )
    }
}