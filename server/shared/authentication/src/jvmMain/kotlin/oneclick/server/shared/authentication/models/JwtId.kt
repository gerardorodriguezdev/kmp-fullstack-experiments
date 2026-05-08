package oneclick.server.shared.authentication.models

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

@Serializable
@Poko
class JwtId private constructor(val value: String) {
    init {
        require(isValid(value)) { ERROR_MESSAGE }
    }

    companion object {
        private const val ERROR_MESSAGE = "Invalid jwt id"

        private val REGEX = """[A-Za-z0-9+/]{86}={0,2}""".toRegex()

        private fun isValid(value: String): Boolean = REGEX.matches(value)

        fun String.toJwtId(): JwtId? = if (isValid(this)) JwtId(this) else null

        fun unsafe(value: String): JwtId = JwtId(value)
    }
}
