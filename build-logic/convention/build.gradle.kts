plugins {
    `kotlin-dsl`
    alias(libs.plugins.kmp.serialization)
}

kotlin {
    jvmToolchain(libs.versions.jvm.api.get().toInt())
}

dependencies {
    implementation(libs.gradle.kmp.kotlin)
    implementation(libs.gradle.jvm.kotlin)
    implementation(libs.gradle.ktor)
    implementation(libs.gradle.compose)
    implementation(libs.gradle.android.application)
    implementation(libs.gradle.android.library)
    implementation(libs.gradle.chamaleon)
    implementation(libs.gradle.serialization)
    implementation(libs.gradle.jib)
    implementation(libs.gradle.docker.compose)
    implementation(libs.gradle.kaml)
    implementation(libs.jvm.chamaleon)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("jvm-server") {
            id = "oneclick.jvm.server"
            implementationClass = "buildLogic.convention.plugins.JvmServerPlugin"
        }

        register("wasm-website") {
            id = "oneclick.wasm.website"
            implementationClass = "buildLogic.convention.plugins.WasmWebsitePlugin"
        }

        register("android-app") {
            id = "oneclick.android.app"
            implementationClass = "buildLogic.convention.plugins.AndroidAppPlugin"
        }

        register("ios-app") {
            id = "oneclick.ios.app"
            implementationClass = "buildLogic.convention.plugins.IOSAppPlugin"
        }

        register("jvm-app") {
            id = "oneclick.jvm.app"
            implementationClass = "buildLogic.convention.plugins.JvmAppPlugin"
        }

        register("jvm-library") {
            id = "oneclick.jvm.library"
            implementationClass = "buildLogic.convention.plugins.JvmLibraryPlugin"
        }

        register("wasm-library") {
            id = "oneclick.wasm.library"
            implementationClass = "buildLogic.convention.plugins.WasmLibraryPlugin"
        }

        register("android-library") {
            id = "oneclick.android.library"
            implementationClass = "buildLogic.convention.plugins.AndroidLibraryPlugin"
        }

        register("ios-library") {
            id = "oneclick.ios.library"
            implementationClass = "buildLogic.convention.plugins.IOSLibraryPlugin"
        }
    }
}
