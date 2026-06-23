plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.aldrenstudios.selfreign"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aldrenstudios.selfreign"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            // Shrink and obfuscate for production builds
            isMinifyEnabled = true
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

    composeOptions {
        // Must be compatible with the Kotlin version above
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android KTX + lifecycle
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    // Jetpack Compose (BOM keeps all Compose artifacts in sync)
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation between screens
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // DataStore for lightweight settings/preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // WorkManager schedules the daily encouragement reminder
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Encrypted local storage for the recovery state machine / flags
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // MediaSession + foreground service support for background ambient audio
    // (removed: the app ships no audio assets)

    // Biometric / device-credential app lock
    implementation("androidx.biometric:biometric:1.1.0")

    // Glance for the home-screen widget
    implementation("androidx.glance:glance-appwidget:1.1.0")

    // Compose animation (onboarding transitions); part of the BOM above
    implementation("androidx.compose.animation:animation")

    // Debug-only Compose tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Unit + UI tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
