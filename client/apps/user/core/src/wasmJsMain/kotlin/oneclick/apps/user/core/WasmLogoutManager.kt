package oneclick.apps.user.core

import oneclick.client.apps.user.navigation.NavigationController
import oneclick.client.apps.user.navigation.logout
import oneclick.client.shared.network.platform.LogoutManager

class WasmLogoutManager(
    private val navigationController: NavigationController,
) : LogoutManager {
    override suspend fun logout() {
        navigationController.logout()
    }
}