package oneclick.server.services.app.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.request.*

internal fun Application.configureRequestBodyLimit() {
    install(RequestBodyLimit)
}

private val RequestBodyLimit = createApplicationPlugin(
    name = "RequestBodyLimit",
    createConfiguration = ::RequestBodyLimitConfiguration
) {
    val bodyLimit = pluginConfig.bodyLimit

    onCall { call ->
        val contentLength = call.request.contentLength()
        if (contentLength != null && contentLength > bodyLimit) throw PayloadTooLargeException(bodyLimit)
    }
}

private data class RequestBodyLimitConfiguration(
    var bodyLimit: Int = DEFAULT_BODY_LIMIT,
) {
    companion object {
        const val DEFAULT_BODY_LIMIT = 2_048
    }
}

private class PayloadTooLargeException(bodyLimit: Int) :
    ContentTransformationException("Request is larger than the limit of $bodyLimit bytes")
