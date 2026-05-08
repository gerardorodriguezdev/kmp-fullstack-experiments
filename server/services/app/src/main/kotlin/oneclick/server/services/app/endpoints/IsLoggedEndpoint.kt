package oneclick.server.services.app.endpoints

import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import oneclick.server.services.app.authentication.JwtCredentials.HomeJwtCredentials
import oneclick.server.services.app.authentication.JwtCredentials.UserJwtCredentials
import oneclick.server.services.app.plugins.apiRateLimit
import oneclick.server.services.app.plugins.authentication.allAuthentication
import oneclick.shared.contracts.auth.models.responses.IsLoggedResponse
import oneclick.shared.contracts.auth.models.responses.IsLoggedResponse.Logged
import oneclick.shared.contracts.auth.models.responses.IsLoggedResponse.NotLogged
import oneclick.shared.contracts.core.models.ClientEndpoint

internal fun Routing.isLoggedEndpoint() {
    apiRateLimit {
        allAuthentication(optional = true) {
            get(ClientEndpoint.IS_LOGGED.route) {
                val credentials = call.principal<UserJwtCredentials>() ?: call.principal<HomeJwtCredentials>()

                if (credentials == null) {
                    call.respond<IsLoggedResponse>(NotLogged)
                } else {
                    call.respond<IsLoggedResponse>(Logged)
                }
            }
        }
    }
}
