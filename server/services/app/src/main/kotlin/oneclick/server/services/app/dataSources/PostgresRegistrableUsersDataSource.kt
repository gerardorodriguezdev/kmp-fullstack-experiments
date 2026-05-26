package oneclick.server.services.app.dataSources

import kotlinx.coroutines.withContext
import oneclick.server.services.app.dataSources.base.RegistrableUsersDataSource
import oneclick.server.services.app.dataSources.models.RegistrableUser
import oneclick.server.services.app.postgresql.AppDatabase
import oneclick.server.services.app.postgresql.RegistrableUsers
import oneclick.server.shared.authentication.models.HashedPassword
import oneclick.server.shared.authentication.models.RegistrationCode
import oneclick.shared.contracts.auth.models.Username
import oneclick.shared.dispatchers.platform.DispatchersProvider
import oneclick.shared.logging.AppLogger

internal class PostgresRegistrableUsersDataSource(
    private val database: AppDatabase,
    private val dispatchersProvider: DispatchersProvider,
    private val appLogger: AppLogger,
) : RegistrableUsersDataSource {

    override suspend fun registrableUser(registrationCode: RegistrationCode): RegistrableUser? =
        try {
            withContext(dispatchersProvider.io()) {
                val dbRegistrableUser =
                    database.registrableUsersQueries
                        .registrableUserByRegistrationCode(registrationCode.value)
                        .executeAsOneOrNull()

                dbRegistrableUser?.toRegistrableUser()
            }
        } catch (error: Exception) {
            appLogger.e("Error trying to find registrable user", error)
            null
        }

    override suspend fun saveRegistrableUser(registrableUser: RegistrableUser): Boolean =
        try {
            withContext(dispatchersProvider.io()) {
                database.registrableUsersQueries.insertRegistrableUser(registrableUser.toRegistrableUsers())
                true
            }
        } catch (error: Exception) {
            appLogger.e("Error trying to save registrable user", error)
            false
        }

    override suspend fun deleteRegistrableUser(registrationCode: RegistrationCode): Boolean =
        try {
            withContext(dispatchersProvider.io()) {
                database.registrableUsersQueries.deleteByRegistrationCode(registrationCode.value)
                true
            }
        } catch (error: Exception) {
            appLogger.e("Error trying to delete registrable user", error)
            false
        }

    private fun RegistrableUsers.toRegistrableUser(): RegistrableUser =
        RegistrableUser(
            registrationCode = RegistrationCode.unsafe(registration_code),
            username = Username.unsafe(username),
            hashedPassword = HashedPassword.unsafe(hashed_password)
        )

    private fun RegistrableUser.toRegistrableUsers(): RegistrableUsers =
        RegistrableUsers(
            username = username.value,
            hashed_password = hashedPassword.value,
            registration_code = registrationCode.value,
        )
}
