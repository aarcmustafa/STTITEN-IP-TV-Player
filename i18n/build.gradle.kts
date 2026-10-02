plugins {
    alias(libs.plugins.com.android.library)
}

android {
    namespace = "com.sttiten.iptv.i18n"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    testImplementation(kotlin("test-junit"))
}
