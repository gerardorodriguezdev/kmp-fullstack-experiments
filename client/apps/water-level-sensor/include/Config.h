#ifndef CONFIG_H
#define CONFIG_H

#include <EEPROM.h>
#include <DeviceType.h>

enum class ConfigOptions {
    DEVICE_TYPE = 0
};

struct Config {
    DeviceType deviceType;
};

/**
 * Reads configuration from EEPROM and returns a Config object
 */
inline Config getConfig() {
    return Config{static_cast<DeviceType>(EEPROM.read(static_cast<int>(ConfigOptions::DEVICE_TYPE)))};
}
#endif //CONFIG_H
