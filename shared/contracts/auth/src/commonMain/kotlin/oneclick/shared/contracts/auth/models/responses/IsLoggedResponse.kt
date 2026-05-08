package oneclick.shared.contracts.auth.models.responses

import kotlinx.serialization.Serializable

@Serializable
sealed interface IsLoggedResponse {

    @Serializable
    data object Logged : IsLoggedResponse

    @Serializable
    data object NotLogged : IsLoggedResponse
}