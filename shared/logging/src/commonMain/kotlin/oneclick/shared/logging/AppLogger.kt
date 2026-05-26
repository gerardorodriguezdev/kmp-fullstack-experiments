package oneclick.shared.logging

interface AppLogger {
    fun i(message: String)
    fun i(tag: String, message: String)
    fun e(message: String)
    fun e(tag: String, message: String)
    fun e(message: String, error: Throwable) = e("$message '${error.stackTraceToString()}'")

    companion object {
        const val DEFAULT_TAG = "[AppLogger]"
    }
}

class EmptyAppLogger : AppLogger {
    override fun i(message: String) {}
    override fun i(tag: String, message: String) {}
    override fun e(message: String) {}
    override fun e(tag: String, message: String) {}
}

expect fun appLogger(): AppLogger
