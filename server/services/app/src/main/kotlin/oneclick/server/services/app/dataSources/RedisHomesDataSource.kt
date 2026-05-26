package oneclick.server.services.app.dataSources

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import oneclick.server.services.app.dataSources.base.HomesDataSource
import oneclick.server.services.app.dataSources.models.HomesEntry
import oneclick.shared.contracts.core.models.*
import oneclick.shared.contracts.homes.models.Home
import oneclick.shared.dispatchers.platform.DispatchersProvider
import oneclick.shared.logging.AppLogger

@OptIn(ExperimentalLettuceCoroutinesApi::class)
internal class RedisHomesDataSource(
    private val syncCommands: RedisCoroutinesCommands<String, String>,
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
                val currentPageIndex = currentPageIndex.value.toLong()

                val homes = syncCommands
                    .homes(userId, start = currentPageIndex, end = pageSize.value.toLong() + currentPageIndex)

                PaginationResult(
                    value = HomesEntry(
                        userId = userId,
                        homes = UniqueList.unsafe(homes),
                    ),
                    pageIndex = NonNegativeInt.unsafe(currentPageIndex.toInt() + 1),
                    totalPages = NonNegativeInt.unsafe(syncCommands.totalHomes(userId)),
                )
            } catch (error: Exception) {
                appLogger.e("Error getting homes", error)
                null
            }
        }

    override suspend fun home(userId: Uuid, homeId: Uuid): Home? =
        withContext(dispatchersProvider.io()) {
            try {
                syncCommands.home(userId = userId, homeId = homeId)
            } catch (error: Exception) {
                appLogger.e("Error getting home", error)
                null
            }
        }

    override suspend fun saveHome(userId: Uuid, home: Home): Boolean =
        withContext(dispatchersProvider.io()) {
            try {
                syncCommands.saveHome(userId = userId, home = home)
                true
            } catch (error: Exception) {
                appLogger.e("Error saving home", error)
                false
            }
        }

    private fun userHomeIdsByUserIdKey(userId: Uuid): String = "userHomes:userId:${userId.value}"
    private fun homeByHomeIdKey(homeId: Uuid): String = "home:homeId:${homeId.value}"

    private suspend fun RedisCoroutinesCommands<String, String>.saveHome(userId: Uuid, home: Home) {
        rpush(userHomeIdsByUserIdKey(userId), home.id.value)

        val homeString = Json.encodeToString(home)
        set(homeByHomeIdKey(home.id), homeString)
    }

    private suspend fun RedisCoroutinesCommands<String, String>.home(userId: Uuid, homeId: Uuid): Home? {
        val hasHome = lpos(userHomeIdsByUserIdKey(userId), homeId.value) ?: return null
        return if (hasHome >= 0) {
            val homeString = get(homeByHomeIdKey(homeId)) ?: return null
            Json.decodeFromString<Home>(homeString)
        } else {
            null
        }
    }

    private suspend fun RedisCoroutinesCommands<String, String>.homes(
        userId: Uuid,
        start: Long,
        end: Long
    ): List<Home> =
        coroutineScope {
            val homeIds = lrange(userHomeIdsByUserIdKey(userId), start, end)
            homeIds
                .map { homeId ->
                    async {
                        val homeString = get(homeByHomeIdKey(Uuid.unsafe(homeId))) ?: return@async null
                        Json.decodeFromString<Home>(homeString)
                    }
                }
                .awaitAll()
                .filterNotNull()
        }

    private suspend fun RedisCoroutinesCommands<String, String>.totalHomes(userId: Uuid): Int =
        llen(userHomeIdsByUserIdKey(userId))?.toInt() ?: 0
}