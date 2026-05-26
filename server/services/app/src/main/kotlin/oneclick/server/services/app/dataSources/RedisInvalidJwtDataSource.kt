package oneclick.server.services.app.dataSources

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.coroutines.withContext
import oneclick.server.services.app.authentication.JwtCredentials
import oneclick.server.services.app.dataSources.base.InvalidJwtDataSource
import oneclick.shared.contracts.core.models.Uuid
import oneclick.shared.dispatchers.platform.DispatchersProvider

@OptIn(ExperimentalLettuceCoroutinesApi::class)
internal class RedisInvalidJwtDataSource(
    private val syncCommands: RedisCoroutinesCommands<String, String>,
    private val dispatchersProvider: DispatchersProvider,
) : InvalidJwtDataSource {

    override suspend fun isJwtInvalid(jti: Uuid): Boolean =
        withContext(dispatchersProvider.io()) {
            syncCommands.get(jti.value) != null
        }

    override suspend fun saveInvalidJwt(jwtCredentials: JwtCredentials): Boolean =
        withContext(dispatchersProvider.io()) {
            val reply =
                syncCommands.setex(
                    jwtCredentials.jti.value,
                    jwtCredentials.expirationTime.inWholeSeconds,
                    jwtCredentials.jti.value,
                )
            reply != null
        }
}