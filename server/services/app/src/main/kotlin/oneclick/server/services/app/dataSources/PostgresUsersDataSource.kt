package oneclick.server.services.app.dataSources

import kotlinx.coroutines.withContext
import oneclick.server.services.app.dataSources.base.UsersDataSource
import oneclick.server.services.app.dataSources.models.User
import oneclick.server.services.app.postgresql.AppDatabase
import oneclick.server.services.app.postgresql.Users
import oneclick.server.shared.authentication.models.HashedPassword
import oneclick.shared.contracts.auth.models.Username
import oneclick.shared.contracts.core.models.Uuid
import oneclick.shared.dispatchers.platform.DispatchersProvider
import oneclick.shared.logging.AppLogger

internal class PostgresUsersDataSource(
    private val database: AppDatabase,
    private val dispatchersProvider: DispatchersProvider,
    private val appLogger: AppLogger,
) : UsersDataSource {

    override suspend fun user(findable: UsersDataSource.Findable): User? =
        try {
            withContext(dispatchersProvider.io()) {
                val dbUser = when (findable) {
                    is UsersDataSource.Findable.ByUserId -> {
                        database.usersQueries.userByUserId(findable.userId.value)
                            .executeAsOneOrNull()
                    }

                    is UsersDataSource.Findable.ByUsername -> {
                        database.usersQueries.userByUsername(findable.username.value)
                            .executeAsOneOrNull()
                    }
                }

                dbUser?.toUser()
            }
        } catch (error: Exception) {
            appLogger.e("Error trying to find user", error)
            null
        }

    private fun Users.toUser(): User =
        User(
            userId = Uuid.unsafe(user_id),
            username = Username.unsafe(username),
            hashedPassword = HashedPassword.unsafe(hashed_password)
        )

    override suspend fun saveUser(user: User): Boolean =
        try {
            withContext(dispatchersProvider.io()) {
                database.usersQueries.insertUser(user.toUsers())
                true
            }
        } catch (error: Exception) {
            appLogger.e("Error trying to save user", error)
            false
        }

    private fun User.toUsers(): Users =
        Users(
            user_id = userId.value,
            username = username.value,
            hashed_password = hashedPassword.value,
        )
}
