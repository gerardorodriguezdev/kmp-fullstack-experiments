package oneclick.client.apps.home.models

internal enum class CommunicationType(val code: String) {
    META_DATA("MD"),
    DATA("DT");

    companion object {
        fun String.toCommunicationType(): CommunicationType? =
            entries.firstOrNull { entry ->
                entry.code == this
            }
    }
}