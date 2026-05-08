package oneclick.shared.contracts.auth.models

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

@Poko
@Serializable
class Username private constructor(val value: String) {

    init {
        require(isValid(value)) { ERROR_MESSAGE }
    }

    companion object {
        private const val ERROR_MESSAGE = "Invalid username"

        private val REGEX = "[a-zA-Z0-9_]{3,20}".toRegex()

        fun isValid(value: String): Boolean = REGEX.matches(value)

        fun unsafe(value: String): Username = Username(value)

        fun String.toUsername(): Username? =
            if (isValid(this)) Username(this) else null
    }
}
