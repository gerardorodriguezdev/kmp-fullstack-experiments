package oneclick.client.apps.home

import io.ktor.http.*
import oneclick.client.apps.home.commands.DefaultCommandsHandler
import oneclick.client.apps.home.controllers.BluetoothDevicesController
import oneclick.client.apps.home.controllers.FakeDevicesController
import oneclick.client.apps.home.dataSources.MemoryDevicesStore
import oneclick.client.apps.home.dataSources.RemoteHomeDataSource
import oneclick.client.apps.home.sensors.DSDBluetoothSensor
import oneclick.client.apps.home.utils.FileAppLogger
import oneclick.client.shared.network.dataSources.DataStoreEncryptedPreferences
import oneclick.client.shared.network.dataSources.LocalTokenDataSource
import oneclick.client.shared.network.dataSources.RemoteAuthenticationDataSource
import oneclick.client.shared.network.extensions.urlProtocol
import oneclick.client.shared.network.nativeHttpClient
import oneclick.client.shared.network.platform.okhttpHttpClientEngine
import oneclick.shared.contracts.core.models.ClientType
import oneclick.shared.contracts.core.models.Uuid
import oneclick.shared.dispatchers.platform.dispatchersProvider
import oneclick.shared.logging.appLogger
import oneclick.shared.security.DefaultSecureRandomProvider
import oneclick.shared.security.encryption.FileKeystoreEncryptor
import oneclick.shared.timeProvider.SystemTimeProvider
import java.io.File

fun main() {
    val environment = Environment()
    val dispatchersProvider = dispatchersProvider()
    val timeProvider = SystemTimeProvider()

    val localDirectory = File("local")
    if (!localDirectory.exists()) localDirectory.mkdirs()

    val appLogger = if (environment.useFileAppLogger) {
        FileAppLogger(
            file = File(localDirectory, "logs.txt").apply {
                createIfNotExists()
            },
            timeProvider = timeProvider,
        )
    } else {
        appLogger()
    }
    val secureRandomProvider = DefaultSecureRandomProvider()
    val tokenDataSource = LocalTokenDataSource(
        preferences = DataStoreEncryptedPreferences(
            preferencesFileProvider = {
                File(localDirectory, "settings.preferences_pb").apply {
                    createIfNotExists()
                }
            },
            appLogger = appLogger,
            dispatchersProvider = dispatchersProvider,
            encryptor = FileKeystoreEncryptor(
                keyStorePath = environment.keyStorePath,
                keyStorePassword = environment.keyStorePassword.toCharArray(),
                secureRandomProvider = secureRandomProvider,
            ),
        )
    )
    val devicesStore = MemoryDevicesStore()
    val url = URLBuilder().apply {
        protocolOrNull = environment.protocol.urlProtocol()
        this.host = environment.host
        environment.port?.let { this.port = environment.port }
    }.build()
    val httpClient = nativeHttpClient(
        appLogger = appLogger,
        url = url,
        clientType = ClientType.DESKTOP,
        httpClientEngine = okhttpHttpClientEngine(),
        tokenDataSource = tokenDataSource,
        logoutManager = HomeLogoutManager(
            devicesStore = devicesStore,
            tokenDataSource = tokenDataSource,
        ),
    )
    val authenticationDataSource = RemoteAuthenticationDataSource(
        dispatchersProvider = dispatchersProvider,
        httpClient = httpClient,
        tokenDataSource = tokenDataSource,
        appLogger = appLogger,
    )
    val homeDataSource = RemoteHomeDataSource(
        httpClient = httpClient,
        dispatchersProvider = dispatchersProvider,
        appLogger = appLogger,
    )
    Entrypoint(
        dispatchersProvider = dispatchersProvider,
        authenticationDataSource = authenticationDataSource,
        devicesStore = devicesStore,
        homeDataSource = homeDataSource,
        appLogger = appLogger,
        commandsHandler = DefaultCommandsHandler(
            authenticationDataSource = authenticationDataSource,
            appLogger = appLogger,
            homeId = environment.homeId,
        ),
        devicesController = if (environment.useFakeDevicesController) {
            FakeDevicesController(
                devicesStore = devicesStore,
            )
        } else {
            BluetoothDevicesController(
                appLogger = appLogger,
                devicesStore = devicesStore,
                dispatchersProvider = dispatchersProvider,
                bluetoothSensorsProvider = DSDBluetoothSensor::dsdBluetoothSensors, //TODO: Update
            )
        }
    ).start()
}

private fun File.createIfNotExists() {
    if (!exists()) {
        createNewFile()
    }
}

private class Environment(
    val homeId: Uuid = Uuid.unsafe(System.getenv("HOME_ID")),
    val keyStorePath: String = System.getenv("KEYSTORE_PATH"),
    val keyStorePassword: String = System.getenv("KEYSTORE_PASSWORD"),
    val protocol: String = System.getenv("PROTOCOL"),
    val port: Int? = System.getenv("IMAGE_PORT")?.toIntOrNull(),
    val host: String = System.getenv("HOST"),
    val useFakeDevicesController: Boolean = System.getenv("USE_FAKE_DEVICES_CONTROLLER") == "true",
    val useFileAppLogger: Boolean = System.getenv("USE_FILE_APP_LOGGER") == "true",
)
