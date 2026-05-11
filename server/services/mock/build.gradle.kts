plugins {
    id("oneclick.jvm.server")
    alias(libs.plugins.kmp.serialization)
}

jvmServer {
    jvmTarget = libs.versions.jvm.api.get().toInt()
    mainClass = "oneclick.server.services.mock.ApplicationKt"
    isDevelopment = true

    dockerConfiguration {
        executablePath = "/usr/local/bin/docker"
        name = "mock"
        port = intProvider("IMAGE_PORT")
        healthzPort = intProvider("HEALTHZ_PORT")
        metricsPort = intProvider("METRICS_PORT")
        tag = stringProvider("IMAGE_TAG")
        registryUrl = stringProvider("REGISTRY_LOCATION")
        registryUsername = stringProvider("REGISTRY_USERNAME")
        registryPassword = stringProvider("REGISTRY_PASSWORD")
    }

    dockerComposeConfiguration {
        executablePath = "/usr/local/bin/docker-compose"
    }
}

dependencies {
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(projects.shared.contracts.core)
    implementation(projects.shared.contracts.auth)
    implementation(projects.shared.contracts.homes)
    implementation(projects.server.shared.utils)
    implementation(libs.jvm.logback.classic)
}

fun stringProvider(name: String): Provider<String> =
    provider { chamaleon.selectedEnvironment().jvmPlatform.propertyStringValue(name) }

fun intProvider(name: String): Provider<Int> =
    stringProvider(name).map { value -> value.toInt() }
