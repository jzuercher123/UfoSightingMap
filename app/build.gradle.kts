// Correct imports for Java classes used below
import java.util.Properties
import java.io.FileInputStream
// DO NOT include 'import kotlin.io.util.*' - it's incorrect

// Top-level plugins block
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt") // Required for Room annotation processing. Consider migrating to KSP.
    id("org.jetbrains.kotlin.plugin.compose") // Compose Compiler Plugin
    // Uncomment if using Hilt for Dependency Injection
    // id("com.google.dagger.hilt.android")
}

android {
    // Specifies the namespace for the application, used for resource generation (R class).
    namespace = "com.ufomap.ufosightingmap" // Corrected namespace
    compileSdk = 34 // Target SDK version for compilation. Use the latest stable version.

    defaultConfig {
        applicationId = "com.ufomap.ufosightingmap" // Corrected application ID
        minSdk = 24 // Minimum Android version required to run the app.
        targetSdk = 34 // Target Android version the app is tested against.
        versionCode = 1 // Internal version number. Increment for each release.
        versionName = "1.0" // User-visible version string.

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" // Test runner class.
        vectorDrawables {
            useSupportLibrary = true // Enable support for vector drawables on older API levels.
        }

        // Read API key from local.properties (Not needed for osmdroid, but harmless)
        val localProperties = Properties()
        try {
            localProperties.load(FileInputStream(rootProject.file("local.properties")))
        } catch (e: java.io.IOException) {
            println("Warning: local.properties file not found. MAPS_API_KEY might be missing.")
        }
        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY", "")

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3" // Check compatibility
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/dependencies"
        }
    }
}

dependencies {

    // Core Android libraries
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // --- Add Material Components library for XML themes ---
    implementation("com.google.android.material:material:1.11.0") // Check for the latest stable version

    // Jetpack Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3") // Material 3 for Compose UI

    // --- osmdroid Dependencies ---
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation(libs.androidx.preference.ktx) // Core osmdroid library

    // Room Database (Keep as is)
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    kapt("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")

    // Coroutines (Keep as is)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Lifecycle ViewModel (Keep as is)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Gson (Keep as is)
    implementation("com.google.code.gson:gson:2.10.1")

    // Hilt (Optional)
    // implementation("com.google.dagger:hilt-android:2.48.1")
    // kapt("com.google.dagger:hilt-compiler:2.48.1")
    // implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Testing dependencies (Keep as is)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // Debug dependencies (Keep as is)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

kapt {
    correctErrorTypes = true
}
