package oneclick.server.services.app.plugins

import io.ktor.server.application.*
import io.ktor.server.sessions.*
import oneclick.server.services.app.authentication.AuthenticationType
import oneclick.server.services.app.authentication.UserJwtProvider
import oneclick.shared.contracts.auth.models.Jwt

internal fun Application.configureSessions(disableSecureCookies: Boolean) {
    install(Sessions) {
        cookie<Jwt>(AuthenticationType.USER_SESSION.value) {
            cookie.path = "/"
            cookie.maxAgeInSeconds = UserJwtProvider.jwtExpirationTime.inWholeSeconds
            cookie.secure = !disableSecureCookies
            cookie.httpOnly = true
        }
    }
}
