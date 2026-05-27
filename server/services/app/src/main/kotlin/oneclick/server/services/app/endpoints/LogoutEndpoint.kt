package oneclick.server.services.app.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import oneclick.server.services.app.dataSources.base.InvalidJwtDataSource
import oneclick.server.services.app.plugins.authentication.allAuthentication
import oneclick.server.services.app.plugins.authentication.requireJwtCredentials
import oneclick.shared.contracts.auth.models.Jwt
import oneclick.shared.contracts.core.models.ClientEndpoint

internal fun Routing.logoutEndpoint(invalidJwtDataSource: InvalidJwtDataSource) {
    allAuthentication {
        get(ClientEndpoint.LOGOUT.route) {
            val jwtCredentials = requireJwtCredentials()

            val isInvalidJwtSaved = invalidJwtDataSource.saveInvalidJwt(jwtCredentials)
            if (!isInvalidJwtSaved) {
                call.application.log.debug("Invalid jwt not saved")
                call.respond(HttpStatusCode.InternalServerError)
                return@get
            }

            call.sessions.clear<Jwt>()
            call.respond(HttpStatusCode.OK)
        }
    }
}
