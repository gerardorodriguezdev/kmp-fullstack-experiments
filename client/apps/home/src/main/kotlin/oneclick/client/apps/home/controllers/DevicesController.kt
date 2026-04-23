package oneclick.client.apps.home.controllers

internal interface DevicesController {
    suspend fun scan(): Boolean
}