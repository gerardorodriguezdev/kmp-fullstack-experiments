#ifndef DevicesTypes_h
#define DevicesTypes_h

constexpr unsigned int DEVICE_TYPE_CODE_SIZE = 2;

enum class DeviceType {
    WATER_LEVEL,
    SOUND_ALARM,
};

constexpr const char *deviceTypeToString(const DeviceType deviceType) {
    switch (deviceType) {
        case DeviceType::WATER_LEVEL: return "WL";
        case DeviceType::SOUND_ALARM: return "SA";
    }
}

constexpr unsigned int deviceTypeToDataSize(const DeviceType deviceType) {
    switch (deviceType) {
        case DeviceType::WATER_LEVEL: return 3;
        case DeviceType::SOUND_ALARM: return 3;
    }
}

#endif //DevicesTypes_h
