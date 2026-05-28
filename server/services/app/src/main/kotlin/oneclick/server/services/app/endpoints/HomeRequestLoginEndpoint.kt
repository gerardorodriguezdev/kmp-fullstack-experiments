package oneclick.server.services.app.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import oneclick.server.services.app.authentication.HomeJwtProvider
import oneclick.server.services.app.dataSources.base.UsersDataSource
import oneclick.server.services.app.plugins.apiRateLimit
import oneclick.server.services.app.repositories.HomesRepository
import oneclick.server.services.app.repositories.UsersRepository
import oneclick.server.shared.authentication.security.PasswordManager
import oneclick.server.shared.utils.clientType
import oneclick.shared.contracts.auth.models.Jwt
import oneclick.shared.contracts.auth.models.requests.LoginRequest.HomeRequestLoginRequest
import oneclick.shared.contracts.auth.models.responses.HomeRequestLoginResponse
import oneclick.shared.contracts.core.models.ClientEndpoint
import oneclick.shared.contracts.core.models.ClientType
import oneclick.shared.contracts.core.models.UniqueList
import oneclick.shared.contracts.core.models.Uuid
import oneclick.shared.contracts.homes.models.Home

internal fun Routing.homeRequestLoginEndpoint(
    usersRepository: UsersRepository,
    homesRepository: HomesRepository,
    passwordManager: PasswordManager,
    homeJwtProvider: HomeJwtProvider,
) {
    apiRateLimit {
        post(ClientEndpoint.HOME_REQUEST_LOGIN.route) { homeRequestLoginRequest: HomeRequestLoginRequest ->
            val (username, password, homeId) = homeRequestLoginRequest

            val clientType = call.request.clientType
            if (clientType != ClientType.DESKTOP) {
                application.log.debug("Invalid client type")
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            val user = usersRepository.user(UsersDataSource.Findable.ByUsername(username))
            if (user == null) {
                application.log.debug("User not found")
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            val isPasswordValid = passwordManager.isPasswordValid(
                password = password,
                hashedPassword = user.hashedPassword
            )
            if (!isPasswordValid) {
                application.log.debug("Invalid password")
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }

            val home = homesRepository.home(userId = user.userId, homeId = homeId)
            if (home == null) {
                application.log.debug("Registrable home")
                registerHome(
                    userId = user.userId,
                    homesRepository = homesRepository,
                    homeJwtProvider = homeJwtProvider,
                    homeId = homeId,
                )
            } else {
                respondValidLogin(
                    jwt = homeJwtProvider.jwt(userId = user.userId, homeId = homeId),
                )
            }
        }
    }
}

private suspend fun RoutingContext.registerHome(
    userId: Uuid,
    homesRepository: HomesRepository,
    homeJwtProvider: HomeJwtProvider,
    homeId: Uuid,
) {
    val newHome = Home(
        id = homeId,
        devices = UniqueList.emptyUniqueList(),
    )

    val isHomeSaved = homesRepository.saveHome(userId = userId, home = newHome)
    if (!isHomeSaved) {
        call.application.log.debug("Home not saved")
        call.respond(HttpStatusCode.InternalServerError)
        return
    }

    val jwt = homeJwtProvider.jwt(userId = userId, homeId = newHome.id)
    respondValidLogin(jwt = jwt)
}

private suspend fun RoutingContext.respondValidLogin(jwt: Jwt) {
    call.application.log.debug("Valid login")
    call.respond<HomeRequestLoginResponse>(
        HomeRequestLoginResponse.ValidLogin(jwt = jwt)
    )
}
