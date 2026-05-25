package oneclick.client.apps.user.core

import io.ktor.http.*
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import oneclick.client.apps.user.core.di.createAppComponent
import oneclick.client.apps.user.di.iosCoreComponent
import oneclick.client.apps.user.navigation.DefaultNavigationController
import oneclick.client.apps.user.notifications.DefaultNotificationsController
import oneclick.client.shared.network.dataSources.IOSPreferences
import oneclick.client.shared.network.dataSources.LocalTokenDataSource
import oneclick.client.shared.network.iosHttpClientEngine
import oneclick.shared.dispatchers.platform.dispatchersProvider
import oneclick.shared.logging.EmptyAppLogger
import oneclick.shared.logging.appLogger
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
internal object OneClickApplication {
    val entrypoint: Entrypoint

    init {
        val appLogger = if (Conf.isDebug) appLogger() else EmptyAppLogger()
        val dispatchersProvider = dispatchersProvider()
        val preferences = IOSPreferences(
            preferencesFileProvider = {
                val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
                    directory = NSDocumentDirectory,
                    inDomain = NSUserDomainMask,
                    appropriateForURL = null,
                    create = false,
                    error = null,
                )
                (requireNotNull(documentDirectory).path + "/settings.preferences_pb").toPath()
            },
            appLogger = appLogger,
            dispatchersProvider = dispatchersProvider,
        )
        val tokenDataSource = LocalTokenDataSource(preferences)
        val navigationController = DefaultNavigationController()
        val url = URLBuilder().apply {
            protocolOrNull = Conf.urlProtocol()
            Conf.host?.let { host -> this.host = host }
            Conf.port?.let { port -> this.port = port }
        }.build()
        val coreComponent = iosCoreComponent(
            url = url,
            appLogger = appLogger,
            httpClientEngine = iosHttpClientEngine(),
            tokenDataSource = tokenDataSource,
            dispatchersProvider = dispatchersProvider(),
            navigationController = navigationController,
            logoutManager = IOSLogoutManager(
                navigationController = navigationController,
                tokenDataSource = tokenDataSource,
            ),
            notificationsController = DefaultNotificationsController(),
        )
        val appComponent = createAppComponent(coreComponent)
        entrypoint = Entrypoint(appComponent = appComponent, coreComponent = coreComponent)
    }
}
