package oneclick.server.shared.authentication.security

import oneclick.server.shared.authentication.models.RegistrationCode
import oneclick.shared.security.SecureRandomProvider
import kotlin.io.encoding.Base64

interface RegistrationCodeProvider {
    fun registrationCode(): RegistrationCode
}

class DefaultRegistrationCodeProvider(
    private val secureRandomProvider: SecureRandomProvider,
) : RegistrationCodeProvider {

    override fun registrationCode(): RegistrationCode {
        val bytes = ByteArray(REGISTRATION_CODE_LENGTH)
        secureRandomProvider.secureRandom().nextBytes(bytes)
        val code = Base64.UrlSafe.encode(bytes)
        return RegistrationCode.unsafe(code)
    }

    private companion object {
        const val REGISTRATION_CODE_LENGTH = 32
    }
}