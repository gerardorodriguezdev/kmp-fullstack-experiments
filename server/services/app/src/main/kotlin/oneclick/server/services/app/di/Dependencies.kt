package oneclick.server.services.app.di

import io.ktor.server.application.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import oneclick.server.services.app.authentication.HomeJwtProvider
import oneclick.server.services.app.authentication.UserJwtProvider
import oneclick.server.services.app.dataSources.base.InvalidJwtDataSource
import oneclick.server.services.app.repositories.HomesRepository
import oneclick.server.services.app.repositories.RegistrableUsersRepository
import oneclick.server.services.app.repositories.UsersRepository
import oneclick.server.shared.authentication.security.PasswordManager
import oneclick.server.shared.authentication.security.RegistrationCodeProvider
import oneclick.server.shared.authentication.security.UuidProvider
import oneclick.shared.timeProvider.TimeProvider
import theoneclick.server.shared.email.base.EmailService

internal class Dependencies(
    val port: Int,
    val healthzPort: Int,
    val metricsPort: Int,
    val passwordManager: PasswordManager,
    val timeProvider: TimeProvider,
    val userJwtProvider: UserJwtProvider,
    val homeJwtProvider: HomeJwtProvider,
    val invalidJwtDataSource: InvalidJwtDataSource,
    val usersRepository: UsersRepository,
    val uuidProvider: UuidProvider,
    val homesRepository: HomesRepository,
    val emailService: EmailService,
    val registrationCodeProvider: RegistrationCodeProvider,
    val registrableUsersRepository: RegistrableUsersRepository,
    val onShutdown: (application: Application) -> Unit,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val originUrl: String,

    // Debug
    val disableRateLimit: Boolean,
    val disableSecureCookie: Boolean,
    val disableHsts: Boolean,
    val disableHttpsRedirect: Boolean,
)