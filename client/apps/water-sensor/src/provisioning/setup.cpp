#include <Arduino.h>
#include <EEPROM.h>
#include <SoftwareSerial.h>
#include <DeviceType.h>
#include <Config.h>
#include <Utils.h>

SoftwareSerial bluetooth(3, 2);

//TODO: Complete setup/no pass-through
void setup() {
    Serial.begin(BAUD_RATE);
    bluetooth.begin(BAUD_RATE);

    EEPROM.put(static_cast<int>(ConfigOptions::DEVICE_TYPE), DeviceType::WATER_LEVEL);
    //TODO: Add uuid

    Serial.println("EEPROM configured successfully");
}

void loop() {
    if (bluetooth.available()) {
        Serial.write(bluetooth.read());
    }

    if (Serial.available()) {
        bluetooth.write(Serial.read());
    }
}
