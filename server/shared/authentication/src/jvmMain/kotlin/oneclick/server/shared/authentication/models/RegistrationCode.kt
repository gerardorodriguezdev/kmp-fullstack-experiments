package oneclick.server.shared.authentication.models

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

@Serializable
@Poko
class RegistrationCode private constructor(val value: String) {
    init {
        require(isValid(value)) { ERROR_MESSAGE }
    }

    companion object {
        private const val ERROR_MESSAGE = "Invalid registration code"

        private val REGEX = """^[A-Za-z0-9+]{43}=$""".toRegex()

        private fun isValid(value: String): Boolean = REGEX.matches(value)

        fun String.toRegistrationCode(): RegistrationCode? =
            if (isValid(this)) RegistrationCode(this) else null

        fun unsafe(value: String): RegistrationCode = RegistrationCode(value)
    }
}