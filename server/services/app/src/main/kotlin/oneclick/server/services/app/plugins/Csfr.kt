package oneclick.server.services.app.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.csrf.*

internal fun Application.configureCsrf(
    originUrl: String,
    allowLocalOrigins: Boolean,
) {
    install(CSRF) {
        allowOrigin(originUrl)

        if (allowLocalOrigins) {
            allowOrigin("http://10.0.2.2:8080")
        }
    }
}