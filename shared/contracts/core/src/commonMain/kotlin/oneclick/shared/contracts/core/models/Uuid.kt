package oneclick.shared.contracts.core.models

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

@Poko
@Serializable
class Uuid private constructor(val value: String) {

    init {
        require(isValid(value)) { ERROR_MESSAGE }
    }

    companion object {
        private const val ERROR_MESSAGE = "Invalid uuid"

        private val REGEX = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}".toRegex()

        private fun isValid(value: String): Boolean = REGEX.matches(value)

        fun String.toUuid(): Uuid? =
            if (isValid(this)) Uuid(this) else null

        fun unsafe(value: String): Uuid = Uuid(value)
    }
}
