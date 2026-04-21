rootProject.name = "OneClick"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")

    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
        maven("https://packages.confluent.io/maven/")
    }

    versionCatalogs {
        create("ktorLibs") {
            from("io.ktor:ktor-version-catalog:3.2.0")
        }
    }
}

include(
    "shared:logging",
    "shared:dispatchers",
    "shared:timeProvider",
    "shared:ktor",
    "shared:security",
    "shared:contracts:core",
    "shared:contracts:auth",
    "shared:contracts:homes",
)

include(
    "client:apps:home",
    "client:apps:user:core",
    "client:apps:user:di",
    "client:apps:user:ui",
    "client:apps:user:navigation",
    "client:apps:user:notifications",
    "client:apps:user:features:home",
    "client:shared:network",
)

include(
    "server:services:app",
    "server:services:mock",
    "server:shared:utils",
    "server:shared:authentication",
    "server:shared:db",
    "server:shared:email",
)
