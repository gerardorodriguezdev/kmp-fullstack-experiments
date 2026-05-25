package oneclick.client.apps.user.core

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.bindToBrowserNavigation
import androidx.navigation.compose.rememberNavController
import io.ktor.client.engine.js.*
import kotlinx.browser.document
import oneclick.apps.user.core.WasmLogoutManager
import oneclick.client.apps.user.core.di.createAppComponent
import oneclick.client.apps.user.di.wasmCoreComponent
import oneclick.client.apps.user.navigation.DefaultNavigationController
import oneclick.client.apps.user.notifications.DefaultNotificationsController
import oneclick.shared.dispatchers.platform.dispatchersProvider
import oneclick.shared.logging.EmptyAppLogger
import oneclick.shared.logging.appLogger

@OptIn(ExperimentalComposeUiApi::class, ExperimentalBrowserHistoryApi::class)
fun main() {
    val appLogger = if (Conf.isDebug) appLogger() else EmptyAppLogger()
    val navigationController = DefaultNavigationController()
    val coreComponent = wasmCoreComponent(
        httpClientEngine = Js.create(),
        appLogger = appLogger,
        dispatchersProvider = dispatchersProvider(),
        navigationController = navigationController,
        logoutManager = WasmLogoutManager(navigationController),
        notificationsController = DefaultNotificationsController(),
    )
    val appComponent = createAppComponent(coreComponent)
    val entrypoint = Entrypoint(appComponent = appComponent, coreComponent = coreComponent)
    ComposeViewport(requireNotNull(document.body)) {
        val navHostController = rememberNavController()

        entrypoint.App(navHostController = navHostController)

        LaunchedEffect(Unit) {
            navHostController.bindToBrowserNavigation()
        }
    }
}
