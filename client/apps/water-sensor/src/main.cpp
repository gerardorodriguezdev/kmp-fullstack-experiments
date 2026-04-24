#include <Arduino.h>
#include <SoftwareSerial.h>
#include <CommunicationType.h>
#include <Utils.h>
#include <Config.h>

//TODO: Make real
constexpr auto mockWaterLevel = 10;
const auto config = getConfig();
const auto DATA_BUFFER_SIZE = dataSize(config.deviceType);
constexpr unsigned int INTERVAL = 1000;
auto nextInterval = millis() + INTERVAL;
SoftwareSerial bluetooth(3, 2);

unsigned int readWaterLevel() {
    return mockWaterLevel;
}

CommunicationType readCommunicationType() {
    char receivedData[COMMUNICATION_TYPE_CODE_SIZE + 1] = {};

    for (unsigned int i = 0; i < COMMUNICATION_TYPE_CODE_SIZE; i++) {
        receivedData[i] = static_cast<char>(bluetooth.read());
    }

    return stringToCommunicationType(receivedData);
}

void handleCommunicationType(const CommunicationType communicationType) {
    switch (communicationType) {
        case CommunicationType::META_DATA: {
            Serial.println("CommunicationType meta data");

            char metaDataBuffer[META_DATA_FORMAT_SIZE] = {};
            metaData(metaDataBuffer, config.deviceType);

            bluetooth.write(metaDataBuffer);
            break;
        }

        case CommunicationType::DATA: {
            Serial.println("CommunicationType data");

            const auto waterLevel = readWaterLevel();

            char dataBuffer[DATA_BUFFER_SIZE];
            memset(dataBuffer, 0, DATA_BUFFER_SIZE);
            data(dataBuffer, config.deviceType, waterLevel);

            bluetooth.write(dataBuffer);
            break;
        }

        case CommunicationType::INVALID:
            Serial.println("CommunicationType invalid");
            break;
    }
}

void setup() {
    Serial.begin(BAUD_RATE);
    bluetooth.begin(BAUD_RATE);
    Serial.println("Device initialized");
}

void loop() {
    const auto currentTime = millis();
    if (currentTime > nextInterval) {
        nextInterval = currentTime + INTERVAL;
        const auto availableData = bluetooth.available();

        const auto hasAvailableData = availableData == COMMUNICATION_TYPE_CODE_SIZE;

        if (hasAvailableData) {
            const auto communicationType = readCommunicationType();
            Serial.println(communicationTypeToString(communicationType));
            handleCommunicationType(communicationType);
        }
    }
}
