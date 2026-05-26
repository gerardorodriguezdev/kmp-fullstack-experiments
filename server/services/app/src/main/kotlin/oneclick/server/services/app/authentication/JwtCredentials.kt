package oneclick.server.services.app.authentication

import kotlinx.serialization.Serializable
import oneclick.shared.contracts.core.models.Uuid
import kotlin.time.Duration

internal sealed interface JwtCredentials {
    val jti: Uuid
    val expirationTime: Duration

    @Serializable
    data class HomeJwtCredentials(
        override val jti: Uuid,
        val userId: Uuid,
        val homeId: Uuid,
    ) : JwtCredentials {
        override val expirationTime: Duration = HomeJwtProvider.jwtExpirationTime
    }

    @Serializable
    data class UserJwtCredentials(
        override val jti: Uuid,
        val userId: Uuid,
    ) : JwtCredentials {
        override val expirationTime: Duration = UserJwtProvider.jwtExpirationTime
    }
}