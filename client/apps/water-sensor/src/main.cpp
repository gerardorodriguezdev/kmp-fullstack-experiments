#include <Arduino.h>
#include <SoftwareSerial.h>
#include <CommunicationType.h>
#include <Utils.h>
#include <Config.h>

constexpr auto mockWaterLevel = 10;
const auto config = getConfig();
const auto DATA_BUFFER_SIZE = dataSize(config.deviceType);
constexpr unsigned int INTERVAL_IN_MILLIS = 1000;
constexpr unsigned int RECEIVE_PIN = 3;
constexpr unsigned int TRANSMIT_PIN = 2;
SoftwareSerial bluetooth(RECEIVE_PIN, TRANSMIT_PIN);
auto currentCommunicationType = CommunicationType::INVALID;
unsigned int lastWaterLevel = 0;

unsigned int readWaterLevel() {
    return random(100);
}

CommunicationType readCommunicationType() {
    char receivedData[COMMUNICATION_TYPE_CODE_SIZE + 1] = {};

    for (unsigned int i = 0; i < COMMUNICATION_TYPE_CODE_SIZE; i++) {
        receivedData[i] = static_cast<char>(bluetooth.read());
    }

    return stringToCommunicationType(receivedData);
}

void handleCommunicationType(const CommunicationType communicationType) {
    currentCommunicationType = communicationType;

    switch (communicationType) {
        case CommunicationType::META_DATA: {
            char metaDataBuffer[META_DATA_FORMAT_SIZE] = {};
            metaData(metaDataBuffer, config.deviceType);

            bluetooth.write(metaDataBuffer);

            break;
        }

        case CommunicationType::DATA: {
            const auto currentWaterLevel = readWaterLevel();
            lastWaterLevel = currentWaterLevel;

            const auto dataBuffer = new char[DATA_BUFFER_SIZE];
            data(dataBuffer, DATA_BUFFER_SIZE, lastWaterLevel);

            bluetooth.write(dataBuffer);

            delete[] dataBuffer;

            break;
        }

        case CommunicationType::INVALID:
            break;
    }
}

boolean shouldReadWaterLevel() {
    return currentCommunicationType == CommunicationType::DATA && bluetooth.isListening();
}

void setup() {
    Serial.begin(BAUD_RATE);
    bluetooth.begin(BAUD_RATE);

    Serial.println("Device initialized");
}

void loop() {
    const auto availableData = bluetooth.available();

    const auto hasAvailableData = availableData == COMMUNICATION_TYPE_CODE_SIZE;

    if (hasAvailableData) {
        const auto communicationType = readCommunicationType();

        Serial.println(communicationTypeToString(communicationType));

        handleCommunicationType(communicationType);
    } else {
        if (shouldReadWaterLevel()) {
            Serial.println("Reading water level");

            const auto currentWaterLevel = readWaterLevel();
            if (currentWaterLevel != lastWaterLevel) {
                Serial.println("Updating water level");

                lastWaterLevel = currentWaterLevel;

                const auto dataBuffer = new char[DATA_BUFFER_SIZE];
                data(dataBuffer, DATA_BUFFER_SIZE, currentWaterLevel);

                bluetooth.write(dataBuffer);

                delete[] dataBuffer;
            }
        }
    }

    delay(INTERVAL_IN_MILLIS);
}
