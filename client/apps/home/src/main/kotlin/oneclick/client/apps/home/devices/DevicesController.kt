package oneclick.client.apps.home.devices

internal interface DevicesController {
    suspend fun scan(): Boolean
}
