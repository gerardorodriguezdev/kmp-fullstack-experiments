plugins {
    id("oneclick.android.app")
    alias(libs.plugins.kmp.compose.compiler)
    alias(libs.plugins.kmp.compose.jetbrains)
    alias(libs.plugins.kmp.serialization)
    alias(libs.plugins.kmp.chamaleon)
    alias(libs.plugins.kmp.stability.analyzer)
}

androidApp {
    jvmTarget = libs.versions.jvm.api.get().toInt()

    namespace = "oneclick.client.apps.user.core"
    compileSdkVersion = libs.versions.android.api.get().toInt()

    applicationId = "oneclick.client.app"
    minSdkVersion = libs.versions.android.api.get().toInt()
    targetSdkVersion = libs.versions.android.api.get().toInt()
    val appVersion = libs.versions.client.app.user.get().toInt()
    versionCode = appVersion
    versionName = "1.$appVersion"
    testRunner = "androidx.test.runner.AndroidJUnitRunner"

    storeFile = file(androidStringProvider("KEYSTORE_PATH"))
    storePassword = androidStringProvider("KEYSTORE_PASSWORD")
    keyAlias = androidStringProvider("KEY_ALIAS")
    keyPassword = androidStringProvider("KEY_PASSWORD")

    composeEnabled = true
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.client.apps.user.core)
                implementation(libs.kmp.datastore)
                implementation(projects.shared.security)
                implementation(projects.shared.dispatchers)
                implementation(projects.shared.logging)
                implementation(projects.shared.timeProvider)
                implementation(projects.client.shared.network)
                implementation(projects.client.apps.user.di)
                implementation(projects.client.apps.user.navigation)
                implementation(projects.client.apps.user.notifications)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.android.activity)
                implementation(ktorLibs.client.okhttp)

                project.dependencies {
                    debugImplementation(libs.android.test.leak.canary)
                    debugImplementation(compose.uiTooling)
                    debugImplementation(libs.android.test.manifest)
                }
            }
        }
    }
}

fun androidStringProvider(name: String): Provider<String> =
    provider { chamaleon.selectedEnvironment().androidPlatform.propertyStringValue(name) }