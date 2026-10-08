plugins {
    id("com.android.application")
}

android {
    namespace = "com.mbia.dataflow"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mbia.dataflow"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
}

