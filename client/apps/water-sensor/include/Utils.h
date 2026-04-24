#ifndef UTILS_H
#define UTILS_H

#include <CommunicationType.h>
#include <DeviceType.h>

constexpr unsigned int BAUD_RATE = 9600;
constexpr unsigned int META_DATA_FORMAT_SIZE = COMMUNICATION_TYPE_CODE_SIZE + 3 + DEVICE_TYPE_CODE_SIZE + 1;

constexpr void metaData(char *metaData, const DeviceType deviceType) {
    constexpr auto format = "%s:T=%s";
    snprintf(metaData, META_DATA_FORMAT_SIZE, format, communicationTypeToString(CommunicationType::META_DATA),
             deviceTypeToString(deviceType));
}

constexpr unsigned int dataSize(const DeviceType deviceType) {
    return COMMUNICATION_TYPE_CODE_SIZE + 3 + deviceTypeToDataSize(deviceType) + 1;
}

constexpr void data(char *data, const DeviceType deviceType, const unsigned int value) {
    constexpr auto format = "%s:D=%d";
    snprintf(data, dataSize(deviceType), format, communicationTypeToString(CommunicationType::META_DATA), value);
}
#endif //UTILS_H
