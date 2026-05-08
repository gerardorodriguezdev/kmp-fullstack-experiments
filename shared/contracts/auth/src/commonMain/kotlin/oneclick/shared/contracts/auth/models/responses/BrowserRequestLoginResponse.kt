package oneclick.shared.contracts.auth.models.responses

import kotlinx.serialization.Serializable

@Serializable
sealed interface BrowserRequestLoginResponse {

    @Serializable
    data object ValidLogin : BrowserRequestLoginResponse

    @Serializable
    data object WaitForApproval : BrowserRequestLoginResponse
}