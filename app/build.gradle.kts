plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.livewallpaper"
    compileSdk = 36

    // Release signing comes from environment (local shell or CI secrets):
    // KEYSTORE_PATH, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD.
    // When absent (e.g. a PR build), the release stays unsigned.
    val ksPath = System.getenv("KEYSTORE_PATH")
    val ksPass = System.getenv("KEYSTORE_PASSWORD")
    val ksAlias = System.getenv("KEY_ALIAS")
    val ksKeyPass = System.getenv("KEY_PASSWORD")
    val hasSigning = !ksPath.isNullOrBlank() && !ksPass.isNullOrBlank() &&
        !ksAlias.isNullOrBlank() && !ksKeyPass.isNullOrBlank()

    if (hasSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(ksPath)
                storePassword = ksPass
                keyAlias = ksAlias
                keyPassword = ksKeyPass
            }
        }
    }

    defaultConfig {
        applicationId = "com.example.livewallpaper"
        minSdk = 29
        // minSdk 29 (Android 10 / One UI 2.x) covers 99%+ of active Samsung
        // Galaxy devices while allowing modern Canvas/ColorSpace APIs.
        // Bump to 30+ only if you need per-app language or exact-alarm APIs.
        targetSdk = 36
        versionCode = 6
        versionName = "1.3.2"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            // Keep debuggable wallpapers responsive; no minification.
            isMinifyEnabled = false
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}
