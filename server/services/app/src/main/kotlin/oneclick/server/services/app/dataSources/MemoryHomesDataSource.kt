package oneclick.server.services.app.dataSources

import oneclick.server.services.app.dataSources.base.HomesDataSource
import oneclick.server.services.app.dataSources.models.HomesEntry
import oneclick.shared.contracts.core.models.*
import oneclick.shared.contracts.core.models.UniqueList.Companion.toUniqueList
import oneclick.shared.contracts.homes.models.Home

internal class MemoryHomesDataSource(
    private val homesEntries: LinkedHashMap<Uuid, HomesEntry> = linkedMapOf(),
) : HomesDataSource {

    override suspend fun homesEntry(
        userId: Uuid,
        pageSize: PositiveInt,
        currentPageIndex: NonNegativeInt
    ): PaginationResult<HomesEntry>? {
        val homesEntry = homesEntries[userId] ?: return null

        return paginateHomesEntry(
            homesEntry = homesEntry,
            pageSize = pageSize,
            currentPageIndex = currentPageIndex
        )
    }

    private fun paginateHomesEntry(
        homesEntry: HomesEntry,
        pageSize: PositiveInt,
        currentPageIndex: NonNegativeInt
    ): PaginationResult<HomesEntry> {
        val firstPageIndex = currentPageIndex.value + 1
        val lastPageIndex = firstPageIndex + pageSize.value

        var newPageIndex = 0
        val newHomes = buildList {
            for (index in firstPageIndex until lastPageIndex) {
                val home = homesEntry.homes.getOrNull(index)
                if (home != null) {
                    newPageIndex = index
                    add(home)
                } else {
                    break
                }
            }
        }

        val newHomesEntry = HomesEntry(
            userId = homesEntry.userId,
            homes = UniqueList.unsafe(newHomes),
        )

        return PaginationResult(
            value = newHomesEntry,
            pageIndex = NonNegativeInt.unsafe(newPageIndex),
            totalPages = NonNegativeInt.unsafe(homesEntry.homes.size),
        )
    }

    override suspend fun home(userId: Uuid, homeId: Uuid): Home? {
        val userHomes = homesEntries[userId]

        return userHomes?.homes?.firstOrNull { home -> home.id == homeId }
    }

    override suspend fun saveHome(
        userId: Uuid,
        home: Home
    ): Boolean {
        val currentHomesEntry = homesEntries[userId]
        if (currentHomesEntry == null) {
            homesEntries[userId] = HomesEntry(userId, UniqueList.unsafe(listOf(home)))
            return true
        } else {
            val newHomes = buildList {
                currentHomesEntry.homes.forEach { currentHome ->
                    if (currentHome.id == home.id) {
                        add(home)
                    } else {
                        add(currentHome)
                    }
                }
            }.toUniqueList()

            if (newHomes == null) {
                return false
            } else {
                homesEntries[userId] = currentHomesEntry.copy(homes = newHomes)
                return true
            }
        }
    }
}
