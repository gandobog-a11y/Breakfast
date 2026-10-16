plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ru.example.breakfast"
    compileSdk = 36

    defaultConfig {
        applicationId = "ru.example.breakfast"
        minSdk = 23
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
    }
}
