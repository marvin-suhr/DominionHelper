import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    // Android target consumed by the :app module
    androidLibrary {
        namespace = "dev.msuhr.dominionkingdoms.shared"
        compileSdk = 36
        minSdk = 24
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // iOS targets. Compiling these requires macOS + Xcode:
    //   ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
    // (iosX64 dropped upstream - CMP 1.12 no longer publishes it)
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.coroutines.core)
            // Room annotations (@Entity, @PrimaryKey, ...) only - the actual Room
            // runtime + compiler stay Android-only in :app for now.
            implementation(libs.androidx.room.common)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
    }

    // Framework produced for the Xcode project in /iosApp
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "shared"  // must match `import shared` in iosApp/ContentView.swift
            isStatic = true
        }
    }
}
