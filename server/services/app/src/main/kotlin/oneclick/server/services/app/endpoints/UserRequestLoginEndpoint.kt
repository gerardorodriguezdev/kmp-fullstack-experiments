package oneclick.server.services.app.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import oneclick.server.services.app.authentication.UserJwtProvider
import oneclick.server.services.app.dataSources.base.UsersDataSource
import oneclick.server.services.app.dataSources.models.RegistrableUser
import oneclick.server.services.app.plugins.apiRateLimit
import oneclick.server.services.app.repositories.RegistrableUsersRepository
import oneclick.server.services.app.repositories.UsersRepository
import oneclick.server.shared.authentication.security.PasswordManager
import oneclick.server.shared.authentication.security.RegistrationCodeProvider
import oneclick.server.shared.utils.clientType
import oneclick.shared.contracts.auth.models.Jwt
import oneclick.shared.contracts.auth.models.Password
import oneclick.shared.contracts.auth.models.Username
import oneclick.shared.contracts.auth.models.requests.LoginRequest.UserRequestLoginRequest
import oneclick.shared.contracts.auth.models.responses.BrowserRequestLoginResponse
import oneclick.shared.contracts.auth.models.responses.MobileRequestLoginResponse
import oneclick.shared.contracts.core.models.ClientEndpoint
import oneclick.shared.contracts.core.models.ClientType
import theoneclick.server.shared.email.base.EmailService

internal fun Routing.userRequestLoginEndpoint(
    usersRepository: UsersRepository,
    passwordManager: PasswordManager,
    userJwtProvider: UserJwtProvider,
    registrationCodeProvider: RegistrationCodeProvider,
    registrableUsersRepository: RegistrableUsersRepository,
    emailService: EmailService,
) {
    apiRateLimit {
        post(ClientEndpoint.USER_REQUEST_LOGIN.route) { userRequestLoginRequest: UserRequestLoginRequest ->
            val (username, password) = userRequestLoginRequest

            val clientType = call.request.clientType
            if (clientType == ClientType.DESKTOP) {
                respondInvalidClientType()
                return@post
            }

            val user = usersRepository.user(UsersDataSource.Findable.ByUsername(username))

            when {
                user == null -> {
                    call.application.log.debug("Registrable user")
                    saveRegistrableUser(
                        username = username,
                        password = password,
                        passwordManager = passwordManager,
                        registrationCodeProvider = registrationCodeProvider,
                        registrableUsersRepository = registrableUsersRepository,
                        emailService = emailService,
                        clientType = clientType,
                    )
                }

                !passwordManager.isPasswordValid(
                    password = password,
                    hashedPassword = user.hashedPassword
                ) -> {
                    call.application.log.debug("Invalid password")
                    call.respond(HttpStatusCode.Unauthorized)
                }

                else -> respondValidLogin(jwt = userJwtProvider.jwt(user.userId), clientType = clientType)
            }
        }
    }
}

private suspend fun RoutingContext.saveRegistrableUser(
    username: Username,
    password: Password,
    passwordManager: PasswordManager,
    registrationCodeProvider: RegistrationCodeProvider,
    registrableUsersRepository: RegistrableUsersRepository,
    emailService: EmailService,
    clientType: ClientType,
) {
    val registrableUser = RegistrableUser(
        registrationCode = registrationCodeProvider.registrationCode(),
        username = username,
        hashedPassword = passwordManager.hashPassword(password),
    )

    val isRegistrableUserSaved = registrableUsersRepository.saveRegistrableUser(registrableUser)
    if (!isRegistrableUserSaved) {
        call.application.log.debug("Registrable user not saved")
        call.respond(HttpStatusCode.InternalServerError)
        return
    }

    sendApprovalEmail(registrableUser = registrableUser, emailService = emailService, clientType = clientType)
}

private suspend fun RoutingContext.sendApprovalEmail(
    registrableUser: RegistrableUser,
    emailService: EmailService,
    clientType: ClientType,
) {
    val emailBody = """
            |username: ${registrableUser.username.value}
            |approvalCode: ${registrableUser.registrationCode.value}
        """.trimMargin()

    val isEmailSent = emailService.sendEmail(
        subject = "User registration requested",
        body = emailBody,
    )
    if (!isEmailSent) {
        call.application.log.debug("Email not sent")
        call.respond(HttpStatusCode.InternalServerError)
    } else {
        respondWaitForApproval(clientType = clientType)
    }
}

private suspend fun RoutingContext.respondValidLogin(jwt: Jwt, clientType: ClientType) {
    call.application.log.debug("Valid login")
    when (clientType) {
        ClientType.MOBILE -> {
            call.respond<MobileRequestLoginResponse>(
                MobileRequestLoginResponse.ValidLogin(jwt = jwt)
            )
        }

        ClientType.BROWSER -> {
            call.sessions.set(jwt)
            call.respond<BrowserRequestLoginResponse>(BrowserRequestLoginResponse.ValidLogin)
        }

        else -> respondInvalidClientType()
    }
}

private suspend fun RoutingContext.respondWaitForApproval(clientType: ClientType) {
    call.application.log.debug("Waiting for approval")
    when (clientType) {
        ClientType.BROWSER -> call.respond<BrowserRequestLoginResponse>(BrowserRequestLoginResponse.WaitForApproval)
        ClientType.MOBILE -> call.respond<MobileRequestLoginResponse>(MobileRequestLoginResponse.WaitForApproval)
        else -> respondInvalidClientType()
    }
}

private suspend fun RoutingContext.respondInvalidClientType() {
    call.application.log.debug("Invalid client type")
    call.respond(HttpStatusCode.BadRequest)
}