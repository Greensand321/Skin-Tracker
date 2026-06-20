plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.skintracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.skintracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0-native-demo"
        vectorDrawables { useSupportLibrary = true }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            // DayRepository logs decode failures via android.util.Log, which
            // throws "not mocked" on the plain JVM unless this is set.
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Room — JSON-column approach (no migrations needed when DayEntry fields change)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // DataStore — persists theme + preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // WorkManager — periodic JSON backup
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // kotlinx.serialization — DayEntry ↔ JSON for the Room data column
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Meal photos — async thumbnail/full-screen loading from on-disk JPEGs,
    // and EXIF orientation correction for camera captures
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // Unit tests (JVM, src/test) — pure-Kotlin data layer: serialization, date math, repositories
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
