package oneclick.server.shared.authentication.security

import io.ktor.util.*
import oneclick.shared.security.encryption.base.BaseEncryptor
import java.security.Key
import javax.crypto.spec.SecretKeySpec

class KtorKeystoreEncryptor(private val secretEncryptionKey: String) : BaseEncryptor() {
    override val transformation: String = "$ALGORITHM/$BLOCK_MODE/$PADDING"

    override fun secretKey(): Key = SecretKeySpec(hex(secretEncryptionKey), ALGORITHM)

    private companion object {
        const val ALGORITHM = "AES"
        const val BLOCK_MODE: String = "GCM"
        const val PADDING: String = "NoPadding"
    }
}