package oneclick.server.services.app.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import oneclick.server.services.app.dataSources.models.User
import oneclick.server.services.app.plugins.apiRateLimit
import oneclick.server.services.app.repositories.RegistrableUsersRepository
import oneclick.server.services.app.repositories.UsersRepository
import oneclick.server.shared.authentication.models.RegistrationCode.Companion.toRegistrationCode
import oneclick.server.shared.authentication.security.UuidProvider
import oneclick.shared.contracts.core.models.ClientEndpoint

internal fun Routing.userApproveRegistrableUserEndpoint(
    uuidProvider: UuidProvider,
    registrableUsersRepository: RegistrableUsersRepository,
    usersRepository: UsersRepository,
) {
    apiRateLimit {
        get(ClientEndpoint.USER_APPROVE_REGISTRABLE_USER.route + "/{registration_code}") {
            val registrationCodeString = call.parameters["registration_code"]
            val registrationCode = registrationCodeString?.toRegistrationCode()

            if (registrationCode == null) {
                call.application.log.debug("Registration code null")
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            val registrableUser = registrableUsersRepository.registrableUser(registrationCode)
            if (registrableUser == null) {
                call.application.log.debug("Registrable user not found")
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            val userId = uuidProvider.uuid()
            val newUser = User(
                userId = userId,
                username = registrableUser.username,
                hashedPassword = registrableUser.hashedPassword,
            )

            val isUserSaved = usersRepository.saveUser(newUser)
            if (!isUserSaved) {
                call.application.log.debug("User not saved")
                call.respond(HttpStatusCode.InternalServerError)
                return@get
            }

            val isRegistrableUserCleaned = registrableUsersRepository.deleteRegistrableUser(registrationCode)
            if (!isRegistrableUserCleaned) {
                call.application.log.debug("Registrable user not cleaned")
            }

            call.respond(HttpStatusCode.OK)
        }
    }
}