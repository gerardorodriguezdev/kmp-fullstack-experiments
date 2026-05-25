Home automation project to experiment with fullstack kmp

## Basic functionality

1) Bluetooth device sends an event to **Home app**
2) **Home app** receives the event and sends it to **App service**
3) **App service** receives the event and stores it in a redis/postgres database for the specific user
4) The **User app** retrieves the devices and events from the **App service**

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

## Deployment

The deployment is not part of the project, only the creation of the artifacts