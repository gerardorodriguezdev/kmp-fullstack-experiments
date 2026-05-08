package oneclick.shared.contracts.auth.models

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

@Poko
@Serializable
class Jwt private constructor(val value: String) {

    init {
        require(isValid(value)) { ERROR_MESSAGE }
    }

    companion object Companion {
        private const val ERROR_MESSAGE = "Invalid jwt"

        private val REGEX = "[A-Za-z0-9-_]+.[A-Za-z0-9-_]+.[A-Za-z0-9-_]+".toRegex()

        private fun isValid(value: String): Boolean = REGEX.matches(value)

        fun String.toJwt(): Jwt? =
            if (isValid(this)) Jwt(this) else null

        fun unsafe(value: String): Jwt = Jwt(value)
    }
}
