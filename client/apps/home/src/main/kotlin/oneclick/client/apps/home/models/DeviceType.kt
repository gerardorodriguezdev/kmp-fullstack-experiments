package oneclick.client.apps.home.models

internal enum class DeviceType(val code: String) {
    WATER_LEVEL("WL"),
    SOUND_ALARM("SA");

    companion object {
        fun String.toDeviceType(): DeviceType? =
            entries.firstOrNull { entry ->
                entry.code == this
            }
    }
}