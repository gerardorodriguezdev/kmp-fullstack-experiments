package oneclick.client.apps.user.core

import io.ktor.http.*
import oneclick.client.apps.user.core.buildkonfig.BuildKonfig
import oneclick.client.shared.network.extensions.urlProtocol

object Conf {
    val protocol: String? = BuildKonfig.PROTOCOL
    val host: String? = BuildKonfig.HOST
    val port: Int? = BuildKonfig.PORT
    val isDebug: Boolean = BuildKonfig.IS_DEBUG

    fun urlProtocol(): URLProtocol? = protocol?.urlProtocol()
}