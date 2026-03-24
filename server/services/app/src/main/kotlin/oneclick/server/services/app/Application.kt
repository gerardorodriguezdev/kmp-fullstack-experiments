package oneclick.server.services.app

import io.ktor.server.application.*
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import oneclick.server.services.app.authentication.HomeJwtProvider
import oneclick.server.services.app.authentication.UserJwtProvider
import oneclick.server.services.app.dataSources.*
import oneclick.server.services.app.dataSources.base.InvalidJwtDataSource
import oneclick.server.services.app.di.Dependencies
import oneclick.server.services.app.postgresql.AppDatabase
import oneclick.server.services.app.repositories.*
import oneclick.server.shared.authentication.security.BcryptPasswordManager
import oneclick.server.shared.authentication.security.DefaultRegistrationCodeProvider
import oneclick.server.shared.authentication.security.DefaultUuidProvider
import oneclick.server.shared.authentication.security.KtorKeystoreEncryptor
import oneclick.server.shared.db.databaseDriver
import oneclick.shared.dispatchers.platform.DispatchersProvider
import oneclick.shared.dispatchers.platform.dispatchersProvider
import oneclick.shared.logging.AppLogger
import oneclick.shared.logging.appLogger
import oneclick.shared.security.DefaultSecureRandomProvider
import oneclick.shared.timeProvider.SystemTimeProvider
import oneclick.shared.timeProvider.TimeProvider
import theoneclick.server.shared.email.DebugEmailService
import theoneclick.server.shared.email.PasswordEmailService

fun main() {
    val environment = Environment()
    val secureRandomProvider = DefaultSecureRandomProvider()
    val timeProvider = SystemTimeProvider()
    val encryptor = KtorKeystoreEncryptor(
        secretEncryptionKey = environment.secretEncryptionKey,
        secureRandomProvider = secureRandomProvider,
    )
    val passwordManager = BcryptPasswordManager(secureRandomProvider)
    val appLogger = appLogger()
    val dispatchersProvider = dispatchersProvider()
    val uuidProvider = DefaultUuidProvider()
    val userJwtProvider = UserJwtProvider(
        audience = environment.jwtAudience,
        issuer = environment.jwtIssuer,
        secretSignKey = environment.secretSignKey,
        timeProvider = timeProvider,
        encryptor = encryptor,
        uuidProvider = uuidProvider,
    )
    val homeJwtProvider = HomeJwtProvider(
        audience = environment.jwtAudience,
        issuer = environment.jwtIssuer,
        secretSignKey = environment.secretSignKey,
        timeProvider = timeProvider,
        encryptor = encryptor,
        uuidProvider = uuidProvider,
    )
    val repositories = if (environment.useMemoryDataSources) {
        memoryRepositories(
            timeProvider = timeProvider,
        )
    } else {
        databaseRepositories(
            createDatabaseTables = environment.createDatabaseTables,
            jdbcUrl = environment.jdbcUrl,
            postgresUsername = environment.postgresUsername,
            postgresPassword = environment.postgresPassword,
            redisUrl = environment.redisUrl,
            appLogger = appLogger,
            dispatchersProvider = dispatchersProvider,
        )
    }
    val emailService = if (environment.useLogEmailService) {
        DebugEmailService(appLogger)
    } else {
        PasswordEmailService(
            serverUsername = environment.senderUsername,
            adminEmail = environment.adminEmail,
            serverPassword = environment.senderPassword,
            smtpHost = environment.smtpHost,
            dispatchersProvider = dispatchersProvider,
            appLogger = appLogger,
        )
    }
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val dependencies = Dependencies(
        port = environment.port,
        healthzPort = environment.healthzPort,
        metricsPort = environment.metricsPort,
        passwordManager = passwordManager,
        timeProvider = timeProvider,
        userJwtProvider = userJwtProvider,
        homeJwtProvider = homeJwtProvider,
        onShutdown = repositories.onShutdown,
        usersRepository = repositories.usersRepository,
        homesRepository = repositories.homesRepository,
        uuidProvider = uuidProvider,
        emailService = emailService,
        invalidJwtDataSource = repositories.invalidJwtDataSource,
        registrationCodeProvider = DefaultRegistrationCodeProvider(
            secureRandomProvider = secureRandomProvider,
        ),
        registrableUsersRepository = repositories.registrableUsersRepository,
        disableRateLimit = environment.disableRateLimit,
        disableSecureCookie = environment.disableSecureCookie,
        disableHsts = environment.disableHsts,
        disableHttpsRedirect = environment.disableHttpsRedirect,
        allowLocalOrigins = environment.allowLocalOrigins,
        prometheusMeterRegistry = prometheusMeterRegistry,
    )

    server(dependencies = dependencies).start(wait = true)
}

private fun memoryRepositories(timeProvider: TimeProvider): Repositories {
    val memoryUsersDataSource = MemoryUsersDataSource()
    val usersRepository = DefaultUsersRepository(
        diskUsersDataSource = memoryUsersDataSource,
        memoryUsersDataSource = memoryUsersDataSource,
    )

    val memoryHomesDataSource = MemoryHomesDataSource()
    val homesRepository = DefaultHomesRepository(
        memoryHomesDataSource = memoryHomesDataSource,
        diskHomesDataSource = memoryHomesDataSource,
    )

    val memoryInvalidJwtDataSource = MemoryInvalidJwtDataSource(
        timeProvider = timeProvider,
    )

    val memoryRegistrableUsersDataSource = MemoryRegistrableUsersDataSource()
    val registrableUsersRepository = DefaultRegistrableUsersRepository(
        memoryRegistrableUsersDataSource = memoryRegistrableUsersDataSource,
        diskRegistrableUsersDataSource = memoryRegistrableUsersDataSource,
    )

    return Repositories(
        usersRepository = usersRepository,
        homesRepository = homesRepository,
        invalidJwtDataSource = memoryInvalidJwtDataSource,
        registrableUsersRepository = registrableUsersRepository,
        onShutdown = {},
    )
}

@OptIn(ExperimentalLettuceCoroutinesApi::class)
private fun databaseRepositories(
    createDatabaseTables: Boolean,
    jdbcUrl: String,
    postgresUsername: String,
    postgresPassword: String,
    redisUrl: String,
    appLogger: AppLogger,
    dispatchersProvider: DispatchersProvider,
): Repositories {
    val databaseDriver = databaseDriver(
        jdbcUrl = jdbcUrl,
        postgresUsername = postgresUsername,
        postgresPassword = postgresPassword,
    )

    val appDatabase = AppDatabase(databaseDriver)
    if (createDatabaseTables) {
        AppDatabase.Schema.create(databaseDriver)
    }

    val redisClient = RedisClient.create(redisUrl)
    val redisConnection = redisClient.connect()

    val memoryUsersDataSource = RedisUsersDataSource(
        syncCommands = redisConnection.coroutines(),
        dispatchersProvider = dispatchersProvider,
        appLogger = appLogger,
    )
    val diskUsersDataSource = PostgresUsersDataSource(appDatabase, dispatchersProvider, appLogger)
    val usersRepository = DefaultUsersRepository(
        diskUsersDataSource = diskUsersDataSource,
        memoryUsersDataSource = memoryUsersDataSource,
    )

    val memoryHomesDataSource = RedisHomesDataSource(
        syncCommands = redisConnection.coroutines(),
        dispatchersProvider = dispatchersProvider,
        appLogger = appLogger,
    )
    val diskHomesDataSource = PostgresHomesDataSource(appDatabase, dispatchersProvider, appLogger)
    val homesRepository = DefaultHomesRepository(
        memoryHomesDataSource = memoryHomesDataSource,
        diskHomesDataSource = diskHomesDataSource,
    )

    val invalidJwtDataSource = RedisInvalidJwtDataSource(
        syncCommands = redisConnection.coroutines(),
        dispatchersProvider = dispatchersProvider,
    )

    val memoryRegistrableUsersDataSource = RedisRegistrableUsersDataSource(
        syncCommands = redisConnection.coroutines(),
        dispatchersProvider = dispatchersProvider,
        appLogger = appLogger,
    )
    val diskRegistrableUsersDataSource = PostgresRegistrableUsersDataSource(
        database = appDatabase,
        dispatchersProvider = dispatchersProvider,
        appLogger = appLogger,
    )
    val registrableUsersRepository = DefaultRegistrableUsersRepository(
        memoryRegistrableUsersDataSource = memoryRegistrableUsersDataSource,
        diskRegistrableUsersDataSource = diskRegistrableUsersDataSource,
    )

    return Repositories(
        usersRepository = usersRepository,
        homesRepository = homesRepository,
        invalidJwtDataSource = invalidJwtDataSource,
        registrableUsersRepository = registrableUsersRepository,
        onShutdown = {
            databaseDriver.close()
            redisConnection.close()
            redisClient.shutdown()
        },
    )
}

private class Environment(
    val port: Int = System.getenv("IMAGE_PORT").toInt(),
    val healthzPort: Int = System.getenv("HEALTHZ_PORT").toInt(),
    val metricsPort: Int = System.getenv("METRICS_PORT").toInt(),

    // Security
    val secretEncryptionKey: String = System.getenv("SECRET_ENCRYPTION_KEY"),
    val secretSignKey: String = System.getenv("SECRET_SIGN_KEY"),
    val jwtAudience: String = System.getenv("JWT_AUDIENCE"),
    val jwtIssuer: String = System.getenv("JWT_ISSUER"),

    // Postgres
    postgresPort: Int = System.getenv("POSTGRES_PORT").toInt(),
    postgresHost: String = System.getenv("POSTGRES_HOST"),
    postgresDatabase: String = System.getenv("POSTGRES_DATABASE"),
    val postgresUsername: String = System.getenv("POSTGRES_USERNAME"),
    val postgresPassword: String = System.getenv("POSTGRES_PASSWORD"),

    // Redis
    val redisUrl: String = System.getenv("REDIS_URL"),

    // Email
    val senderUsername: String = System.getenv("SENDER_USERNAME"),
    val adminEmail: String = System.getenv("ADMIN_EMAIL"),
    val senderPassword: String = System.getenv("SENDER_PASSWORD"),
    val smtpHost: String = System.getenv("SMTP_HOST"),

    // Debug
    val useMemoryDataSources: Boolean = System.getenv("USE_MEMORY_DATA_SOURCES") == "true",
    val useLogEmailService: Boolean = System.getenv("USE_DEBUG_EMAIL_SERVICE") == "true",
    val disableSecureCookie: Boolean = System.getenv("DISABLE_SECURE_COOKIE") == "true",
    val disableHsts: Boolean = System.getenv("DISABLE_HSTS") == "true",
    val disableHttpsRedirect: Boolean = System.getenv("DISABLE_HTTPS_REDIRECT") == "true",
    val disableRateLimit: Boolean = System.getenv("DISABLE_RATE_LIMIT") == "true",
    val allowLocalOrigins: Boolean = System.getenv("ALLOW_LOCAL_ORIGINS") == "true",
    val createDatabaseTables: Boolean = System.getenv("CREATE_DATABASE_TABLES") == "true",
) {
    val jdbcUrl: String = "jdbc:postgresql://$postgresHost:$postgresPort/$postgresDatabase"
}

private class Repositories(
    val usersRepository: UsersRepository,
    val homesRepository: HomesRepository,
    val invalidJwtDataSource: InvalidJwtDataSource,
    val registrableUsersRepository: RegistrableUsersRepository,
    val onShutdown: (application: Application) -> Unit,
)