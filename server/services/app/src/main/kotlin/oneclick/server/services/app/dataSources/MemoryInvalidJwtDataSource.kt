package oneclick.server.services.app.dataSources

import oneclick.server.services.app.authentication.JwtCredentials
import oneclick.server.services.app.dataSources.base.InvalidJwtDataSource
import oneclick.shared.contracts.core.models.Uuid
import oneclick.shared.timeProvider.TimeProvider
import java.util.concurrent.ConcurrentHashMap

internal class MemoryInvalidJwtDataSource(private val timeProvider: TimeProvider) : InvalidJwtDataSource {
    private val invalidJwts = ConcurrentHashMap<Uuid, Long>()

    override suspend fun isJwtInvalid(jti: Uuid): Boolean {
        val expirationTime = invalidJwts[jti] ?: return false
        val isExpired = expirationTime < timeProvider.currentTimeMillis()
        if (isExpired) invalidJwts.remove(jti)
        return isExpired
    }

    override suspend fun saveInvalidJwt(jwtCredentials: JwtCredentials): Boolean {
        invalidJwts[jwtCredentials.jti] =
            timeProvider.currentTimeMillis() + jwtCredentials.expirationTime.inWholeMilliseconds
        return true
    }
}