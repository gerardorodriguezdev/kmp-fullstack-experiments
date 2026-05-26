package oneclick.server.services.app.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.routing.*
import oneclick.server.services.app.plugins.RateLimitConstants.apiRateLimitName
import oneclick.server.services.app.plugins.RateLimitConstants.refillPeriod
import oneclick.shared.timeProvider.TimeProvider
import kotlin.time.Duration.Companion.seconds

internal fun Application.configureRateLimit(
    disableRateLimit: Boolean,
    timeProvider: TimeProvider
) {
    install(RateLimit) {
        register(apiRateLimitName) {
            val limit = if (disableRateLimit) Int.MAX_VALUE else RateLimitConstants.API_RATE_LIMIT
            rateLimiter(limit = limit, refillPeriod = refillPeriod, clock = { timeProvider.currentTimeMillis() })
        }
    }
}

private object RateLimitConstants {
    val apiRateLimitName = RateLimitName("api")
    val refillPeriod = 60.seconds
    const val API_RATE_LIMIT = 50
}

internal fun Routing.apiRateLimit(build: Route.() -> Unit) =
    rateLimit(configuration = apiRateLimitName, build = build)