package theoneclick.server.shared.email

import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.withContext
import oneclick.shared.dispatchers.platform.DispatchersProvider
import oneclick.shared.logging.AppLogger
import theoneclick.server.shared.email.base.EmailService
import java.util.*

class PasswordEmailService(
    private val serverUsername: String,
    private val serverPassword: String,
    private val adminEmail: String,
    private val smtpHost: String,
    private val dispatchersProvider: DispatchersProvider,
    private val appLogger: AppLogger,
) : EmailService {

    private val sessionProperties = Properties().apply {
        put("mail.smtp.auth", "true")
        put("mail.smtp.starttls.enable", "true")
        put("mail.smtp.host", smtpHost)
        put("mail.smtp.port", "587")
        put("mail.smtp.ssl.trust", smtpHost)
        put("mail.smtp.connectiontimeout", "10000")
        put("mail.smtp.timeout", "10000")
        put("mail.smtp.writetimeout", "10000")
    }

    private val session = Session.getInstance(
        sessionProperties,
        PasswordAuthenticator()
    )

    override suspend fun sendEmail(subject: String, body: String): Boolean =
        withContext(dispatchersProvider.io()) {
            val message = message(session = session, subject = subject, body = body)
            try {
                Transport.send(message)
                true
            } catch (e: Exception) {
                appLogger.e("Exception '${e.stackTraceToString()}' while sending email")
                false
            }
        }

    private fun message(session: Session, subject: String, body: String): MimeMessage =
        MimeMessage(session).apply {
            setFrom(InternetAddress(serverUsername))
            setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(adminEmail)
            )
            setSubject(subject)
            setText(body, CONTENT_CHARSET)
        }

    private inner class PasswordAuthenticator : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(serverUsername, serverPassword)
        }
    }

    private companion object {
        const val CONTENT_CHARSET = "UTF-8"
    }
}