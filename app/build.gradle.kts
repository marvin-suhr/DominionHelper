import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.dagger.hilt.android)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("17")
    }
}

android {
    namespace = "dev.msuhr.dominionkingdoms"
    compileSdk = 36

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
        targetSdk = 35
        versionCode = 6
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {

        debug {
            applicationIdSuffix = ".debug"
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
    }
}

dependencies {

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
    implementation(libs.gson)

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
    //implementation(libs.firebase.crashlytics.ndk) // Only needed for native C code??
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