package oneclick.server.services.app.dataSources

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import oneclick.server.services.app.dataSources.base.HomesDataSource
import oneclick.server.services.app.dataSources.models.HomesEntry
import oneclick.server.services.app.postgresql.*
import oneclick.shared.contracts.core.models.*
import oneclick.shared.contracts.core.models.NonNegativeInt.Companion.toNonNegativeInt
import oneclick.shared.contracts.homes.models.Device
import oneclick.shared.contracts.homes.models.Home
import oneclick.shared.dispatchers.platform.DispatchersProvider
import oneclick.shared.logging.AppLogger

internal class PostgresHomesDataSource(
    private val database: AppDatabase,
    private val dispatchersProvider: DispatchersProvider,
    private val appLogger: AppLogger,
) : HomesDataSource {

    override suspend fun homesEntry(
        userId: Uuid,
        pageSize: PositiveInt,
        currentPageIndex: NonNegativeInt
    ): PaginationResult<HomesEntry>? =
        withContext(dispatchersProvider.io()) {
            try {
                val entries = database.homesQueries.homesByUserId(
                    user_id = userId.value,
                    value_ = pageSize.value.toLong(),
                    value__ = currentPageIndex.value.toLong()
                ).executeAsList()

                val (homes, devices) = entries.homesByUserIdToEntries().normalizeHomes()
                PaginationResult(
                    value = HomesEntry(
                        userId = userId,
                        homes = toHomes(homes, devices),
                    ),
                    pageIndex = NonNegativeInt.unsafe(currentPageIndex.value + 1),
                    totalPages = totalHomes()
                )
            } catch (error: Exception) {
                appLogger.e("Error getting homes", error)
                null
            }
        }

    private suspend fun CoroutineScope.toHomes(
        homes: HashSet<Homes>,
        devices: HashSet<Devices>
    ): UniqueList<Home> =
        UniqueList.unsafe(
            homes.map { home ->
                async {
                    val devices = devices.filter { device -> device.home_id == home.home_id }
                    Home(
                        id = Uuid.unsafe(home.home_id),
                        devices = toDevices(devices)
                    )
                }
            }.awaitAll()
        )

    private suspend fun CoroutineScope.toDevices(devices: List<Devices>): UniqueList<Device> =
        UniqueList.unsafe(
            devices.map { device ->
                async {
                    Json.decodeFromString<Device>(device.device)
                }
            }.awaitAll()
        )

    private fun totalHomes(): NonNegativeInt =
        database
            .homesQueries
            .totalHomes()
            .executeAsOneOrNull()
            ?.toInt()
            ?.toNonNegativeInt()
            ?: NonNegativeInt.zero

    override suspend fun home(userId: Uuid, homeId: Uuid): Home? =
        withContext(dispatchersProvider.io()) {
            try {
                val entries = database.homesQueries.homeByUserIdAndHomeId(
                    user_id = userId.value,
                    home_id = homeId.value,
                ).executeAsList()

                val (homes, devices) = entries.homeByUserIdAndHomeIdToEntries().normalizeHomes()
                toHomes(homes, devices).firstOrNull()
            } catch (error: Exception) {
                appLogger.e("Error getting home", error)
                null
            }
        }

    override suspend fun saveHome(userId: Uuid, home: Home): Boolean =
        withContext(dispatchersProvider.io()) {
            try {
                database.homesQueries.insertHome(
                    Homes(user_id = userId.value, home_id = home.id.value)
                )

                home.devices.map { device ->
                    async {
                        val devices = Devices(
                            home_id = home.id.value,
                            device_id = device.id.value,
                            device = Json.encodeToString(device)
                        )
                        database.devicesQueries.insertDevice(devices)
                    }
                }.awaitAll()

                true
            } catch (error: Exception) {
                appLogger.e("Error inserting home", error)
                false
            }
        }

    private data class NormalizedHomes(
        val homes: HashSet<Homes>,
        val devices: HashSet<Devices>,
    )

    private data class Entry(
        val userId: String?,
        val homeId: String,
        val deviceId: String,
        val device: String,
    )

    private fun List<Entry>.normalizeHomes(): NormalizedHomes {
        val homes = hashSetOf<Homes>()
        val devices = hashSetOf<Devices>()

        forEach { entry ->
            homes.add(
                Homes(
                    user_id = entry.userId,
                    home_id = entry.homeId,
                )
            )

            devices.add(
                Devices(
                    home_id = entry.homeId,
                    device_id = entry.deviceId,
                    device = entry.device,
                )
            )
        }

        return NormalizedHomes(homes, devices)
    }

    private fun List<HomesByUserId>.homesByUserIdToEntries(): List<Entry> =
        map { entry ->
            Entry(
                userId = entry.user_id,
                homeId = entry.home_id,
                deviceId = entry.device_id,
                device = entry.device,
            )
        }

    private fun List<HomeByUserIdAndHomeId>.homeByUserIdAndHomeIdToEntries(): List<Entry> =
        map { entry ->
            Entry(
                userId = entry.user_id,
                homeId = entry.home_id,
                deviceId = entry.device_id,
                device = entry.device,
            )
        }

}
