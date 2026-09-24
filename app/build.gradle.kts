import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension

plugins {
    alias(libs.plugins.android.application)
    // Kotlin compilation via AGP built-in Kotlin (android.builtInKotlin=true)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.dagger.hilt.android)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "dev.msuhr.dominionkingdoms"
    compileSdk = 37

    // Needed?
    signingConfigs {
        getByName("debug") {
            // This allows you to run the release build on your phone
            // without needing your production play store key
            storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
        }

        create("release") {
            val storePath = System.getenv("RELEASE_STORE_PATH")

            if (!storePath.isNullOrEmpty()) {
                storeFile = file(storePath)
                storePassword = System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    defaultConfig {
        applicationId = "dev.msuhr.dominionkingdoms"
        minSdk = 24
        targetSdk = 37
        versionCode = 21
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {

        debug {
            applicationIdSuffix = ".debug"
            // Kingdom sharing against the locally running web service.
            // Uses `adb reverse tcp:8085 tcp:8085` (USB tunnel to the dev PC) -
            // re-run that command after reconnecting the device.
            // Alternative over Wi-Fi: http://192.168.178.188:8085 (needs inbound
            // firewall rule on the PC) or http://10.0.2.2:8085 on the emulator.
            buildConfigField("String", "SHARE_SERVICE_BASE_URL", "\"http://127.0.0.1:8085\"")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // If the user has the release env variable, sign with it.
            // Otherwise, gracefully fall back to the debug key so the build doesn't crash.
            signingConfig = if (!System.getenv("RELEASE_STORE_PATH").isNullOrEmpty()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }

            buildConfigField("String", "SHARE_SERVICE_BASE_URL", "\"https://kingdoms.msuhr.dev\"")

            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true // google-services generates string resources
    }
}

dependencies {

    // Shared KMP module (domain logic, also consumed by the iOS app)
    implementation(project(":shared"))

    // Core Android and Kotlin
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom)) // Handles versioning
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.foundation)// try rem
    implementation(libs.androidx.ui.graphics)// try rem
    implementation(libs.androidx.material3)
    implementation(libs.androidx.animation)// try rem
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Data Management
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.preferences.core) // try rem
    implementation(libs.kotlinx.serialization.json)

    // Networking and Image Loading
    implementation(libs.coil.compose)

    // Dependency Injection
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose) // Potentially for Hilt integration with Navigtaion ViewModels
    ksp(libs.hilt.android.compiler)

    // Firebase / (Crashlytics)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    //implementation("com.google.firebase:firebase-crashlytics") // Duplicate
    //implementation(libs.firebase.crashlytics.ndk) // Only needed for native C code?? MIGHT INTERFERE WITH SYMBOL MAPPING UPLOADING
    //implementation(libs.firebase.crashlytics.ktx)
    implementation(libs.firebase.analytics)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debugging Tools
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.test.manifest)
}