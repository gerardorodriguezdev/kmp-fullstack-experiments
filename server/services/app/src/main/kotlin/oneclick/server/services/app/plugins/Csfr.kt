package oneclick.server.services.app.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.csrf.*

internal fun Application.configureCsrf(originUrl: String) {
    install(CSRF) {
        allowOrigin(originUrl)
    }
}