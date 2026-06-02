#include <Arduino.h>
#include <SoftwareSerial.h>

void setup() {
    Serial.begin(9600);
    pinMode(5, INPUT);
    Serial.println("Init");
}

void loop() {
    int waterLevel = digitalRead(5);

    if (waterLevel == HIGH) {
        Serial.println("Water level is high");
    } else {
        Serial.println("Water level is low");
    }

    delay(1000);
}
