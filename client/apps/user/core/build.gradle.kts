import buildLogic.convention.configurations.WebpackConfiguration
import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.compose.ExperimentalComposeLibrary

plugins {
    id("oneclick.wasm.website")
    id("oneclick.android.app")
    id("oneclick.ios.app")
    alias(libs.plugins.kmp.compose.compiler)
    alias(libs.plugins.kmp.compose.jetbrains)
    alias(libs.plugins.kmp.serialization)
    alias(libs.plugins.kmp.build.config)
    alias(libs.plugins.kmp.ksp)
    alias(libs.plugins.kmp.chamaleon)
    alias(libs.plugins.kmp.stability.analyzer)
}

wasmWebsite {
    webpackConfiguration {
        port = 3_000
        proxy = WebpackConfiguration.Proxy(
            context = mutableListOf("/api"),
            target = "http://localhost:8080",
        )
        ignoredFiles = listOf("**/local/**")
    }
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
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(ktorLibs.serialization.kotlinx.json)
                implementation(ktorLibs.client.core)
                implementation(ktorLibs.client.contentNegotiation)
                implementation(ktorLibs.client.logging)
                implementation(libs.kmp.window.classes)
                implementation(libs.kmp.coroutines)
                implementation(ktorLibs.client.auth)
                implementation(libs.kmp.navigation)
                implementation(libs.kmp.viewModel)
                implementation(libs.kmp.immutable)
                implementation(libs.kmp.kotlin.inject)
                implementation(libs.kmp.kotlin.inject.kmp)
                implementation(projects.shared.logging)
                implementation(projects.shared.contracts.core)
                implementation(projects.shared.contracts.auth)
                implementation(projects.shared.dispatchers)
                implementation(projects.shared.timeProvider)
                implementation(projects.client.apps.user.di)
                implementation(projects.client.apps.user.navigation)
                implementation(projects.client.apps.user.ui)
                implementation(projects.client.apps.user.notifications)
                implementation(projects.client.apps.user.features.home)
                implementation(projects.client.shared.network)

                project.dependencies {
                    kspAndroid(libs.gradle.ksp.kotlin.inject)
                    kspWasmJs(libs.gradle.ksp.kotlin.inject)
                    kspIosX64(libs.gradle.ksp.kotlin.inject)
                    kspIosArm64(libs.gradle.ksp.kotlin.inject)
                    kspIosSimulatorArm64(libs.gradle.ksp.kotlin.inject)
                }
            }
        }

        @OptIn(ExperimentalComposeLibrary::class)
        commonTest {
            dependencies {
                implementation(libs.kmp.test)
                implementation(compose.uiTest)
                implementation(ktorLibs.client.mock)
                implementation(libs.kmp.test.turbine)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.kmp.datastore)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.android.activity)
                implementation(ktorLibs.client.okhttp)
                implementation(libs.kmp.datastore)
                implementation(projects.shared.security)
            }
        }

        androidInstrumentedTest {
            dependencies {
                implementation(libs.android.test.junit)
                implementation(libs.android.test.navigation)
                implementation(libs.kmp.test.turbine)
            }

            project.dependencies {
                debugImplementation(libs.android.test.leak.canary)
                debugImplementation(compose.uiTooling)
                debugImplementation(libs.android.test.manifest)
            }
        }

        wasmJsMain {
            dependencies {
                implementation(devNpm("compression-webpack-plugin", libs.versions.webpack.compression.get()))
                implementation(devNpm("html-webpack-plugin", libs.versions.webpack.html.get()))
            }
        }

        wasmJsTest {
            dependencies {
                implementation(libs.kmp.test.turbine)
            }
        }
    }
}

buildkonfig {
    packageName = "oneclick.client.apps.user.core.buildkonfig"

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, name = "PROTOCOL", value = null, nullable = true)
        buildConfigField(FieldSpec.Type.STRING, name = "HOST", value = null, nullable = true)
        buildConfigField(FieldSpec.Type.INT, name = "PORT", value = null, nullable = true)
        buildConfigField(FieldSpec.Type.BOOLEAN, name = "IS_DEBUG", value = "false")
    }

    targetConfigs {
        create("android") {
            buildConfigField(
                FieldSpec.Type.STRING,
                name = "PROTOCOL",
                value = androidStringProvider("PROTOCOL").get(),
                nullable = true
            )
            buildConfigField(
                FieldSpec.Type.STRING,
                name = "HOST",
                value = androidStringProvider("HOST").get(),
                nullable = true
            )
            buildConfigField(
                FieldSpec.Type.INT,
                name = "PORT",
                value = androidNullableStringProvider("PORT").orNull,
                nullable = true
            )
            buildConfigField(
                FieldSpec.Type.BOOLEAN,
                name = "IS_DEBUG",
                value = chamaleon.selectedEnvironment().androidPlatform.propertyBooleanValue("IS_DEBUG").toString()
            )
        }

        listOf("iosSimulatorArm64", "iosX64", "iosArm64").forEach { target ->
            create(target) {
                buildConfigField(
                    FieldSpec.Type.STRING,
                    name = "PROTOCOL",
                    value = iosStringProvider("PROTOCOL").get(),
                    nullable = true
                )
                buildConfigField(
                    FieldSpec.Type.STRING,
                    name = "HOST",
                    value = iosStringProvider("HOST").get(),
                    nullable = true
                )
                buildConfigField(
                    FieldSpec.Type.INT,
                    name = "PORT",
                    value = iosNullableStringProvider("PORT").orNull,
                    nullable = true
                )
                buildConfigField(
                    FieldSpec.Type.BOOLEAN,
                    name = "IS_DEBUG",
                    value = chamaleon.selectedEnvironment().nativePlatform.propertyBooleanValue("IS_DEBUG").toString()
                )
            }
        }

        create("wasmJs") {
            buildConfigField(
                FieldSpec.Type.BOOLEAN,
                name = "IS_DEBUG",
                value = chamaleon.selectedEnvironment().wasmPlatform.propertyBooleanValue("IS_DEBUG").toString()
            )
        }
    }
}

fun androidNullableStringProvider(name: String): Provider<String> =
    provider { chamaleon.selectedEnvironment().androidPlatform.propertyStringValueOrNull(name) }

fun androidStringProvider(name: String): Provider<String> =
    provider { chamaleon.selectedEnvironment().androidPlatform.propertyStringValue(name) }

fun iosNullableStringProvider(name: String): Provider<String> =
    provider { chamaleon.selectedEnvironment().nativePlatform.propertyStringValueOrNull(name) }

fun iosStringProvider(name: String): Provider<String> =
    provider { chamaleon.selectedEnvironment().nativePlatform.propertyStringValue(name) }
