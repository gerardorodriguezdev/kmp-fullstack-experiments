package theoneclick.server.shared.email

import oneclick.shared.logging.AppLogger
import theoneclick.server.shared.email.base.EmailService

class DebugEmailService(private val appLogger: AppLogger) : EmailService {
    override suspend fun sendEmail(
        subject: String,
        body: String
    ): Boolean {
        appLogger.i(
            """
            |subject: $subject
            |body: $body
            """.trimMargin()
        )
        return true
    }
}