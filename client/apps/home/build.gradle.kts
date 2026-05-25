plugins {
    id("oneclick.jvm.app")
}

jvmApp {
    jvmTarget = libs.versions.jvm.api.get().toInt()
    mainClass = "oneclick.client.apps.home.ApplicationKt"

    dockerConfiguration {
        executablePath = "/usr/local/bin/docker"
        name = stringProvider("IMAGE_NAME")
        port = intProvider("IMAGE_PORT")
        tag = stringProvider("IMAGE_TAG")
        registryUrl = stringProvider("REGISTRY_LOCATION")
        registryUsername = stringProvider("REGISTRY_USERNAME")
        registryPassword = stringProvider("REGISTRY_PASSWORD")
    }
}

kotlin {
    dependencies {
        implementation(ktorLibs.client.core)
        implementation(libs.kmp.coroutines)
        implementation(libs.kmp.kable)
        implementation(libs.jvm.logback.classic)
        implementation(projects.client.shared.network)
        implementation(projects.shared.logging)
        implementation(projects.shared.contracts.core)
        implementation(projects.shared.contracts.auth)
        implementation(projects.shared.contracts.homes)
        implementation(projects.shared.dispatchers)
        implementation(projects.shared.logging)
        implementation(projects.shared.ktor)
        implementation(projects.shared.security)
        implementation(projects.shared.timeProvider)
    }
}

fun stringProvider(name: String): Provider<String> =
    provider { chamaleon.selectedEnvironment().jvmPlatform.propertyStringValue(name) }

fun intProvider(name: String): Provider<Int> =
    stringProvider(name).map { value -> value.toInt() }
