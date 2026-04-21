#ifndef DevicesTypes_h
#define DevicesTypes_h

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

#endif //DevicesTypes_h
