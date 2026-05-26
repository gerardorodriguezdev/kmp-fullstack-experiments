package oneclick.server.services.app.authentication

import oneclick.server.shared.authentication.security.UuidProvider
import oneclick.server.shared.authentication.security.base.BaseEncryptedJwtProvider
import oneclick.shared.contracts.auth.models.Jwt
import oneclick.shared.contracts.core.models.Uuid
import oneclick.shared.security.encryption.base.Encryptor
import oneclick.shared.timeProvider.TimeProvider
import kotlin.time.Duration.Companion.minutes

internal class UserJwtProvider(
    secretSignKey: String,
    audience: String,
    issuer: String,
    timeProvider: TimeProvider,
    encryptor: Encryptor,
    uuidProvider: UuidProvider,
) : BaseEncryptedJwtProvider(
    secretSignKey = secretSignKey,
    audience = audience,
    issuer = issuer,
    expirationTime = jwtExpirationTime,
    timeProvider = timeProvider,
    encryptor = encryptor,
    uuidProvider = uuidProvider,
    verifierSetup = {
        withClaimPresence(USER_ID_CLAIM)
    },
) {
    fun jwt(userId: Uuid): Jwt =
        jwt(
            claims = buildMap {
                val userId = jwtId(userId)
                put(USER_ID_CLAIM, userId.value)
            }
        )

    companion object {
        const val USER_ID_CLAIM = "userId"
        val jwtExpirationTime = 5.minutes
    }
}