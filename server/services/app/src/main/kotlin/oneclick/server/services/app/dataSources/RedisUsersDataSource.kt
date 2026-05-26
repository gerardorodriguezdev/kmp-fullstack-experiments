package oneclick.server.services.app.dataSources

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import oneclick.server.services.app.dataSources.base.UsersDataSource
import oneclick.server.services.app.dataSources.base.UsersDataSource.Findable
import oneclick.server.services.app.dataSources.models.User
import oneclick.shared.contracts.auth.models.Username
import oneclick.shared.contracts.core.models.Uuid
import oneclick.shared.dispatchers.platform.DispatchersProvider
import oneclick.shared.logging.AppLogger

@OptIn(ExperimentalLettuceCoroutinesApi::class)
internal class RedisUsersDataSource(
    private val syncCommands: RedisCoroutinesCommands<String, String>,
    private val dispatchersProvider: DispatchersProvider,
    private val appLogger: AppLogger,
) : UsersDataSource {

    override suspend fun user(findable: Findable): User? =
        try {
            withContext(dispatchersProvider.io()) {
                syncCommands.getUser(findable)
            }
        } catch (error: Exception) {
            appLogger.e("Error trying to find user", error)
            null
        }

    override suspend fun saveUser(user: User): Boolean =
        try {
            withContext(dispatchersProvider.io()) {
                val userString = Json.encodeToString(user)
                syncCommands.setUser(user, userString)
                true
            }
        } catch (error: Exception) {
            appLogger.e("Error trying to save user", error)
            false
        }

    private fun userByUserIdKey(userId: Uuid): String = "user:userId:${userId.value}"
    private fun userByUsernameKey(username: Username): String = "user:username:${username.value}"

    private fun Findable.toKey(): String =
        when (this) {
            is Findable.ByUserId -> userByUserIdKey(userId)
            is Findable.ByUsername -> userByUsernameKey(username)
        }

    private suspend fun RedisCoroutinesCommands<String, String>.getUser(findable: Findable): User? {
        val userString = get(findable.toKey()) ?: return null
        return Json.decodeFromString<User>(userString)
    }

    private suspend fun RedisCoroutinesCommands<String, String>.setUser(user: User, userString: String) {
        set(userByUserIdKey(user.userId), userString)
        set(userByUsernameKey(user.username), userString)
    }
}