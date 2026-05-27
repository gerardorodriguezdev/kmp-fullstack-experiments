package oneclick.server.services.app.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import oneclick.server.services.app.plugins.authentication.homeAuthentication
import oneclick.server.services.app.plugins.authentication.requireHomeJwtCredentials
import oneclick.server.services.app.repositories.HomesRepository
import oneclick.shared.contracts.core.models.ClientEndpoint
import oneclick.shared.contracts.homes.models.Home
import oneclick.shared.contracts.homes.models.requests.SyncDevicesRequest

internal fun Routing.homeSyncDevicesEndpoint(homesRepository: HomesRepository) {
    homeAuthentication {
        post(ClientEndpoint.HOME_SYNC_DEVICES.route) { syncDevicesRequest: SyncDevicesRequest ->
            val (_, userId, homeId) = requireHomeJwtCredentials()
            val isHomeSaved = homesRepository.saveHome(
                userId = userId,
                home = Home(id = homeId, devices = syncDevicesRequest.devices)
            )
            if (isHomeSaved) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.application.log.debug("Home not saved")
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}
