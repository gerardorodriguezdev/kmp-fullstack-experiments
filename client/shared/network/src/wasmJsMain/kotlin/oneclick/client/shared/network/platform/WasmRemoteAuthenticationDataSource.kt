package oneclick.client.shared.network.platform

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.withContext
import oneclick.client.shared.network.models.LogoutResult
import oneclick.client.shared.network.models.RequestLoginResult
import oneclick.client.shared.network.models.UserLoggedResult
import oneclick.shared.contracts.auth.models.requests.LoginRequest
import oneclick.shared.contracts.auth.models.responses.BrowserRequestLoginResponse
import oneclick.shared.contracts.auth.models.responses.IsLoggedResponse
import oneclick.shared.contracts.core.models.ClientEndpoint
import oneclick.shared.dispatchers.platform.DispatchersProvider
import oneclick.shared.logging.AppLogger

class WasmRemoteAuthenticationDataSource(
    private val httpClient: HttpClient,
    private val dispatchersProvider: DispatchersProvider,
    private val appLogger: AppLogger,
) : AuthenticationDataSource {

    override suspend fun isUserLogged(): UserLoggedResult =
        withContext(dispatchersProvider.io()) {
            try {
                val response = httpClient.get(ClientEndpoint.IS_LOGGED.route).body<IsLoggedResponse>()
                response.toUserLoggedResult()
            } catch (error: Exception) {
                appLogger.e("Exception '${error.stackTraceToString()}' while checking if user is logged")
                UserLoggedResult.UnknownError
            }
        }

    private fun IsLoggedResponse.toUserLoggedResult(): UserLoggedResult =
        when (this) {
            is IsLoggedResponse.Logged -> UserLoggedResult.Logged
            is IsLoggedResponse.NotLogged -> UserLoggedResult.NotLogged
        }

    override suspend fun login(request: LoginRequest): RequestLoginResult =
        withContext(dispatchersProvider.io()) {
            try {
                val response = httpClient.post(ClientEndpoint.USER_REQUEST_LOGIN.route) {
                    setBody(request)
                }

                when (response.status) {
                    HttpStatusCode.OK -> {
                        val browserRequestLoginResponse = response.body<BrowserRequestLoginResponse>()
                        browserRequestLoginResponse.handle()
                    }

                    else -> RequestLoginResult.Error
                }
            } catch (error: Exception) {
                appLogger.e(
                    "Exception '${error.stackTraceToString()}' " +
                            "while requesting logging user '${request.username.value}'"
                )
                RequestLoginResult.Error
            }
        }

    private fun BrowserRequestLoginResponse.handle(): RequestLoginResult =
        when (this) {
            is BrowserRequestLoginResponse.ValidLogin -> RequestLoginResult.ValidLogin
            is BrowserRequestLoginResponse.WaitForApproval -> RequestLoginResult.WaitForApproval
        }

    override suspend fun logout(): LogoutResult =
        withContext(dispatchersProvider.io()) {
            try {
                val response = httpClient.get(ClientEndpoint.LOGOUT.route)

                when (response.status) {
                    HttpStatusCode.OK -> LogoutResult.Success
                    else -> LogoutResult.Error
                }
            } catch (error: Exception) {
                appLogger.e("Exception '${error.stackTraceToString()}' while logging out")
                LogoutResult.Error
            }
        }
}
