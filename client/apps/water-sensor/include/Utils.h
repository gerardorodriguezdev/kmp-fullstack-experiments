#ifndef UTILS_H
#define UTILS_H

#include <CommunicationType.h>
#include <DeviceType.h>

constexpr unsigned int BAUD_RATE = 9600;
constexpr unsigned int META_DATA_FORMAT_SIZE = COMMUNICATION_TYPE_CODE_SIZE + 3 + DEVICE_TYPE_CODE_SIZE + 1;

/**
 * Moves string to `metaData` buffer in format MD:T=WL where it means MetaDataCommunicationType:T=DeviceType
 */
constexpr void metaData(char *metaData, const DeviceType deviceType) {
    constexpr auto format = "%s:T=%s";
    snprintf(metaData, META_DATA_FORMAT_SIZE, format, communicationTypeToString(CommunicationType::META_DATA),
             deviceTypeToString(deviceType));
}

constexpr unsigned int dataSize(const DeviceType deviceType) {
    return COMMUNICATION_TYPE_CODE_SIZE + 3 + deviceTypeToDataSize(deviceType) + 1;
}

/**
 * Moves string to `data` buffer in format DT:D=100 where it means DataCommunicationType:D=Data
 */
constexpr void data(char *data, const unsigned int dataSize, const unsigned int value) {
    memset(data, 0, dataSize);
    constexpr auto format = "%s:D=%d";
    snprintf(data, dataSize, format, communicationTypeToString(CommunicationType::DATA), value);
}
#endif //UTILS_H
