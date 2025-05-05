// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
}

// Task to clean the build directory
tasks.register("clean") {
    delete(rootProject.buildDir)
}

// Print Gradle, AGP, and Kotlin versions for debugging
tasks.register("versions") {
    doLast {
        val gradleVersion = project.gradle.gradleVersion
        println("Gradle version: $gradleVersion")

        // Try to get AGP version
        val agpVersion = libs.versions.agp.get()
        println("Android Gradle Plugin version: $agpVersion")

        // Try to get Kotlin version
        val kotlinVersion = libs.versions.kotlin.get()
        println("Kotlin version: $kotlinVersion")
    }
}