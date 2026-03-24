Experimental home automation project

## Basic functionality

1) Bluetooth device sends event to **Home app**
2) **Home app** receives the event and sends it to **App service**
3) **App service** receives the event and stores it in a redis/postgres database for the specific user
4) The **User app** retrieves the devices and events from the **App service** for the specific user

## Pieces

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

## Features

Server:

- Ktor Jvm
- Postgres
- Redis
- JWT for fast authentication but its contents are encrypted
- Secure sessions for browser authentication
- Bearer token for native devices authentication
- Locally runnable with in-memory data sources
- Simple Java Email sender
- Password authentication
- User pre-authentication
- JWT blacklisting
- Wasm application served by Ktor automatically with security best practices
- Multi-client authentication with the same flow
- Safe-validated models from creation to use from server to client and vice versa
- Code reused between all targets as much as possible

User client

- Android/iOS/Browser targets
- Kotlin inject
- Session scoped dependencies
- Locally testable with locally running servers (mock/production)
- Encrypted shared preferences

Home client

- Jvm target
- User authentication from the command line
- Bluetooth communication through Kable
- Manual setup of Jib for docker image creation
- Able to run locally with real/mock bluetooth devices

Gradle plugins

- Standardized plugins for libraries and apps
- Docker image configuration
- Docker compose configuration
- Postgres configuration
- Redis configuration
- Fake server for the browser client
- Easy Wasm distribution serving for server
- Wasm configuration with compression, HTML generation, deliverables with hashes for caching

CI

- Creation of environments
- Creation of artifacts for each target

## How to run?

The project is thought to be able to be run locally and mock certain pieces if required

To select which environment to run, it uses [Chamaleon](https://github.com/gerardorodriguezdev/chamaleon)

### Important run configurations

Server

- runAll: Runs the server using docker compose (and redis/postgres if needed automatically)
- runJvm: Runs the server with the JVM (requires using the Chamaleon in-memory environment)

User Client

- runAndroid: Runs the android client
- runIos: Runs the iOS client
- runWasm: Runs the wasm client

Home Client

- runAll: Runs the client using docker
- runJvm: Runs the client with the Jvm

### Environments

Server

- localMemoryDataSources: Uses in-memory data sources
- localRealDataSources: Uses real data sources (postgres and redis)
- localRemoteTemplate: Template with almost the same configuration as production except still using non-secure http

User Client

- local: Connects to a local non-secure http server so you can use the mock server if required
- debugRemoteTemplate: Template to test with a remote server (can be staging/production)

Home Client

- local: Connects to a local non-secure http server so you can use the mock server if required
- localFakeDevicesController: Uses a fake devices controller that returns events from a device as if you were connected
  to a bluetooth device so you can test without a real device
- debugRemoteTemplate: Template to test with a remote server (can be staging/production)