import buildLogic.convention.configurations.WebpackConfiguration

plugins {
    id("oneclick.wasm.website")
    alias(libs.plugins.kmp.compose.compiler)
    alias(libs.plugins.kmp.compose.jetbrains)
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

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(ktorLibs.client.core)
                implementation(libs.kmp.navigation)
                implementation(projects.shared.logging)
                implementation(projects.client.apps.user.di)
                implementation(projects.shared.dispatchers)
                implementation(projects.client.apps.user.navigation)
                implementation(projects.client.apps.user.notifications)
                implementation(projects.client.shared.network)
                implementation(projects.client.apps.user.core)
            }
        }

        wasmJsMain {
            dependencies {
                implementation(devNpm("compression-webpack-plugin", libs.versions.webpack.compression.get()))
                implementation(devNpm("html-webpack-plugin", libs.versions.webpack.html.get()))
            }
        }
    }
}