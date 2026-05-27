package oneclick.shared.security.encryption

import oneclick.shared.security.SecureRandomProvider
import oneclick.shared.security.encryption.base.BaseEncryptor
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.Key
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class FileKeystoreEncryptor(
    private val keyStorePath: String,
    private val keyStorePassword: CharArray,
    private val secureRandomProvider: SecureRandomProvider
) : BaseEncryptor() {
    override val transformation: String = "$ALGORITHM/$BLOCK_MODE/$PADDING"
    private val keyStore: KeyStore = KeyStore.getInstance(KeyStore.getDefaultType())

    init {
        val keystoreFile = File(keyStorePath)

        if (!keystoreFile.parentFile.exists()) {
            keystoreFile.parentFile.mkdirs()
        }

        if (keystoreFile.exists()) {
            keyStore.load(FileInputStream(keyStorePath), keyStorePassword)
        } else {
            keyStore.load(null, keyStorePassword)
        }
    }

    override fun secretKey(): Key {
        val existingKey = keyStore.getEntry(
            KEY_STORE_ALIAS,
            KeyStore.PasswordProtection(keyStorePassword)
        )

        return if (existingKey is KeyStore.SecretKeyEntry) {
            existingKey.secretKey
        } else {
            val secretKey = generateSecretKey()
            storeSecretKey(secretKey)
            secretKey
        }
    }

    private fun generateSecretKey(): SecretKey {
        val keyGenerator = keyGenerator()
        keyGenerator.init(
            KEY_SIZE,
            secureRandomProvider.secureRandom(),
        )
        return keyGenerator.generateKey()
    }

    private fun storeSecretKey(secretKey: SecretKey) {
        keyStore.setEntry(
            KEY_STORE_ALIAS,
            KeyStore.SecretKeyEntry(secretKey),
            KeyStore.PasswordProtection(keyStorePassword)
        )

        FileOutputStream(keyStorePath).use { os ->
            keyStore.store(os, keyStorePassword)
        }
    }

    private fun keyGenerator(): KeyGenerator = KeyGenerator.getInstance(ALGORITHM)

    private companion object Companion {
        const val KEY_STORE_ALIAS = "secret"
        const val KEY_SIZE = 256
        const val ALGORITHM = "AES"
        const val BLOCK_MODE: String = "GCM"
        const val PADDING: String = "NoPadding"
    }
}