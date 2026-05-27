package oneclick.shared.security.encryption

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import oneclick.shared.security.encryption.base.BaseEncryptor
import java.security.Key
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class AndroidKeystoreEncryptor : BaseEncryptor() {
    override val transformation: String = "$ALGORITHM/$BLOCK_MODE/$PADDING"
    private val keyStore: KeyStore = KeyStore.getInstance(KEY_STORE_TYPE)

    init {
        keyStore.load(null)
    }

    override fun secretKey(): Key {
        val existingKey = keyStore.getEntry(KEY_STORE_ALIAS, null)

        return if (existingKey is KeyStore.SecretKeyEntry) {
            existingKey.secretKey
        } else {
            generateSecretKey()
        }
    }

    private fun generateSecretKey(): SecretKey {
        val keyGenerator = keyGenerator()
        keyGenerator.init(
            KeyGenParameterSpec
                .Builder(
                    KEY_STORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                .setBlockModes(BLOCK_MODE)
                .setEncryptionPaddings(PADDING)
                .setKeySize(KEY_SIZE)
                .build()
        )
        return keyGenerator.generateKey()
    }

    private fun keyGenerator(): KeyGenerator =
        KeyGenerator.getInstance(
            ALGORITHM,
            KEY_STORE_TYPE
        )

    companion object Companion {
        const val KEY_STORE_ALIAS = "secret"
        const val KEY_STORE_TYPE = "AndroidKeyStore"
        const val KEY_SIZE = 256
        const val ALGORITHM: String = KeyProperties.KEY_ALGORITHM_AES
        const val BLOCK_MODE: String = KeyProperties.BLOCK_MODE_GCM
        const val PADDING: String = KeyProperties.ENCRYPTION_PADDING_NONE
    }
}