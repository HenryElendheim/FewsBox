pluginManagement {
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
    plugins {
        id("com.android.application") version "8.7.3"
        id("org.jetbrains.kotlin.android") version "2.0.21"
        id("org.jetbrains.kotlin.jvm") version "2.0.21"
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FewsBox"

include(":engine")

// The app module needs the Android SDK. The engine is pure Kotlin and does
// not, so headless environments (CI boxes without the SDK) can still build
// and test all the game logic. Android Studio always has an SDK, so the app
// is always included there.
val hasAndroidSdk = System.getenv("ANDROID_HOME") != null ||
    System.getenv("ANDROID_SDK_ROOT") != null ||
    File(rootDir, "local.properties").let { it.exists() && it.readText().contains("sdk.dir") }

if (hasAndroidSdk) {
    include(":app")
} else {
    logger.lifecycle("No Android SDK found - only :engine is included in this build.")
}
