package oneclick.server.services.app.dataSources

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import oneclick.server.services.app.dataSources.base.RegistrableUsersDataSource
import oneclick.server.services.app.dataSources.models.RegistrableUser
import oneclick.server.shared.authentication.models.RegistrationCode
import oneclick.shared.dispatchers.platform.DispatchersProvider
import oneclick.shared.logging.AppLogger

@OptIn(ExperimentalLettuceCoroutinesApi::class)
internal class RedisRegistrableUsersDataSource(
    private val syncCommands: RedisCoroutinesCommands<String, String>,
    private val dispatchersProvider: DispatchersProvider,
    private val appLogger: AppLogger,
) : RegistrableUsersDataSource {

    override suspend fun registrableUser(registrationCode: RegistrationCode): RegistrableUser? =
        try {
            withContext(dispatchersProvider.io()) {
                syncCommands.getRegistrableUser(registrationCode)
            }
        } catch (error: Exception) {
            appLogger.e("Error trying to find registrable user", error)
            null
        }

    override suspend fun saveRegistrableUser(registrableUser: RegistrableUser): Boolean =
        try {
            withContext(dispatchersProvider.io()) {
                val registrableUserString = Json.encodeToString(registrableUser)
                syncCommands.setRegistrableUser(registrableUser, registrableUserString)
                true
            }
        } catch (error: Exception) {
            appLogger.e("Error trying to save registrable user", error)
            false
        }

    override suspend fun deleteRegistrableUser(registrationCode: RegistrationCode): Boolean =
        try {
            withContext(dispatchersProvider.io()) {
                syncCommands.deleteRegistrableUserByRegistrationCode(registrationCode)
            }
        } catch (error: Exception) {
            appLogger.e("Error trying to delete registrable user", error)
            false
        }

    private fun registrableUserByRegistrationCodeKey(registrationCode: RegistrationCode): String =
        "registrableUser:registrationCode:${registrationCode.value}"

    private suspend fun RedisCoroutinesCommands<String, String>.getRegistrableUser(registrationCode: RegistrationCode): RegistrableUser? {
        val registrableUserString = get(registrableUserByRegistrationCodeKey(registrationCode)) ?: return null
        return Json.decodeFromString<RegistrableUser>(registrableUserString)
    }

    private suspend fun RedisCoroutinesCommands<String, String>.setRegistrableUser(
        registrableUser: RegistrableUser,
        registrableUserString: String
    ) {
        set(registrableUserByRegistrationCodeKey(registrableUser.registrationCode), registrableUserString)
    }

    private suspend fun RedisCoroutinesCommands<String, String>.deleteRegistrableUserByRegistrationCode(registrationCode: RegistrationCode): Boolean {
        del(registrableUserByRegistrationCodeKey(registrationCode))
        return true
    }
}