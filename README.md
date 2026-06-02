Fullstack kmp home automation project for experimentation

## Basic functionality

1) Arduino device reads a water level sensor
2) The water level is sent by a bluetooth device to **Home app**
3) **Home app** receives the event and sends it to **App service**
4) **App service** receives the event and stores it for the specific user
5) The **User app** retrieves the devices and events from the **App service**

## Components

- Server
    - App service
        - Jvm target
- Client
    - User app
        - Android target
        - iOS target
        - Browser target (Wasm)
    - Home app
        - Jvm target
    - Water level sensor
        - Arduino target

## Artifacts

All artifacts are production in mind but made with fake credentials

- Server
    - App service = Docker image

- Client
    - User app
        - Android target = apk
        - iOS target = ipa (unsigned)
        - Browser target = wasm
    - Home app
        - Jvm target = Docker image
    - Water level sensor
        - Arduino target = Hex file

## Deployment

The deployment is not part of the project, only the creation of the artifacts