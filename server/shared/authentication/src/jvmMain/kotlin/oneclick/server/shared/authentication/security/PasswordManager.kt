package oneclick.server.shared.authentication.security

import at.favre.lib.crypto.bcrypt.BCrypt
import oneclick.server.shared.authentication.models.HashedPassword
import oneclick.shared.contracts.auth.models.Password
import oneclick.shared.security.SecureRandomProvider

interface PasswordManager {
    fun hashPassword(password: Password): HashedPassword
    fun isPasswordValid(password: Password, hashedPassword: HashedPassword): Boolean
}

class BcryptPasswordManager(
    private val secureRandomProvider: SecureRandomProvider,
) : PasswordManager {
    override fun hashPassword(password: Password): HashedPassword =
        HashedPassword.unsafe(
            BCrypt.with(secureRandomProvider.secureRandom())
                .hashToString(PASSWORD_VERIFICATION_COST, password.value.toCharArray())
        )

    override fun isPasswordValid(password: Password, hashedPassword: HashedPassword): Boolean =
        BCrypt.verifyer().verify(
            password.value.toCharArray(),
            hashedPassword.value.toCharArray()
        ).verified

    private companion object {
        const val PASSWORD_VERIFICATION_COST = 12
    }
}