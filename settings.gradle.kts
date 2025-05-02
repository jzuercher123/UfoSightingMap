// settings.gradle.kts (Located in the root directory of your project)

pluginManagement {
    // Specifies where Gradle should look for plugins themselves.
    repositories {
        google() // Google's Maven repository (for Android Gradle Plugin, etc.)
        mavenCentral() // Standard Maven Central repository
        gradlePluginPortal() // Gradle's official plugin portal
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev") // Add this line

    }
}

dependencyResolutionManagement {
    // Configures how repositories are declared and prioritized.
    // FAIL_ON_PROJECT_REPOS is recommended for preventing accidental use of project-specific repos.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    // Specifies where Gradle should look for your project's library dependencies.
    repositories {
        google() // Google's Maven repository (for AndroidX, Play Services, etc.)
        mavenCentral() // Standard Maven Central repository
        maven { url = uri("https://jitpack.io") }
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev") // Add this line
// <-- Added repository for JitPack libraries (like osmdroid-compose)
    }
}

// Sets the name for the root project.
rootProject.name = "UfoSightingMap" // Adjust if your project folder has a different name

// Includes the 'app' module in the build. Add other modules here if you create them.
include(":app")

