#include <Arduino.h>
#include <SoftwareSerial.h>
#include <DeviceType.h>
#include <CommunicationType.h>

//TODO: Update
float mockWaterLevel = 45.5;

SoftwareSerial bluetooth(3, 2);
constexpr unsigned int baudRate = 9600;
constexpr unsigned int interval = 1000;
constexpr auto deviceType = DeviceType::WATER_LEVEL;
constexpr auto RECEIVED_DATA_SIZE = COMMUNICATION_TYPE_CODE_SIZE + 1;

void setup() {
    Serial.begin(baudRate);
    bluetooth.begin(baudRate);
    Serial.println("Device initialized");
}

float readWaterLevel() {
    return mockWaterLevel;
}

CommunicationType getCommunicationType() {
    char receivedData[RECEIVED_DATA_SIZE] = {};

    for (int i = 0; i < COMMUNICATION_TYPE_CODE_SIZE; i++) {
        receivedData[i] = static_cast<char>(bluetooth.read());
    }

    return stringToCommunicationType(receivedData);
}

void processCommunicationType(const CommunicationType communicationType) {
    switch (communicationType) {
        case CommunicationType::META_DATA: {
            Serial.println("CommunicationType meta data");
            bluetooth.write(deviceTypeToString(deviceType));
        }

        case CommunicationType::DATA: {
            Serial.println("CommunicationType data");
            bluetooth.print(readWaterLevel());
        }

        case CommunicationType::INVALID:
            Serial.println("CommunicationType invalid");
    }
}

void loop() {
    const auto availableData = bluetooth.available();

    const auto hasAvailableData = availableData == COMMUNICATION_TYPE_CODE_SIZE;

    if (hasAvailableData) {
        const auto communicationType = getCommunicationType();
        processCommunicationType(communicationType);
    }

    delay(interval);
}
