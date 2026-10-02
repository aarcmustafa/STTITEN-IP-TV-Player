plugins {
    alias(libs.plugins.com.android.library)
}

android {
    namespace = "com.sttiten.iptv.core"
}

dependencies {
    api(project(":core:foundation"))
}
