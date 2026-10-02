plugins {
    alias(libs.plugins.com.android.application)
}

android {
    namespace = "com.sttiten.iptv.samples.hello.extension"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sttiten.iptv.samples.hello.extension"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
}
