// Top-level build.gradle.kts
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
}

// Task to clean the build directory
tasks.register("clean") {
    delete(rootProject.layout.buildDirectory)
}
// Print Gradle, AGP, and Kotlin versions for debugging
tasks.register("versions") {
    doLast {
        val gradleVersion = project.gradle.gradleVersion
        println("Gradle version: $gradleVersion")
        println("Android Gradle Plugin version: 8.9.2")
        println("Kotlin version: 1.9.10")
    }
}