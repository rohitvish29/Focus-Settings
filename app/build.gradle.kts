plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // 1. CHANGE HERE: Unique package namespace for your app source code
    namespace = "com.example.selectivesettings"
    compileSdk = 34

    defaultConfig {
        // 2. CHANGE HERE: Unique Application ID installed on Android devices
        applicationId = "com.example.selectivesettings"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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

    // 3. ADDED: Enables ViewBinding for layout XML auto-generation in Kotlin
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}
