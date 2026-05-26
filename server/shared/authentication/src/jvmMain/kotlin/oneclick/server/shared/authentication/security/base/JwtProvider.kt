package oneclick.server.shared.authentication.security.base

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Verification
import oneclick.server.shared.authentication.models.JwtId
import oneclick.server.shared.authentication.security.UuidProvider
import oneclick.shared.contracts.auth.models.Jwt
import oneclick.shared.contracts.core.models.Uuid
import oneclick.shared.contracts.core.models.Uuid.Companion.toUuid
import oneclick.shared.security.encryption.base.Encryptor
import oneclick.shared.timeProvider.TimeProvider
import java.util.*
import kotlin.time.Duration

abstract class BaseEncryptedJwtProvider(
    private val secretSignKey: String,
    private val audience: String,
    private val issuer: String,
    private val expirationTime: Duration,
    private val timeProvider: TimeProvider,
    private val encryptor: Encryptor,
    private val uuidProvider: UuidProvider,
    verifierSetup: Verification.() -> Verification
) {
    val jwtVerifier: JWTVerifier = JWT
        .require(Algorithm.HMAC256(secretSignKey))
        .withAudience(audience)
        .withIssuer(issuer)
        .verifierSetup()
        .build()

    protected fun jwt(claims: Map<String, String>): Jwt {
        val builder = JWT.create()

        claims.forEach { (key, value) ->
            builder.withClaim(key, value)
        }

        return Jwt.unsafe(
            builder
                .withJWTId(uuidProvider.uuid().value)
                .withAudience(audience)
                .withIssuer(issuer)
                .withExpiresAt(jwtExpiration())
                .sign(Algorithm.HMAC256(secretSignKey))
        )
    }

    private fun jwtExpiration(): Date {
        val currentTimeMillis = timeProvider.currentTimeMillis()
        return Date(currentTimeMillis + expirationTime.inWholeMilliseconds)
    }

    protected fun jwtId(id: Uuid): JwtId {
        val encryptedId = encryptor.encrypt(id.value).getOrThrow()
        val encodedIdString = Base64.getEncoder().encodeToString(encryptedId)
        return JwtId.unsafe(encodedIdString)
    }

    fun id(jwtId: JwtId): Uuid? {
        val decodedUserIdString = Base64.getDecoder().decode(jwtId.value)
        val decryptedUserIdString = encryptor.decrypt(decodedUserIdString).getOrNull() ?: return null
        return decryptedUserIdString.toUuid()
    }
}
