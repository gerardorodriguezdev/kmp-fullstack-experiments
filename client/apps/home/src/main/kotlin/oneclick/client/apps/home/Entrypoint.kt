package oneclick.client.apps.home

import kotlinx.coroutines.*
import oneclick.client.apps.home.commands.CommandsHandler
import oneclick.client.apps.home.commands.CommandsParser
import oneclick.client.apps.home.commands.CommandsParser.CommandParserResult
import oneclick.client.apps.home.controllers.DevicesController
import oneclick.client.apps.home.dataSources.base.DevicesStore
import oneclick.client.apps.home.dataSources.base.HomeDataSource
import oneclick.client.shared.network.models.UserLoggedResult
import oneclick.client.shared.network.platform.AuthenticationDataSource
import oneclick.shared.contracts.homes.models.requests.SyncDevicesRequest
import oneclick.shared.dispatchers.platform.DispatchersProvider
import oneclick.shared.logging.AppLogger

internal class Entrypoint(
    private val dispatchersProvider: DispatchersProvider,
    private val authenticationDataSource: AuthenticationDataSource,
    private val devicesStore: DevicesStore,
    private val homeDataSource: HomeDataSource,
    private val devicesController: DevicesController,
    private val appLogger: AppLogger,
    private val commandsHandler: CommandsHandler,
) {
    private var syncJob: Job? = null

    fun start() = runBlocking<Unit> {
        withContext(dispatchersProvider.io()) {
            launch {
                while (isActive) {
                    commands()
                }
            }

            launch {
                while (isActive) {
                    syncJob?.cancel()
                    syncJob = launch { sync() }
                    delay(SYNC_INTERVAL)
                }
            }

            launch {
                var shouldScan = true
                var scanDelay = STARTING_SCAN_INTERVAL

                while (shouldScan && isActive) {
                    shouldScan = !devicesController.scan()
                    delay(scanDelay)
                    scanDelay *= 2
                }
            }
        }
    }

    private suspend fun commands() {
        print("> ")
        val commandString = readlnOrNull()?.trim()
        if (!commandString.isNullOrEmpty()) {
            when (val commandResult = CommandsParser.parse(commandString)) {
                is CommandParserResult.Success -> commandsHandler.execute(commandResult.command)
                is CommandParserResult.Error -> appLogger.e(commandResult.message)
            }
        }
    }

    private suspend fun sync() {
        val isUserLogged = authenticationDataSource.isUserLogged() == UserLoggedResult.Logged
        if (isUserLogged) {
            val devices = devicesStore.getDevices()
            homeDataSource.syncDevices(
                request = SyncDevicesRequest(devices = devices)
            )
        }
    }

    private companion object {
        const val SYNC_INTERVAL = 5000L
        const val STARTING_SCAN_INTERVAL = 1_000L
    }
}